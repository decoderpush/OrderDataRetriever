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
            
            List<Order> segmentOrders = fetchOrdersForSegment(segment);
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
    
    private List<Order> fetchOrdersForSegment(DateRangeSegmenter.DateRange dateRange) {
        List<Order> segmentOrders = new ArrayList<>();
        
        ZonedDateTime startDateTime = dateRange.getStartDate().atStartOfDay(ZoneOffset.UTC);
        ZonedDateTime endDateTime = dateRange.getEndDate().atTime(LocalTime.MAX).atZone(ZoneOffset.UTC);
        
        String where = String.format(
                CREATED_AT_FILTER_FORMAT,
                startDateTime.format(ISO_FORMATTER),
                endDateTime.format(ISO_FORMATTER)
        );
        
        // Start with the first page
        int offset = 0;
        boolean hasMore = true;
        
        while (hasMore && offset < MAX_RESULTS_PER_QUERY) {
            OrderPagedQueryResponse response = fetchOrdersPage(where, offset, PAGE_SIZE);
            segmentOrders.addAll(response.getResults());
            
            // Prepare for next page
            offset += PAGE_SIZE;
            hasMore = (response.getResults().size() == PAGE_SIZE);
        }
        
        return segmentOrders;
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
                    .withSort("createdAt asc")
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
