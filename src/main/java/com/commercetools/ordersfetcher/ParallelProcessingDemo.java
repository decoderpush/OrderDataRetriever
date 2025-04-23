package com.commercetools.ordersfetcher;

import com.commercetools.ordersfetcher.model.DateRangeRequest;
import com.commercetools.ordersfetcher.util.DateRangeSegmenter;
import com.commercetools.ordersfetcher.util.DateRangeSegmenter.DateSegment;

import java.time.LocalDate;
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

/**
 * A standalone demo to show the parallel processing implementation.
 */
public class ParallelProcessingDemo {
    private static final Logger logger = Logger.getLogger(ParallelProcessingDemo.class.getName());
    private static final DateRangeSegmenter segmenter = new DateRangeSegmenter();
    
    public static void main(String[] args) {
        System.out.println("========== Parallel Processing Demo for Commerce Tools Orders Fetcher ==========");
        
        // Let's demonstrate with a 365-day period (1 year)
        LocalDate startDate = LocalDate.of(2022, 1, 1);  // Jan 1, 2022
        LocalDate endDate = LocalDate.of(2022, 12, 31);  // Dec 31, 2022
        DateRangeRequest dateRange = new DateRangeRequest(startDate, endDate);
        
        System.out.println("\n1. Segmenting Date Range: " + startDate + " to " + endDate);
        System.out.println("   Total days: " + (ChronoUnit.DAYS.between(startDate, endDate) + 1));
        
        List<DateSegment> segments = segmenter.segmentDateRange(startDate, endDate);
        
        System.out.println("\n2. Created " + segments.size() + " segments:");
        for (int i = 0; i < segments.size(); i++) {
            DateSegment segment = segments.get(i);
            long daysInSegment = ChronoUnit.DAYS.between(segment.getStartDate(), segment.getEndDate()) + 1;
            System.out.println("   Segment " + (i+1) + ": " + segment.getStartDate() + " to " + 
                              segment.getEndDate() + " (" + daysInSegment + " days)");
        }
        
        System.out.println("\n3. Performance Comparison:");
        
        // Run sequential processing
        System.out.println("\n   Sequential Processing:");
        long startTimeSequential = System.currentTimeMillis();
        
        int sequentialTotal = 0;
        for (DateSegment segment : segments) {
            int count = processSegment(segment);
            sequentialTotal += count;
            System.out.println("   - Processed segment " + segment + ": " + count + " records");
        }
        
        long endTimeSequential = System.currentTimeMillis();
        long sequentialDuration = endTimeSequential - startTimeSequential;
        System.out.println("   Total records: " + sequentialTotal);
        System.out.println("   Total time: " + sequentialDuration + "ms");
        
        // Run parallel processing
        System.out.println("\n   Parallel Processing:");
        long startTimeParallel = System.currentTimeMillis();
        
        // Create a thread pool for parallel processing
        int threadCount = Math.min(segments.size(), 4); // Use at most 4 threads
        System.out.println("   Using " + threadCount + " threads");
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        
        final ConcurrentHashMap<String, Integer> results = new ConcurrentHashMap<>();
        List<CompletableFuture<Void>> futures = new ArrayList<>();
        
        for (DateSegment segment : segments) {
            CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                int count = processSegment(segment);
                results.put(segment.toString(), count);
                System.out.println("   - [Thread: " + Thread.currentThread().getName() + 
                                 "] Processed segment " + segment + ": " + count + " records");
            }, executor);
            
            futures.add(future);
        }
        
        // Wait for all tasks to complete
        CompletableFuture<Void> allDone = CompletableFuture.allOf(
                futures.toArray(new CompletableFuture[0]));
        
        // This blocks until all futures complete
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
        
        System.out.println("   Total records: " + parallelTotal);
        System.out.println("   Total time: " + parallelDuration + "ms");
        
        // Show speedup factor
        double speedupFactor = (double) sequentialDuration / parallelDuration;
        System.out.println("\n4. Performance Improvement:");
        System.out.println("   Sequential: " + sequentialDuration + "ms");
        System.out.println("   Parallel:   " + parallelDuration + "ms");
        System.out.printf("   Speedup:    %.2fx faster with parallel processing\n", speedupFactor);
        
        System.out.println("\n========== Demo Complete ==========");
    }
    
    /**
     * Simulates processing a segment of data.
     * In a real application, this would fetch orders from Commerce Tools API.
     */
    private static int processSegment(DateSegment segment) {
        try {
            // Calculate how many records we would expect in this segment
            long days = ChronoUnit.DAYS.between(segment.getStartDate(), segment.getEndDate()) + 1;
            int recordCount = (int) (days * 200); // Simulate about 200 orders per day
            
            // Simulate processing time (longer for segments with more days)
            // This simulates the network and processing overhead
            Thread.sleep(50 + (days * 10)); // Base time plus time per day
            
            return recordCount;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return 0;
        }
    }
}