package com.commercetools.ordersfetcher.demo.service;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Logger;

import org.springframework.stereotype.Service;

import com.commercetools.ordersfetcher.model.DateRangeRequest;
import com.commercetools.ordersfetcher.util.DefaultDailyVolumeEstimator;
import com.commercetools.ordersfetcher.util.EnhancedDateRangeSegmenter;
import com.commercetools.ordersfetcher.util.EnhancedDateRangeSegmenter.DateTimeSegment;

/**
 * Standalone service that demonstrates the date range segmentation and parallel
 * processing capabilities without requiring actual Commerce Tools API credentials.
 */
@Service
public class StandaloneOrderService {

    private static final Logger logger = Logger.getLogger(StandaloneOrderService.class.getName());
    private final EnhancedDateRangeSegmenter segmenter;
    private final DefaultDailyVolumeEstimator volumeEstimator;
    private final Random random = new Random();
    
    /**
     * Demo order object.
     */
    public static class Order {
        private final String id;
        private final ZonedDateTime createdAt;
        private final String customerEmail;
        private final double totalPrice;
        
        public Order(ZonedDateTime createdAt) {
            this.id = UUID.randomUUID().toString();
            this.createdAt = createdAt;
            this.customerEmail = "customer" + Math.abs(random.nextInt(1000)) + "@example.com";
            this.totalPrice = 10 + random.nextDouble() * 490; // $10-$500
        }
        
        public String getId() {
            return id;
        }
        
        public ZonedDateTime getCreatedAt() {
            return createdAt;
        }
        
        public String getCustomerEmail() {
            return customerEmail;
        }
        
        public double getTotalPrice() {
            return totalPrice;
        }
        
        @Override
        public String toString() {
            return "Order{id='" + id + "', createdAt=" + 
                  DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(createdAt) + 
                  ", total=" + String.format("$%.2f", totalPrice) + "}";
        }
    }
    
    public StandaloneOrderService() {
        this.segmenter = new EnhancedDateRangeSegmenter();
        this.volumeEstimator = new DefaultDailyVolumeEstimator();
        
        // Register some high volume dates for demo
        volumeEstimator.registerHighVolumeDate(LocalDate.of(2023, 11, 24), 15000); // Black Friday
        volumeEstimator.registerHighVolumeDate(LocalDate.of(2023, 11, 27), 12000); // Cyber Monday
        volumeEstimator.registerHighVolumeDate(LocalDate.of(2023, 12, 23), 8500);  // Christmas rush
    }
    
    /**
     * Demonstrates the adaptive segmentation for a date range.
     * 
     * @param dateRange The date range to segment
     * @return Detailed information about the segmentation
     */
    public Map<String, Object> demonstrateAdaptiveSegmentation(DateRangeRequest dateRange) {
        logger.info("Demonstrating adaptive segmentation for date range: " + 
                   dateRange.getStartDate() + " to " + dateRange.getEndDate());
                   
        // Get segments with adaptive segmentation
        List<DateTimeSegment> segments = segmenter.segmentDateRangeAdaptively(
                dateRange.getStartDate(), 
                dateRange.getEndDate(),
                volumeEstimator);
                
        Map<String, Object> result = new HashMap<>();
        result.put("dateRange", Map.of(
                "startDate", dateRange.getStartDate().toString(),
                "endDate", dateRange.getEndDate().toString(),
                "totalDays", ChronoUnit.DAYS.between(dateRange.getStartDate(), dateRange.getEndDate()) + 1
        ));
        
        result.put("segmentCount", segments.size());
        
        // Group segments by type
        Map<String, Integer> segmentTypes = new HashMap<>();
        for (DateTimeSegment segment : segments) {
            String type;
            if (segment.isHourly()) {
                type = "hourly";
            } else if (segment.getStartTime().getHour() == 0 && segment.getEndTime().getHour() == 23) {
                type = "full-day";
            } else {
                type = "half-day";
            }
            
            segmentTypes.merge(type, 1, Integer::sum);
        }
        result.put("segmentTypes", segmentTypes);
        
        // Add segment details
        List<Map<String, Object>> segmentDetails = new ArrayList<>();
        for (DateTimeSegment segment : segments) {
            String type;
            int estimatedVolume;
            
            if (segment.isHourly()) {
                type = "hourly";
                estimatedVolume = volumeEstimator.estimateVolumeForDay(segment.getStartDate()) / 24;
            } else if (segment.getStartTime().getHour() == 0 && segment.getEndTime().getHour() == 11) {
                type = "half-day-morning";
                estimatedVolume = volumeEstimator.estimateVolumeForDay(segment.getStartDate()) / 2;
            } else if (segment.getStartTime().getHour() == 12 && segment.getEndTime().getHour() == 23) {
                type = "half-day-afternoon";
                estimatedVolume = volumeEstimator.estimateVolumeForDay(segment.getStartDate()) / 2;
            } else {
                type = "full-day";
                estimatedVolume = volumeEstimator.estimateVolumeForDay(segment.getStartDate());
            }
            
            segmentDetails.add(Map.of(
                    "startDate", segment.getStartDate().toString(),
                    "endDate", segment.getEndDate().toString(),
                    "startTime", segment.getStartTime().toString(),
                    "endTime", segment.getEndTime().toString(),
                    "type", type,
                    "estimatedVolume", estimatedVolume
            ));
        }
        result.put("segments", segmentDetails);
        
        // Demonstrate performance advantage
        result.put("performanceComparison", demonstrateParallelPerformance(segments));
        
        return result;
    }
    
