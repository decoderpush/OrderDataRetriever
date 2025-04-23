package com.commercetools.ordersfetcher.demo.service;

import com.commercetools.ordersfetcher.demo.model.SimpleOrder;
import com.commercetools.ordersfetcher.model.DateRangeRequest;
import com.commercetools.ordersfetcher.util.DateRangeSegmenter;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * A simplified order service that demonstrates how to overcome the Commerce Tools
 * API's 10,000 record limit using date range segmentation and parallel processing.
 */
@Service
public class SimpleOrderService {

    private static final Logger logger = Logger.getLogger(SimpleOrderService.class.getName());
    private static final int MAX_API_LIMIT = 10000; // Commerce Tools API limit
    private static final int BATCH_SIZE = 500; // How many orders to fetch in each API call
    private static final DateRangeSegmenter segmenter = new DateRangeSegmenter();
    
    /**
     * Fetches all orders in a date range using parallel processing to improve performance.
     * This segments the date range into smaller chunks and processes them in parallel.
     * 
     * @param dateRange The date range to fetch orders for
     * @return A list of all orders in the date range
     */
    public List<SimpleOrder> fetchAllOrdersInDateRangeParallel(DateRangeRequest dateRange) {
        logger.info("Fetching all orders in parallel from " + dateRange.getStartDate() + " to " + dateRange.getEndDate());
        
        // Segment the date range into smaller chunks
        List<DateRangeSegmenter.DateSegment> segments = 
                segmenter.segmentDateRange(dateRange.getStartDate(), dateRange.getEndDate());
        
        logger.info("Segmented date range into " + segments.size() + " segments");
        
        List<CompletableFuture<List<SimpleOrder>>> futures = new ArrayList<>();
        
        // Process each segment in parallel
        for (DateRangeSegmenter.DateSegment segment : segments) {
            // Create a date range request for this segment
            DateRangeRequest segmentRange = new DateRangeRequest(segment.getStartDate(), segment.getEndDate());
            
            // Process this segment asynchronously
            futures.add(fetchAllOrdersInDateRangeAsync(segmentRange));
        }
        
        // Wait for all futures to complete and combine results
        CompletableFuture<Void> allDone = CompletableFuture.allOf(
                futures.toArray(new CompletableFuture[0]));
        
        // Collect all results
        List<SimpleOrder> allOrders = allDone.thenApply(v -> 
            futures.stream()
                   .map(CompletableFuture::join)
                   .flatMap(List::stream)
                   .collect(Collectors.toList())
        ).join();
        
        logger.info("Completed parallel fetch of all orders, total: " + allOrders.size());
        
        return allOrders;
    }
    
    /**
     * Asynchronously fetches all orders in a date range.
     * 
     * @param dateRange The date range to fetch orders for
     * @return A future that will contain all orders in the date range when complete
     */
    @Async("orderProcessingExecutor")
    public CompletableFuture<List<SimpleOrder>> fetchAllOrdersInDateRangeAsync(DateRangeRequest dateRange) {
        logger.info("Asynchronously fetching orders from " + dateRange.getStartDate() + " to " + dateRange.getEndDate() + 
                   " on thread " + Thread.currentThread().getName());
        
        // Simulate processing time to demonstrate parallel execution
        try {
            long days = ChronoUnit.DAYS.between(dateRange.getStartDate(), dateRange.getEndDate()) + 1;
            Thread.sleep(days * 50); // Simulate longer processing for larger segments
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        // Generate sample orders for this segment
        List<SimpleOrder> orders = generateSampleOrders(dateRange);
        
        logger.info("Completed async fetching for segment " + dateRange.getStartDate() + " to " + dateRange.getEndDate() + 
                   ", found " + orders.size() + " orders");
        
        return CompletableFuture.completedFuture(orders);
    }
    
    /**
     * Demonstrates the segmentation and parallel processing approach.
     * 
     * @param dateRange The date range to demonstrate with
     * @return A map with demonstration details
     */
    public Map<String, Object> demonstrateSegmentation(DateRangeRequest dateRange) {
        logger.info("Demonstrating segmentation for date range: " + 
                dateRange.getStartDate() + " to " + dateRange.getEndDate());
        
        List<DateRangeSegmenter.DateSegment> segments = 
                segmenter.segmentDateRange(dateRange.getStartDate(), dateRange.getEndDate());
        
        Map<String, Object> result = new HashMap<>();
        result.put("originalDateRange", Map.of(
                "startDate", dateRange.getStartDate().toString(),
                "endDate", dateRange.getEndDate().toString(),
                "totalDays", ChronoUnit.DAYS.between(dateRange.getStartDate(), dateRange.getEndDate()) + 1
        ));
        
        result.put("segmentCount", segments.size());
        
        List<Map<String, Object>> segmentDetails = new ArrayList<>();
        for (DateRangeSegmenter.DateSegment segment : segments) {
            segmentDetails.add(Map.of(
                    "startDate", segment.getStartDate().toString(),
                    "endDate", segment.getEndDate().toString(),
                    "daysInSegment", ChronoUnit.DAYS.between(segment.getStartDate(), segment.getEndDate()) + 1
            ));
        }
        result.put("segments", segmentDetails);
        
        // Demonstrate performance advantage of parallel processing
        result.put("performanceComparison", demonstrateParallelPerformance(segments));
        
        return result;
    }
    
    /**
     * Demonstrates the performance advantage of parallel processing.
     * 
     * @param segments The date segments to process
     * @return Performance comparison details
     */
    private Map<String, Object> demonstrateParallelPerformance(List<DateRangeSegmenter.DateSegment> segments) {
        Map<String, Object> result = new HashMap<>();
        
        // Create a thread pool for parallel processing
        int threadCount = Math.min(segments.size(), 4); // Use at most 4 threads
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        
        result.put("threadPoolSize", threadCount);
        
        // First measure sequential processing time
        long startTimeSequential = System.currentTimeMillis();
        
        int sequentialTotal = 0;
        for (DateRangeSegmenter.DateSegment segment : segments) {
            // Simulate processing a segment (fetch + process)
            int count = processSegmentSimulation(segment);
            sequentialTotal += count;
        }
        
        long endTimeSequential = System.currentTimeMillis();
        long sequentialDuration = endTimeSequential - startTimeSequential;
        
        // Then measure parallel processing time
        long startTimeParallel = System.currentTimeMillis();
        
        final ConcurrentHashMap<String, Integer> results = new ConcurrentHashMap<>();
        List<CompletableFuture<Void>> futures = new ArrayList<>();
        
        for (DateRangeSegmenter.DateSegment segment : segments) {
            CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                int count = processSegmentSimulation(segment);
                results.put(segment.toString(), count);
            }, executor);
            
            futures.add(future);
        }
        
