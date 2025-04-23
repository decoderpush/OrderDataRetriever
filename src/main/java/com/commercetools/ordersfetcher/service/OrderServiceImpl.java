package com.commercetools.ordersfetcher.service;

import com.commercetools.api.client.ProjectApiRoot;
import com.commercetools.api.models.order.Order;
import com.commercetools.api.models.order.OrderPagedQueryResponse;
import com.commercetools.ordersfetcher.exception.CommerceToolsException;
import com.commercetools.ordersfetcher.model.DateRangeRequest;
import com.commercetools.ordersfetcher.util.DateRangeSegmenter;
import io.vrap.rmf.base.client.error.BadGatewayException;
import io.vrap.rmf.base.client.error.ConcurrentModificationException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;

@Service
@Slf4j
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private static final int PAGE_SIZE = 500; // Maximum allowed by Commerce Tools
    private static final int MAX_RESULTS_PER_QUERY = 10000; // Commerce Tools limit
    private static final String CREATED_AT_FILTER_FORMAT = "createdAt >= \"%s\" and createdAt <= \"%s\"";
    private static final DateTimeFormatter ISO_FORMATTER = DateTimeFormatter.ISO_OFFSET_DATE_TIME;
    private static final long TIMESTAMP_INCREMENT_NANOS = 1_000_000; // 1 millisecond in nanoseconds

    private final ProjectApiRoot apiRoot;
    private final DateRangeSegmenter dateRangeSegmenter;

    @Override
    public List<Order> fetchAllOrdersInDateRange(DateRangeRequest dateRangeRequest) {
        log.info("Starting to fetch orders from {} to {}", dateRangeRequest.getStartDate(), dateRangeRequest.getEndDate());
        
        LocalDate startDate = dateRangeRequest.getStartDate();
        LocalDate endDate = dateRangeRequest.getEndDate();
        
        // Calculate date segments if needed
        List<DateRangeSegmenter.DateRange> dateRanges = dateRangeSegmenter.segmentDateRange(startDate, endDate);
        log.info("Date range has been segmented into {} segments", dateRanges.size());
        
        List<Order> allOrders = new ArrayList<>();
        AtomicInteger totalOrdersProcessed = new AtomicInteger(0);
        
        // Process each date segment
        for (int i = 0; i < dateRanges.size(); i++) {
            DateRangeSegmenter.DateRange segment = dateRanges.get(i);
            log.info("Processing segment {}/{}: {} to {}", i+1, dateRanges.size(), segment.getStartDate(), segment.getEndDate());
            
            List<Order> segmentOrders = fetchAllOrdersForSegment(segment);
            allOrders.addAll(segmentOrders);
            
            totalOrdersProcessed.addAndGet(segmentOrders.size());
            log.info("Completed segment {}/{}: Retrieved {} orders", i+1, dateRanges.size(), segmentOrders.size());
        }
        
        log.info("Completed fetching all orders. Total orders retrieved: {}", totalOrdersProcessed.get());
        return allOrders;
    }
    
    @Override
    @Async
    public CompletableFuture<List<Order>> fetchAllOrdersInDateRangeAsync(DateRangeRequest dateRangeRequest) {
        return CompletableFuture.completedFuture(fetchAllOrdersInDateRange(dateRangeRequest));
    }
    
    /**
     * Fetches ALL orders for a given date range segment, handling Commerce Tools' 10,000 result limit
     * using a time-based cursor pagination approach.
     * 
     * @param dateRange The date range to fetch orders for
     * @return A complete list of all orders in the date range
     */
    private List<Order> fetchAllOrdersForSegment(DateRangeSegmenter.DateRange dateRange) {
        List<Order> segmentOrders = new ArrayList<>();
        
        // Initialize the time window
        ZonedDateTime segmentStartDateTime = dateRange.getStartDate().atStartOfDay(ZoneOffset.UTC);
        ZonedDateTime segmentEndDateTime = dateRange.getEndDate().atTime(LocalTime.MAX).atZone(ZoneOffset.UTC);
        
        // Current window start time - this will advance as we process batches
        ZonedDateTime currentStartDateTime = segmentStartDateTime;
        
        // Keep fetching until we've covered the entire date range
        while (currentStartDateTime.isBefore(segmentEndDateTime) || currentStartDateTime.isEqual(segmentEndDateTime)) {
            log.info("Fetching batch of orders from {} to {}", 
                    currentStartDateTime.format(ISO_FORMATTER), 
                    segmentEndDateTime.format(ISO_FORMATTER));
                    
            // Construct the filter for the current time window
            String where = String.format(
                    CREATED_AT_FILTER_FORMAT,
                    currentStartDateTime.format(ISO_FORMATTER),
                    segmentEndDateTime.format(ISO_FORMATTER)
            );
            
            // Fetch a batch of orders (up to 10,000) within the current time window
            List<Order> batchOrders = fetchOrdersBatch(where);
            segmentOrders.addAll(batchOrders);
            
            log.info("Retrieved {} orders in current batch", batchOrders.size());
            
            // If we got fewer than MAX_RESULTS_PER_QUERY, we've reached the end of this segment
            if (batchOrders.size() < MAX_RESULTS_PER_QUERY) {
                break;
            }
            
            // Find the latest timestamp in the batch to use as the start of the next window
            // This ensures we don't miss any orders that have the same timestamp as our cursor
            ZonedDateTime latestTimestamp = findLatestTimestamp(batchOrders);
            if (latestTimestamp == null || !latestTimestamp.isAfter(currentStartDateTime)) {
                // Safeguard against potential issues - if we can't advance the cursor, we're done
                log.warn("Could not advance time cursor beyond {}. Stopping.", currentStartDateTime);
                break;
            }
            
            // Advance the cursor with a small increment to avoid duplication
            currentStartDateTime = latestTimestamp.plusNanos(TIMESTAMP_INCREMENT_NANOS);
            log.info("Advancing time cursor to: {}", currentStartDateTime.format(ISO_FORMATTER));
        }
        
        return segmentOrders;
    }
    
    /**
     * Fetches a batch of orders (up to 10,000) for the given filter criteria.
     * 
     * @param where The filter criteria
     * @return A list of orders (up to 10,000)
     */
    private List<Order> fetchOrdersBatch(String where) {
        List<Order> batchOrders = new ArrayList<>();
        int offset = 0;
        boolean hasMore = true;
        
        // Keep fetching pages until we get all results or reach the 10,000 limit
        while (hasMore && offset < MAX_RESULTS_PER_QUERY) {
            OrderPagedQueryResponse response = fetchOrdersPage(where, offset, PAGE_SIZE);
            List<Order> pageOrders = response.getResults();
            batchOrders.addAll(pageOrders);
            
            // Prepare for next page
            offset += PAGE_SIZE;
            
            // Check if we've reached the end
            hasMore = (pageOrders.size() == PAGE_SIZE);
            
            // Log progress for large batches
            if (offset % 2000 == 0 && offset > 0) {
                log.info("Fetched {} orders so far in current batch", offset);
            }
        }
        
        return batchOrders;
    }
    
    /**
     * Finds the latest timestamp from a list of orders.
     * 
     * @param orders The list of orders to search
     * @return The latest ZonedDateTime found, or null if the list is empty
     */
    private ZonedDateTime findLatestTimestamp(List<Order> orders) {
        if (orders == null || orders.isEmpty()) {
            return null;
        }
        
        return orders.stream()
                .map(order -> ZonedDateTime.parse(order.getCreatedAt()))
                .max(Comparator.naturalOrder())
                .orElse(null);
    }
    
    @Retryable(
            value = {BadGatewayException.class, ConcurrentModificationException.class},
            maxAttempts = 3,
            backoff = @Backoff(delay = 1000, multiplier = 2)
    )
    private OrderPagedQueryResponse fetchOrdersPage(String where, int offset, int limit) {
        try {
            log.debug("Fetching orders page with offset: {}, limit: {}, where: {}", offset, limit, where);
            
            return apiRoot.orders()
                    .get()
                    .withWhere(where)
                    .withSort("createdAt asc") // Crucial for correct cursor-based pagination
                    .withOffset(offset)
                    .withLimit(limit)
                    .executeBlocking()
                    .getBody();
                    
        } catch (Exception e) {
            log.error("Error fetching orders: {}", e.getMessage(), e);
            throw new CommerceToolsException("Failed to fetch orders from Commerce Tools API", e);
        }
    }
}