    /**
     * Demonstrates the performance advantage of parallel processing.
     * 
     * @param segments The segments to process
     * @return Performance comparison details
     */
    private Map<String, Object> demonstrateParallelPerformance(List<DateTimeSegment> segments) {
        Map<String, Object> result = new HashMap<>();
        
        // Create thread pool
        int threadCount = Math.min(segments.size(), Runtime.getRuntime().availableProcessors());
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        
        result.put("threadPoolSize", threadCount);
        
        // First measure sequential processing
        long startTimeSequential = System.currentTimeMillis();
        int sequentialTotal = 0;
        
        for (DateTimeSegment segment : segments) {
            int ordersInSegment = simulateProcessingSegment(segment);
            sequentialTotal += ordersInSegment;
        }
        
        long endTimeSequential = System.currentTimeMillis();
        long sequentialDuration = endTimeSequential - startTimeSequential;
        
        // Then measure parallel processing
        long startTimeParallel = System.currentTimeMillis();
        ConcurrentHashMap<String, Integer> segmentResults = new ConcurrentHashMap<>();
        
        List<CompletableFuture<Void>> futures = segments.stream()
                .map(segment -> CompletableFuture.runAsync(() -> {
                    int ordersInSegment = simulateProcessingSegment(segment);
                    segmentResults.put(segment.toString(), ordersInSegment);
                }, executor))
                .toList();
                
        // Wait for all to complete
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
        
        long endTimeParallel = System.currentTimeMillis();
        long parallelDuration = endTimeParallel - startTimeParallel;
        
        // Calculate speedup
        double speedup = (double) sequentialDuration / parallelDuration;
        
        int parallelTotal = segmentResults.values().stream().mapToInt(Integer::intValue).sum();
        
        // Add performance data to result
        result.put("sequentialProcessingMs", sequentialDuration);
        result.put("parallelProcessingMs", parallelDuration);
        result.put("speedupFactor", String.format("%.2fx", speedup));
        result.put("totalOrdersProcessed", parallelTotal);
        
        // Cleanup
        executor.shutdown();
        
        return result;
    }
    
    /**
     * Simulates processing a segment of orders.
     * 
     * @param segment The segment to process
     * @return The number of orders processed
     */
    private int simulateProcessingSegment(DateTimeSegment segment) {
        // Determine how many orders to generate based on volume estimation
        int ordersToGenerate;
        
        if (segment.isHourly()) {
            ordersToGenerate = volumeEstimator.estimateVolumeForDay(segment.getStartDate()) / 24;
        } else if (segment.getStartTime().getHour() == 0 && segment.getEndTime().getHour() == 11) {
            ordersToGenerate = volumeEstimator.estimateVolumeForDay(segment.getStartDate()) / 2;
        } else if (segment.getStartTime().getHour() == 12 && segment.getEndTime().getHour() == 23) {
            ordersToGenerate = volumeEstimator.estimateVolumeForDay(segment.getStartDate()) / 2;
        } else {
            ordersToGenerate = volumeEstimator.estimateVolumeForDay(segment.getStartDate());
        }
        
        // Throttle number for demo purposes (we don't want to generate millions of objects)
        ordersToGenerate = Math.min(ordersToGenerate, 1000);
        
        // Add some randomness
        int actualOrders = ordersToGenerate + random.nextInt(ordersToGenerate / 10) - ordersToGenerate / 20;
        actualOrders = Math.max(1, actualOrders);
        
        // Generate orders for this segment
        List<Order> orders = new ArrayList<>();
        ZonedDateTime startDateTime = segment.getStartDate().atTime(segment.getStartTime()).atZone(ZoneOffset.UTC);
        ZonedDateTime endDateTime = segment.getEndDate().atTime(segment.getEndTime()).atZone(ZoneOffset.UTC);
        
        long segmentDurationSeconds = startDateTime.until(endDateTime, ChronoUnit.SECONDS);
        
        for (int i = 0; i < actualOrders; i++) {
            // Distribute orders evenly across the segment time range
            long secondsOffset = (long) (random.nextDouble() * segmentDurationSeconds);
            ZonedDateTime orderTime = startDateTime.plusSeconds(secondsOffset);
            
            orders.add(new Order(orderTime));
        }
        
        // Simulate processing time (more orders = more time)
        try {
            // Scale processing time: 100 orders per millisecond, with some minimum time
            long processingTimeMs = Math.max(10, actualOrders / 100);
            Thread.sleep(processingTimeMs);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        return actualOrders;
    }
}