        // Wait for all tasks to complete
        CompletableFuture<Void> allDone = CompletableFuture.allOf(
                futures.toArray(new CompletableFuture[0]));
        allDone.join();
        
        long endTimeParallel = System.currentTimeMillis();
        long parallelDuration = endTimeParallel - startTimeParallel;
        
        // Shutdown executor
        executor.shutdown();
        try {
            if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
        }
        
        int parallelTotal = results.values().stream().mapToInt(Integer::intValue).sum();
        
        // Add results to the map
        result.put("sequentialDuration", sequentialDuration);
        result.put("parallelDuration", parallelDuration);
        result.put("speedupFactor", (double) sequentialDuration / parallelDuration);
        result.put("recordsProcessed", parallelTotal);
        
        return result;
    }
    
    /**
     * Simulates processing a date segment.
     * 
     * @param segment The date segment to process
     * @return The number of records processed (simulated)
     */
    private int processSegmentSimulation(DateRangeSegmenter.DateSegment segment) {
        try {
            // Simulate the time it takes to process a segment
            long days = ChronoUnit.DAYS.between(segment.getStartDate(), segment.getEndDate()) + 1;
            int recordCount = (int) (days * 200); // Simulate about 200 orders per day
            
            // Simulate processing time (longer for segments with more days)
            Thread.sleep(50 + (days * 10)); // Base time plus time per day
            
            return recordCount;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return 0;
        }
    }
    
    /**
     * Generates sample orders for a date range.
     * For demonstration purposes only.
     * 
     * @param dateRange The date range to generate orders for
     * @return A list of sample orders
     */
    private List<SimpleOrder> generateSampleOrders(DateRangeRequest dateRange) {
        List<SimpleOrder> orders = new ArrayList<>();
        
        // Calculate number of days in range
        long days = ChronoUnit.DAYS.between(dateRange.getStartDate(), dateRange.getEndDate()) + 1;
        
        // Generate approximately 200 orders per day
        int orderCount = (int) (days * 200);
        
        // Generate sample orders
        for (int i = 0; i < orderCount; i++) {
            // Create a random date within the range
            long randomDay = (long) (Math.random() * days);
            LocalDate orderDate = dateRange.getStartDate().plusDays(randomDay);
            
            // Create a random time
            int hour = (int) (Math.random() * 24);
            int minute = (int) (Math.random() * 60);
            int second = (int) (Math.random() * 60);
            ZonedDateTime orderDateTime = orderDate.atTime(hour, minute, second).atZone(ZoneId.systemDefault());
            
            // Generate a sample order
            SimpleOrder order = createSampleOrder(orderDateTime);
            orders.add(order);
        }
        
        // Sort by creation time
        orders.sort((a, b) -> a.getCreatedAt().compareTo(b.getCreatedAt()));
        
        return orders;
    }
    
    /**
     * Creates a sample order.
     * 
     * @param orderTime The creation time for the order
     * @return A sample order
     */
    private SimpleOrder createSampleOrder(ZonedDateTime orderTime) {
        // Generate a unique ID
        String id = UUID.randomUUID().toString();
        
        // Use a set of sample customer names
        String[] customerNames = {
            "John Smith", "Jane Doe", "Robert Johnson", "Mary Williams", 
            "Michael Brown", "Patricia Jones", "James Garcia", "Linda Martinez"
        };
        String customerName = customerNames[(int) (Math.random() * customerNames.length)];
        
        // Generate a random amount between 10 and 1000
        double amount = 10 + (Math.random() * 990);
        amount = Math.round(amount * 100) / 100.0; // Round to 2 decimal places
        
        // Use EUR as the currency
        String currency = "EUR";
        
        // Pick a random status
        String[] statuses = {"Open", "Confirmed", "Shipped", "Delivered", "Cancelled"};
        String status = statuses[(int) (Math.random() * statuses.length)];
        
        return new SimpleOrder(id, orderTime, customerName, amount, currency, status);
    }
}