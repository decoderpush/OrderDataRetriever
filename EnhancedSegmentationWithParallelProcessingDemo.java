import java.time.LocalDate;
import java.time.LocalTime;
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
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Complete demonstration of the enhanced date range segmentation with
 * hourly breakdowns for high volume days and parallel processing.
 * 
 * This demo shows how to:
 * 1. Segment a date range into manageable chunks
 * 2. Detect high-volume days that need hourly segmentation
 * 3. Process segments in parallel for improved performance
 * 4. Handle potential Commerce Tools API 10K record limit
 */
public class EnhancedSegmentationWithParallelProcessingDemo {

    private static final Random random = new Random();
    
    /**
     * Main method to run the demo.
     */
    public static void main(String[] args) {
        System.out.println("======= Commerce Tools Order Fetcher Demo =======");
        System.out.println("This demo shows how to overcome the 10,000 record limit");
        System.out.println("by using enhanced date segmentation and parallel processing.\n");
        
        // Define a date range including high-volume days
        LocalDate startDate = LocalDate.of(2023, 11, 20);
        LocalDate endDate = LocalDate.of(2023, 12, 5);
        System.out.println("Date Range: " + startDate + " to " + endDate);
        System.out.println("Total Days: " + (ChronoUnit.DAYS.between(startDate, endDate) + 1));
        
        // Create a volume estimator and register high-volume dates
        DailyVolumeEstimator volumeEstimator = new DefaultDailyVolumeEstimator();
        
        // Register some high volume dates
        registerHighVolumeDates(volumeEstimator);
        
        // Create segments for the date range
        EnhancedDateRangeSegmenter segmenter = new EnhancedDateRangeSegmenter();
        List<DateTimeSegment> segments = segmenter.segmentDateRangeAdaptively(
                startDate, endDate, volumeEstimator);
        
        System.out.println("\nCreated " + segments.size() + " segments for the date range");
        
        // Group segments by type
        Map<String, Integer> segmentTypes = groupSegmentsByType(segments, volumeEstimator);
        System.out.println("\nSegment Types:");
        segmentTypes.forEach((type, count) -> System.out.println("- " + type + ": " + count));
        
        // Run parallel vs sequential processing comparison
        System.out.println("\nRunning performance comparison...");
        Map<String, Object> performanceResults = demonstrateParallelPerformance(segments, volumeEstimator);
        
        System.out.println("\n--- PERFORMANCE RESULTS ---");
        long seqTime = (long)performanceResults.get("sequentialTimeMs");
        long parTime = (long)performanceResults.get("parallelTimeMs");
        String speedupFactor = (String)performanceResults.get("speedupFactor");
        int totalOrders = (int)performanceResults.get("totalOrdersProcessed");
        
        System.out.println("Sequential processing time:   " + seqTime + "ms");
        System.out.println("Parallel processing time:     " + parTime + "ms");
        System.out.println("Time saved:                   " + (seqTime - parTime) + "ms");
        System.out.println("Speedup factor:               " + speedupFactor + "x");
        System.out.println("Total orders processed:       " + totalOrders);
        
        // Calculate extrapolated results for a full year of data
        long yearlyOrders = totalOrders * (365 / 16); // Rough estimate for a full year based on our 16-day sample
        long seqYearlyTimeMs = seqTime * (365 / 16);
        long parYearlyTimeMs = parTime * (365 / 16);
        
        System.out.println("\n--- PROJECTED ANNUAL SAVINGS ---");
        System.out.println("For processing a year of data (" + yearlyOrders + " orders):");
        System.out.println("Sequential processing time:   " + formatTime(seqYearlyTimeMs));
        System.out.println("Parallel processing time:     " + formatTime(parYearlyTimeMs));
        System.out.println("Annual time saved:            " + formatTime(seqYearlyTimeMs - parYearlyTimeMs));
        
        System.out.println("\n======= Demo Complete =======");
    }
    
    /**
     * Formats time in milliseconds to a human-readable format.
     */
    private static String formatTime(long timeMs) {
        long seconds = timeMs / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        
        if (hours > 0) {
            return String.format("%d hours, %d minutes", hours, minutes % 60);
        } else if (minutes > 0) {
            return String.format("%d minutes, %d seconds", minutes, seconds % 60);
        } else {
            return String.format("%d seconds", seconds);
        }
    }
    
