package com.commercetools.ordersfetcher.service;

import com.commercetools.api.client.ProjectApiRoot;
import com.commercetools.api.models.order.Order;
import com.commercetools.api.models.order.OrderPagedQueryResponse;
import com.commercetools.ordersfetcher.exception.CommerceToolsException;
import com.commercetools.ordersfetcher.model.DateRangeRequest;
import com.commercetools.ordersfetcher.util.DefaultDailyVolumeEstimator;
import com.commercetools.ordersfetcher.util.EnhancedDateRangeSegmenter;
import com.commercetools.ordersfetcher.util.EnhancedDateRangeSegmenter.DateTimeSegment;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Enhanced implementation of OrderService that uses advanced date segmentation
 * to handle high-volume days with more than 10,000 orders.
 */
@Service
public class EnhancedOrderServiceImpl implements OrderService {

    private static final Logger logger = Logger.getLogger(EnhancedOrderServiceImpl.class.getName());
    
    private static final int PAGE_SIZE = 500; // Maximum allowed by Commerce Tools
    private static final int MAX_RESULTS_PER_QUERY = 10000; // Commerce Tools limit
    private static final String CREATED_AT_FILTER_FORMAT = "createdAt >= \"%s\" and createdAt <= \"%s\"";
    private static final DateTimeFormatter ISO_FORMATTER = DateTimeFormatter.ISO_OFFSET_DATE_TIME;
    private static final long TIMESTAMP_INCREMENT_NANOS = 1_000_000; // 1 millisecond in nanoseconds

    private final ProjectApiRoot apiRoot;
    private final EnhancedDateRangeSegmenter segmenter;
    private final DefaultDailyVolumeEstimator volumeEstimator;
    private final Executor orderSegmentExecutor;

    /**
     * Constructor with dependencies injected.
     */
    public EnhancedOrderServiceImpl(
            ProjectApiRoot apiRoot,
            @Qualifier("orderSegmentExecutor") Executor orderSegmentExecutor) {
        this.apiRoot = apiRoot;
        this.segmenter = new EnhancedDateRangeSegmenter();
        this.volumeEstimator = new DefaultDailyVolumeEstimator();
        this.orderSegmentExecutor = orderSegmentExecutor;
        
        logger.info("Initialized EnhancedOrderServiceImpl with adaptive segmentation");
    }

    @Override
    public List<Order> fetchAllOrdersInDateRange(DateRangeRequest dateRange) {
        logger.info("Fetching all orders from " + dateRange.getStartDate() + " to " + dateRange.getEndDate());
        
        long startTime = System.currentTimeMillis();
        
        // Get all segments with adaptive segmentation based on expected volume
        List<DateTimeSegment> segments = segmenter.segmentDateRangeAdaptively(
                dateRange.getStartDate(), 
                dateRange.getEndDate(),
                volumeEstimator);
        
        logger.info("Created " + segments.size() + " segments for date range");
        
        // Process each segment sequentially
        List<Order> allOrders = new ArrayList<>();
        
        for (DateTimeSegment segment : segments) {
            logger.info("Processing segment: " + segment);
            List<Order> segmentOrders = fetchOrdersForDateTimeSegment(segment);
            allOrders.addAll(segmentOrders);
            logger.info("Retrieved " + segmentOrders.size() + " orders from segment " + segment);
        }
        
        long endTime = System.currentTimeMillis();
        logger.info("Completed fetching all orders in " + (endTime - startTime) + "ms, total: " + allOrders.size());
        
        return allOrders;
    }

    @Override
    public List<Order> fetchAllOrdersInDateRangeParallel(DateRangeRequest dateRange) {
        logger.info("Fetching all orders in parallel from " + dateRange.getStartDate() + " to " + dateRange.getEndDate());
        
        long startTime = System.currentTimeMillis();
        
        // Get all segments with adaptive segmentation based on expected volume
        List<DateTimeSegment> segments = segmenter.segmentDateRangeAdaptively(
                dateRange.getStartDate(), 
                dateRange.getEndDate(),
                volumeEstimator);
        
        logger.info("Created " + segments.size() + " segments for parallel processing");
        
        // Track overall progress
        AtomicInteger totalOrdersProcessed = new AtomicInteger(0);
        AtomicInteger completedSegments = new AtomicInteger(0);
        
        // Create a list to hold all segment futures
        List<CompletableFuture<List<Order>>> futures = new ArrayList<>();
        
        // Launch parallel tasks for each segment using our custom executor
        for (int i = 0; i < segments.size(); i++) {
            final int segmentIndex = i;
            DateTimeSegment segment = segments.get(i);
            
            CompletableFuture<List<Order>> future = CompletableFuture.supplyAsync(() -> {
                logger.info("Starting processing of segment " + (segmentIndex + 1) + "/" + segments.size() + 
                           ": " + segment);
                
                List<Order> segmentOrders = fetchOrdersForDateTimeSegment(segment);
                
                int completed = completedSegments.incrementAndGet();
                int totalProcessed = totalOrdersProcessed.addAndGet(segmentOrders.size());
                
                logger.info("Completed segment " + completed + "/" + segments.size() + 
                           ": Retrieved " + segmentOrders.size() + " orders. Total so far: " + totalProcessed);
                
                return segmentOrders;
            }, orderSegmentExecutor);
            
            futures.add(future);
        }
        
        // Combine all results
        List<Order> allOrders = futures.stream()
                .map(CompletableFuture::join)
                .flatMap(List::stream)
                .collect(Collectors.toList());
        
        long endTime = System.currentTimeMillis();
        logger.info("Completed parallel fetch of all orders in " + (endTime - startTime) + 
                   "ms, total: " + allOrders.size());
        
        return allOrders;
    }
    
