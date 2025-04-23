package com.commercetools.ordersfetcher.demo;

import com.commercetools.ordersfetcher.model.DateRangeRequest;
import com.commercetools.ordersfetcher.util.DateRangeSegmenter;

import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

/**
 * Demo service to demonstrate the segmentation and parallel processing concepts.
 */
@Service
public class DemoOrderService {

    private static final Logger logger = Logger.getLogger(DemoOrderService.class.getName());
    private final DateRangeSegmenter dateRangeSegmenter;
    
    public DemoOrderService() {
        this.dateRangeSegmenter = new DateRangeSegmenter();
    }
    
    /**
     * Demonstrates the date range segmentation and parallel processing logic.
     * @param dateRange The date range to segment
     * @return A map with demonstration details
     */
    public Map<String, Object> demonstrateSegmentation(DateRangeRequest dateRange) {
        logger.info("Starting segmentation demonstration for date range: " + 
                dateRange.getStartDate() + " to " + dateRange.getEndDate());
        
        // Get segments
        List<DateRangeSegmenter.DateSegment> segments = 
                dateRangeSegmenter.segmentDateRange(dateRange.getStartDate(), dateRange.getEndDate());
        
        // Build result
        Map<String, Object> result = new HashMap<>();
        result.put("originalDateRange", formatDateRange(dateRange));
        result.put("segmentCount", segments.size());
        result.put("totalDays", ChronoUnit.DAYS.between(dateRange.getStartDate(), dateRange.getEndDate()) + 1);
        
        // Convert segments to a more readable format
        List<Map<String, Object>> segmentDetails = new ArrayList<>();
        for (DateRangeSegmenter.DateSegment segment : segments) {
            Map<String, Object> segmentMap = new HashMap<>();
            segmentMap.put("startDate", segment.getStartDate().format(DateTimeFormatter.ISO_DATE));
            segmentMap.put("endDate", segment.getEndDate().format(DateTimeFormatter.ISO_DATE));
            segmentMap.put("daysInSegment", ChronoUnit.DAYS.between(segment.getStartDate(), segment.getEndDate()) + 1);
            segmentDetails.add(segmentMap);
        }
        result.put("segments", segmentDetails);
        
        // Demonstrate parallel processing
        result.put("parallelProcessing", demonstrateParallelProcessing(segments));
        
        logger.info("Completed segmentation demonstration with " + segments.size() + " segments");
        return result;
    }
    
    /**
     * Creates a formatted string representation of a date range.
     * @param dateRange The date range
     * @return Formatted string
     */
    private Map<String, String> formatDateRange(DateRangeRequest dateRange) {
        Map<String, String> result = new HashMap<>();
        result.put("startDate", dateRange.getStartDate().format(DateTimeFormatter.ISO_DATE));
        result.put("endDate", dateRange.getEndDate().format(DateTimeFormatter.ISO_DATE));
        result.put("durationDays", 
                String.valueOf(ChronoUnit.DAYS.between(dateRange.getStartDate(), dateRange.getEndDate()) + 1));
        return result;
    }
    
    /**
     * Demonstrates parallel processing of segments.
     * @param segments The date segments to process
     * @return Processing details
     */
    private Map<String, Object> demonstrateParallelProcessing(List<DateRangeSegmenter.DateSegment> segments) {
        logger.info("Demonstrating parallel processing for " + segments.size() + " segments");
        
        Map<String, Object> result = new HashMap<>();
        
        // Create a thread pool
        int threadCount = Math.min(segments.size(), 4); // Use at most 4 threads for demo
        result.put("threadPoolSize", threadCount);
        
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        
        // Track processing metrics
        ConcurrentHashMap<String, Object> metrics = new ConcurrentHashMap<>();
        metrics.put("completedSegments", 0);
        metrics.put("totalRecordsProcessed", 0);
        
        long startTime = System.currentTimeMillis();
        
        // Submit tasks for each segment
        List<Future<?>> futures = new ArrayList<>();
        for (DateRangeSegmenter.DateSegment segment : segments) {
            futures.add(executor.submit(() -> {
                try {
                    // Simulate processing time (faster for demo)
                    Thread.sleep(500);
                    
                    // Simulate fetching orders
                    // In a real implementation, this would call the Commerce Tools API
                    int recordCount = simulateOrderCount(segment.getStartDate(), segment.getEndDate());
                    
                    // Update metrics
                    metrics.put("completedSegments", (int)metrics.get("completedSegments") + 1);
                    metrics.put("totalRecordsProcessed", (int)metrics.get("totalRecordsProcessed") + recordCount);
                    
                    logger.info("Processed segment " + segment.getStartDate() + " to " + 
                            segment.getEndDate() + " with " + recordCount + " records");
                    
                    return recordCount;
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    logger.warning("Segment processing interrupted");
                    return 0;
                }
            }));
        }
        
        // Wait for all tasks to complete
        for (Future<?> future : futures) {
            try {
                future.get(10, TimeUnit.SECONDS);
            } catch (Exception e) {
                logger.warning("Error waiting for segment processing: " + e.getMessage());
            }
        }
        
        // Shutdown executor
        executor.shutdown();
        try {
            if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
        
        long endTime = System.currentTimeMillis();
        
        // Calculate metrics
        result.put("processingTimeMs", endTime - startTime);
        result.put("completedSegments", metrics.get("completedSegments"));
        result.put("totalRecordsProcessed", metrics.get("totalRecordsProcessed"));
        
        // Estimate sequential time (would be approximately sum of all segment times)
        result.put("estimatedSequentialTimeMs", segments.size() * 500);
        result.put("speedupFactor", 
                (double)segments.size() * 500 / Math.max(1, (endTime - startTime)));
        
        logger.info("Completed parallel processing demonstration");
        return result;
    }
    
    /**
     * Simulates generating a realistic order count for a date range.
     * @param startDate Start date
     * @param endDate End date
     * @return Simulated order count
     */
    private int simulateOrderCount(LocalDate startDate, LocalDate endDate) {
        // This is a simple simulation that creates a somewhat realistic order count
        // In a real implementation, this would fetch actual data from Commerce Tools
        long days = ChronoUnit.DAYS.between(startDate, endDate) + 1;
        
        // Simulate about 1000-3000 orders per day with some randomness
        return (int)(days * (1000 + Math.random() * 2000));
    }
}