    /**
     * Registers some high volume and extreme volume dates for the demo.
     */
    private static void registerHighVolumeDates(DailyVolumeEstimator estimator) {
        System.out.println("\nHigh Volume Days:");
        
        // Black Friday (extreme volume)
        LocalDate blackFriday = LocalDate.of(2023, 11, 24);
        int blackFridayVolume = 15000;
        estimator.registerHighVolumeDate(blackFriday, blackFridayVolume);
        System.out.println("- " + blackFriday + " (Black Friday): " + blackFridayVolume + " orders");
        
        // Cyber Monday (extreme volume)
        LocalDate cyberMonday = LocalDate.of(2023, 11, 27);
        int cyberMondayVolume = 12000;
        estimator.registerHighVolumeDate(cyberMonday, cyberMondayVolume);
        System.out.println("- " + cyberMonday + " (Cyber Monday): " + cyberMondayVolume + " orders");
        
        // Pre-Christmas (high volume but not extreme)
        LocalDate preChristmas = LocalDate.of(2023, 12, 1);
        int preChristmasVolume = 8000;
        estimator.registerHighVolumeDate(preChristmas, preChristmasVolume);
        System.out.println("- " + preChristmas + " (Pre-Christmas): " + preChristmasVolume + " orders");
    }
    
    /**
     * Groups segments by their type.
     */
    private static Map<String, Integer> groupSegmentsByType(List<DateTimeSegment> segments, DailyVolumeEstimator estimator) {
        Map<String, Integer> segmentTypes = new HashMap<>();
        
        for (DateTimeSegment segment : segments) {
            String type;
            int volume = estimator.estimateVolumeForDay(segment.getStartDate());
            
            if (segment.isHourly()) {
                type = "hourly (for " + volume + " orders/day)";
            } else if (segment.getStartTime().equals(LocalTime.MIN) && segment.getEndTime().equals(LocalTime.MAX)) {
                type = "full-day";
            } else {
                type = "half-day (for " + volume + " orders/day)";
            }
            
            segmentTypes.merge(type, 1, Integer::sum);
        }
        
        return segmentTypes;
    }
    
    /**
     * Demonstrates the performance advantage of parallel processing.
     */
    private static Map<String, Object> demonstrateParallelPerformance(
            List<DateTimeSegment> segments, DailyVolumeEstimator estimator) {
        Map<String, Object> results = new HashMap<>();
        
        // Sequential processing
        long startTimeSequential = System.currentTimeMillis();
        int totalOrdersSequential = processSegmentsSequentially(segments, estimator);
        long sequentialTimeMs = System.currentTimeMillis() - startTimeSequential;
        
        // Parallel processing
        long startTimeParallel = System.currentTimeMillis();
        int totalOrdersParallel = processSegmentsInParallel(segments, estimator);
        long parallelTimeMs = System.currentTimeMillis() - startTimeParallel;
        
        // Calculate speedup
        double speedup = (double) sequentialTimeMs / parallelTimeMs;
        
        results.put("sequentialTimeMs", sequentialTimeMs);
        results.put("parallelTimeMs", parallelTimeMs);
        results.put("speedupFactor", String.format("%.2f", speedup));
        results.put("totalOrdersProcessed", totalOrdersParallel);
        
        return results;
    }
    
    /**
     * Processes segments sequentially.
     */
    private static int processSegmentsSequentially(List<DateTimeSegment> segments, DailyVolumeEstimator estimator) {
        int totalOrders = 0;
        
        System.out.println("\n--- SEQUENTIAL PROCESSING ---");
        System.out.println("Processing segments one at a time...");
        
        for (DateTimeSegment segment : segments) {
            int orderCount = processSegment(segment, estimator);
            totalOrders += orderCount;
        }
        
        System.out.println("Sequential processing completed.");
        
        return totalOrders;
    }
    