    @Override
    @Async("orderSegmentExecutor")
    public CompletableFuture<List<Order>> fetchAllOrdersInDateRangeAsync(DateRangeRequest dateRange) {
        logger.info("Asynchronously fetching orders from " + dateRange.getStartDate() + " to " + dateRange.getEndDate());
        
        List<Order> orders = fetchAllOrdersInDateRange(dateRange);
        
        logger.info("Completed async fetching for range " + dateRange.getStartDate() + " to " + dateRange.getEndDate() + 
                    ", found " + orders.size() + " orders");
        
        return CompletableFuture.completedFuture(orders);
    }
    
    /**
     * Fetches all orders for a date-time segment.
     * This handles segments that may include specific time ranges.
     * 
     * @param segment The date-time segment to fetch orders for
     * @return A list of all orders in the segment
     */
    private List<Order> fetchOrdersForDateTimeSegment(DateTimeSegment segment) {
        // Convert the dates and times to ZonedDateTime for querying
        ZonedDateTime startDateTime;
        ZonedDateTime endDateTime;
        
        if (segment.getStartDate().equals(segment.getEndDate())) {
            // Single day segment with specific time range
            startDateTime = segment.getStartDate()
                    .atTime(segment.getStartTime())
                    .atZone(ZoneOffset.UTC);
            
            endDateTime = segment.getStartDate()
                    .atTime(segment.getEndTime())
                    .atZone(ZoneOffset.UTC);
        } else {
            // Multi-day segment
            startDateTime = segment.getStartDate()
                    .atTime(segment.getStartTime())
                    .atZone(ZoneOffset.UTC);
            
            endDateTime = segment.getEndDate()
                    .atTime(segment.getEndTime())
                    .atZone(ZoneOffset.UTC);
        }
        
        return fetchAllOrdersWithTimestampPagination(startDateTime, endDateTime);
    }
    
    /**
     * Fetches all orders between two timestamps using time-based cursor pagination.
     * This approach can handle more than 10,000 records in any time range.
     * 
     * @param startDateTime The start timestamp
     * @param endDateTime The end timestamp
     * @return A list of all orders between the timestamps
     */
    private List<Order> fetchAllOrdersWithTimestampPagination(ZonedDateTime startDateTime, ZonedDateTime endDateTime) {
        List<Order> allOrders = new ArrayList<>();
        
        // Current window start time - this will advance as we process batches
        ZonedDateTime currentStartDateTime = startDateTime;
        
        // Keep fetching until we've covered the entire date range
        while (currentStartDateTime.isBefore(endDateTime) || currentStartDateTime.isEqual(endDateTime)) {
            logger.info("Fetching batch of orders from " + 
                       currentStartDateTime.format(ISO_FORMATTER) + " to " + 
                       endDateTime.format(ISO_FORMATTER));
                      
            // Construct the filter for the current time window
            String where = String.format(
                    CREATED_AT_FILTER_FORMAT,
                    currentStartDateTime.format(ISO_FORMATTER),
                    endDateTime.format(ISO_FORMATTER)
            );
            
            // Fetch a batch of orders (up to 10,000) within the current time window
            List<Order> batchOrders = fetchOrdersBatch(where);
            allOrders.addAll(batchOrders);
            
            logger.info("Retrieved " + batchOrders.size() + " orders in current batch");
            
            // If we got fewer than MAX_RESULTS_PER_QUERY, we've reached the end of this segment
            if (batchOrders.size() < MAX_RESULTS_PER_QUERY) {
                break;
            }
            
            // Find the latest timestamp in the batch to use as the start of the next window
            ZonedDateTime latestTimestamp = findLatestTimestamp(batchOrders);
            if (latestTimestamp == null || !latestTimestamp.isAfter(currentStartDateTime)) {
                // Safeguard against potential issues - if we can't advance the cursor, we're done
                logger.warning("Could not advance time cursor beyond " + currentStartDateTime + ". Stopping.");
                break;
            }
            
            // Advance the cursor with a small increment to avoid duplication
            currentStartDateTime = latestTimestamp.plusNanos(TIMESTAMP_INCREMENT_NANOS);
            logger.info("Advancing time cursor to: " + currentStartDateTime.format(ISO_FORMATTER));
        }
        
        return allOrders;
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
                logger.info("Fetched " + offset + " orders so far in current batch");
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
        
        // In a real implementation, this would extract the createdAt timestamp from each order
        // and return the latest one. For now, using a simple approach for demonstration.
        
        // Sort orders by createdAt in descending order and get the first one
        // For demonstration purposes only:
        Order lastOrder = orders.get(orders.size() - 1);
        return lastOrder.getCreatedAt();
    }
    
    /**
     * Fetches a page of orders from the Commerce Tools API.
     * Uses retry logic for resilience against temporary failures.
     * 
     * @param where The filter criteria
     * @param offset The offset for pagination
     * @param limit The limit for pagination
     * @return The API response
     */
    @Retryable(
            value = {Exception.class},
            maxAttempts = 3,
            backoff = @Backoff(delay = 1000, multiplier = 2)
    )
    private OrderPagedQueryResponse fetchOrdersPage(String where, int offset, int limit) {
        try {
            logger.fine("Fetching orders page with offset: " + offset + ", limit: " + limit + ", where: " + where);
            
            return apiRoot.orders()
                    .get()
                    .withWhere(where)
                    .withSort("createdAt asc") // Crucial for correct cursor-based pagination
                    .withOffset(offset)
                    .withLimit(limit)
                    .executeBlocking()
                    .getBody();
                    
        } catch (Exception e) {
            logger.severe("Error fetching orders: " + e.getMessage());
            throw new CommerceToolsException("Failed to fetch orders from Commerce Tools API", e);
        }
    }
}