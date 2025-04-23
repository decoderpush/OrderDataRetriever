package com.commercetools.ordersfetcher.demo;

import com.commercetools.ordersfetcher.model.DateRangeRequest;
import com.commercetools.ordersfetcher.util.DefaultDailyVolumeEstimator;
import com.commercetools.ordersfetcher.util.EnhancedDateRangeSegmenter;
import com.commercetools.ordersfetcher.util.EnhancedDateRangeSegmenter.DateTimeSegment;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.Month;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

/**
 * Complete demo of the enhanced order fetcher with all features.
 * This demo runs when Spring Boot starts up with the "demo" profile.
 */
@Component
@Profile("demo")
public class CompleteOrderFetcherDemo implements CommandLineRunner {

    private static final Logger logger = Logger.getLogger(CompleteOrderFetcherDemo.class.getName());
    
    @Override
    public void run(String... args) {
        logger.info("Starting Complete Order Fetcher Demo");
        
        runAdvancedSegmentationDemo();
        
        logger.info("Demo completed successfully!");
    }
    
    /**
     * Demo showcasing the advanced segmentation for high-volume periods.
     */
    private void runAdvancedSegmentationDemo() {
        logger.info("=== Advanced Segmentation Demo ===");
        
        // Create a test date range that includes high-volume days
        LocalDate startDate = LocalDate.of(2023, Month.NOVEMBER, 20);
        LocalDate endDate = LocalDate.of(2023, Month.DECEMBER, 5);
        DateRangeRequest dateRange = new DateRangeRequest(startDate, endDate);
        
        // Create volume estimator with some high-volume days
        DefaultDailyVolumeEstimator volumeEstimator = new DefaultDailyVolumeEstimator();
        
        // Override defaults to add some extreme volume days for demo
        // Black Friday (Nov 24, 2023)
        volumeEstimator.registerHighVolumeDate(LocalDate.of(2023, Month.NOVEMBER, 24), 15000);
        // Cyber Monday (Nov 27, 2023)
        volumeEstimator.registerHighVolumeDate(LocalDate.of(2023, Month.NOVEMBER, 27), 12000);
        // Also a high but not extreme volume day
        volumeEstimator.registerHighVolumeDate(LocalDate.of(2023, Month.DECEMBER, 1), 8000);
        
        logger.info("Date range: " + startDate + " to " + endDate + 
                   " (" + ChronoUnit.DAYS.between(startDate, endDate) + " days)");
        
        // Create segments with adaptive segmentation
        EnhancedDateRangeSegmenter segmenter = new EnhancedDateRangeSegmenter();
        List<DateTimeSegment> segments = segmenter.segmentDateRangeAdaptively(
                dateRange.getStartDate(), 
                dateRange.getEndDate(),
                volumeEstimator::estimateVolumeForDay);
        
        logger.info("Created " + segments.size() + " adaptive segments for the date range");
        
        // Group segments by type for reporting
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
        
        logger.info("Segment breakdown: " + segmentTypes);
        
        // Demonstrate parallel processing
        logger.info("Demonstrating parallel processing of segments...");
        
        ExecutorService executor = Executors.newFixedThreadPool(4);
        AtomicInteger totalOrdersProcessed = new AtomicInteger(0);
        ConcurrentHashMap<String, Integer> segmentResults = new ConcurrentHashMap<>();
        
        long startTime = System.currentTimeMillis();
        
        // Launch parallel tasks
        List<CompletableFuture<Void>> futures = segments.stream()
                .map(segment -> CompletableFuture.runAsync(() -> {
                    // Simulate processing by estimating volume and adding delay
                    int estimatedVolume;
                    if (segment.isHourly()) {
                        estimatedVolume = volumeEstimator.estimateVolumeForDay(segment.getStartDate()) / 24;
                    } else if (segment.getStartTime().getHour() == 0 && segment.getEndTime().getHour() == 11) {
                        estimatedVolume = volumeEstimator.estimateVolumeForDay(segment.getStartDate()) / 2;
                    } else if (segment.getStartTime().getHour() == 12 && segment.getEndTime().getHour() == 23) {
                        estimatedVolume = volumeEstimator.estimateVolumeForDay(segment.getStartDate()) / 2;
                    } else {
                        estimatedVolume = volumeEstimator.estimateVolumeForDay(segment.getStartDate());
                    }
                    
                    // Add artificial processing delay
                    try {
                        Thread.sleep(estimatedVolume / 500); // 500 orders per millisecond
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                    
                    // Store results
                    segmentResults.put(segment.toString(), estimatedVolume);
                    int totalProcessed = totalOrdersProcessed.addAndGet(estimatedVolume);
                    
                    logger.info(Thread.currentThread().getName() + " processed " + 
                               segment + ": " + estimatedVolume + " orders, total: " + totalProcessed);
                }, executor))
                .toList();
        
        // Wait for all tasks to complete
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
        
        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;
        
        logger.info("Parallel processing completed in " + duration + "ms");
        logger.info("Total orders processed: " + totalOrdersProcessed.get());
        
        // Sequential processing for comparison
        long startTimeSeq = System.currentTimeMillis();
        
        int sequentialTotal = 0;
        for (DateTimeSegment segment : segments) {
            int estimatedVolume;
            if (segment.isHourly()) {
                estimatedVolume = volumeEstimator.estimateVolumeForDay(segment.getStartDate()) / 24;
            } else if (segment.getStartTime().getHour() == 0 && segment.getEndTime().getHour() == 11) {
                estimatedVolume = volumeEstimator.estimateVolumeForDay(segment.getStartDate()) / 2;
            } else if (segment.getStartTime().getHour() == 12 && segment.getEndTime().getHour() == 23) {
                estimatedVolume = volumeEstimator.estimateVolumeForDay(segment.getStartDate()) / 2;
            } else {
                estimatedVolume = volumeEstimator.estimateVolumeForDay(segment.getStartDate());
            }
            
            // Add artificial processing delay
            try {
                Thread.sleep(estimatedVolume / 500); // 500 orders per millisecond
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            
            sequentialTotal += estimatedVolume;
        }
        
        long endTimeSeq = System.currentTimeMillis();
        long durationSeq = endTimeSeq - startTimeSeq;
        
        logger.info("Sequential processing completed in " + durationSeq + "ms");
        logger.info("Total orders processed: " + sequentialTotal);
        
        // Calculate speedup
        double speedup = (double) durationSeq / duration;
        logger.info(String.format("Parallel processing speedup: %.2fx", speedup));
        
        // Cleanup
        executor.shutdown();
    }
}