    /**
     * Processes segments in parallel.
     */
    private static int processSegmentsInParallel(List<DateTimeSegment> segments, DailyVolumeEstimator estimator) {
        // Create thread pool based on available processors
        int threadCount = Math.min(segments.size(), Runtime.getRuntime().availableProcessors());
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        
        System.out.println("\n--- PARALLEL PROCESSING ---");
        System.out.println("Processing segments with " + threadCount + " threads in parallel...");
        
        // Track results
        Map<String, Integer> results = new ConcurrentHashMap<>();
        AtomicInteger totalProcessed = new AtomicInteger(0);
        
        // Create futures for each segment
        List<CompletableFuture<Void>> futures = new ArrayList<>();
        
        for (DateTimeSegment segment : segments) {
            CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                int orderCount = processSegment(segment, estimator);
                results.put(segment.toString(), orderCount);
                totalProcessed.addAndGet(orderCount);
            }, executor);
            
            futures.add(future);
        }
        
        // Wait for all futures to complete
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
        
        // Shutdown the executor
        executor.shutdown();
        
        System.out.println("Parallel processing completed.");
        
        return totalProcessed.get();
    }
    
    /**
     * Processes a single segment.
     */
    private static int processSegment(DateTimeSegment segment, DailyVolumeEstimator estimator) {
        // Calculate orders to process
        int totalDailyVolume = estimator.estimateVolumeForDay(segment.getStartDate());
        int segmentVolume;
        
        if (segment.isHourly()) {
            segmentVolume = totalDailyVolume / 24;
        } else if (segment.getStartTime().equals(LocalTime.MIN) && segment.getEndTime().equals(LocalTime.MAX)) {
            segmentVolume = totalDailyVolume;
        } else {
            segmentVolume = totalDailyVolume / 2;
        }
        
        // Limit volume for demo purposes
        segmentVolume = Math.min(segmentVolume, 5000);
        
        // During sequential processing, we'll print less frequently to avoid cluttering the output
        boolean isMainThread = Thread.currentThread().getName().equals("main");
        
        if (!isMainThread || segment.getStartDate().getDayOfMonth() % 3 == 0) {
            // Log segment processing (either all parallel segments, or every 3rd day for sequential)
            if (segment.isHourly()) {
                System.out.println("[" + Thread.currentThread().getName() + "] Processing hourly segment: " + 
                    segment + " (" + segmentVolume + " orders)");
            } else if (segment.getStartTime().equals(LocalTime.MIN) && segment.getEndTime().equals(LocalTime.MAX)) {
                System.out.println("[" + Thread.currentThread().getName() + "] Processing full-day segment: " + 
                    segment + " (" + segmentVolume + " orders)");
            } else {
                System.out.println("[" + Thread.currentThread().getName() + "] Processing half-day segment: " + 
                    segment + " (" + segmentVolume + " orders)");
            }
        }
        
        // Simulate processing by adding delay proportional to volume
        try {
            Thread.sleep(segmentVolume / 20); // 20 orders per millisecond - simulate a more complex operation
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        return segmentVolume;
    }
    
    /**
     * Order model class.
     */
    static class Order {
        private final String id;
        private final ZonedDateTime createdAt;
        private final double totalPrice;
        
        public Order(ZonedDateTime createdAt) {
            this.id = UUID.randomUUID().toString();
            this.createdAt = createdAt;
            this.totalPrice = 10 + random.nextDouble() * 490;
        }
        
        public String getId() {
            return id;
        }
        
        public ZonedDateTime getCreatedAt() {
            return createdAt;
        }
        
        public double getTotalPrice() {
            return totalPrice;
        }
    }
    
    /**
     * Interface for estimating volume for a specific day.
     */
    interface DailyVolumeEstimator {
        int estimateVolumeForDay(LocalDate date);
        void registerHighVolumeDate(LocalDate date, int volume);
    }
    
    /**
     * Default implementation of DailyVolumeEstimator.
     */
    static class DefaultDailyVolumeEstimator implements DailyVolumeEstimator {
        private final Map<LocalDate, Integer> knownVolumes = new HashMap<>();
        
        public DefaultDailyVolumeEstimator() {
            // Default volumes by day of week
            for (LocalDate date = LocalDate.of(2023, 1, 1); 
                 date.isBefore(LocalDate.of(2024, 1, 1)); 
                 date = date.plusDays(1)) {
                 
                int defaultVolume;
                switch (date.getDayOfWeek()) {
                    case SATURDAY:
                    case SUNDAY:
                        defaultVolume = 4000; // Higher volume on weekends
                        break;
                    case MONDAY:
                    case FRIDAY:
                        defaultVolume = 3000; // Medium volume
                        break;
                    default:
                        defaultVolume = 2000; // Normal volume
                }
                
                knownVolumes.put(date, defaultVolume);
            }
        }
        
        @Override
        public int estimateVolumeForDay(LocalDate date) {
            return knownVolumes.getOrDefault(date, 2000);
        }
        
        @Override
        public void registerHighVolumeDate(LocalDate date, int volume) {
            knownVolumes.put(date, volume);
        }
    }
    
    /**
     * Enhanced date range segmenter.
     */
    static class EnhancedDateRangeSegmenter {
        
        private static final int HIGH_VOLUME_THRESHOLD = 5000;
        private static final int EXTREME_VOLUME_THRESHOLD = 10000;
        
        /**
         * Segments a date range with adaptive granularity.
         */
        public List<DateTimeSegment> segmentDateRangeAdaptively(
                LocalDate startDate, LocalDate endDate, DailyVolumeEstimator estimator) {
            
            List<DateTimeSegment> segments = new ArrayList<>();
            LocalDate currentDate = startDate;
            
            while (currentDate.isBefore(endDate) || currentDate.equals(endDate)) {
                int estimatedVolume = estimator.estimateVolumeForDay(currentDate);
                
                if (estimatedVolume > EXTREME_VOLUME_THRESHOLD) {
                    // Extreme volume - use hourly segments
                    for (int hour = 0; hour < 24; hour++) {
                        segments.add(new DateTimeSegment(currentDate, hour));
                    }
                } else if (estimatedVolume > HIGH_VOLUME_THRESHOLD) {
                    // High volume - use half-day segments
                    segments.add(new DateTimeSegment(currentDate, true));  // Morning
                    segments.add(new DateTimeSegment(currentDate, false)); // Afternoon
                } else {
                    // Normal volume - use full day
                    segments.add(new DateTimeSegment(currentDate));
                }
                
                currentDate = currentDate.plusDays(1);
            }
            
            return segments;
        }
    }
    
    /**
     * DateTimeSegment class representing a segment of time.
     */
    static class DateTimeSegment {
        private final LocalDate startDate;
        private final LocalDate endDate;
        private final LocalTime startTime;
        private final LocalTime endTime;
        private final boolean hourly;
        
        // Full day segment
        public DateTimeSegment(LocalDate date) {
            this.startDate = date;
            this.endDate = date;
            this.startTime = LocalTime.MIN;
            this.endTime = LocalTime.MAX;
            this.hourly = false;
        }
        
        // Hourly segment
        public DateTimeSegment(LocalDate date, int hour) {
            this.startDate = date;
            this.endDate = date;
            this.startTime = LocalTime.of(hour, 0, 0);
            this.endTime = LocalTime.of(hour, 59, 59, 999_999_999);
            this.hourly = true;
        }
        
        // Half-day segment
        public DateTimeSegment(LocalDate date, boolean morning) {
            this.startDate = date;
            this.endDate = date;
            if (morning) {
                this.startTime = LocalTime.of(0, 0, 0);
                this.endTime = LocalTime.of(11, 59, 59, 999_999_999);
            } else {
                this.startTime = LocalTime.of(12, 0, 0);
                this.endTime = LocalTime.of(23, 59, 59, 999_999_999);
            }
            this.hourly = false;
        }
        
        public LocalDate getStartDate() {
            return startDate;
        }
        
        public LocalDate getEndDate() {
            return endDate;
        }
        
        public LocalTime getStartTime() {
            return startTime;
        }
        
        public LocalTime getEndTime() {
            return endTime;
        }
        
        public boolean isHourly() {
            return hourly;
        }
        
        @Override
        public String toString() {
            if (hourly) {
                return String.format("%s %02d:00-%02d:59", 
                        startDate, startTime.getHour(), endTime.getHour());
            } else if (startTime.equals(LocalTime.MIN) && endTime.equals(LocalTime.MAX)) {
                return startDate.toString();
            } else {
                return String.format("%s %02d:00-%02d:59", 
                        startDate, startTime.getHour(), endTime.getHour());
            }
        }
    }
}