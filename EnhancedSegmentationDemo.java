import java.time.LocalDate;
import java.time.LocalTime;
import java.time.MonthDay;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * This demo demonstrates how the enhanced date segmentation strategy works
 * for high-volume days with more than 10,000 orders.
 */
public class EnhancedSegmentationDemo {

    // Represents a date-time segment
    static class DateTimeSegment {
        private final LocalDate startDate;
        private final LocalDate endDate;
        private final LocalTime startTime;
        private final LocalTime endTime;
        private final boolean isHourly;
        
        // Full day segment
        public DateTimeSegment(LocalDate date) {
            this.startDate = date;
            this.endDate = date;
            this.startTime = LocalTime.MIN;
            this.endTime = LocalTime.MAX;
            this.isHourly = false;
        }
        
        // Hourly segment
        public DateTimeSegment(LocalDate date, int hour) {
            this.startDate = date;
            this.endDate = date;
            this.startTime = LocalTime.of(hour, 0, 0);
            this.endTime = LocalTime.of(hour, 59, 59, 999_999_999);
            this.isHourly = true;
        }
        
        // Half-day segment
        public DateTimeSegment(LocalDate date, boolean isMorning) {
            this.startDate = date;
            this.endDate = date;
            if (isMorning) {
                this.startTime = LocalTime.of(0, 0, 0);
                this.endTime = LocalTime.of(11, 59, 59, 999_999_999);
            } else {
                this.startTime = LocalTime.of(12, 0, 0);
                this.endTime = LocalTime.of(23, 59, 59, 999_999_999);
            }
            this.isHourly = false;
        }
        
        // Date range segment (full days)
        public DateTimeSegment(LocalDate startDate, LocalDate endDate) {
            this.startDate = startDate;
            this.endDate = endDate;
            this.startTime = LocalTime.MIN;
            this.endTime = LocalTime.MAX;
            this.isHourly = false;
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
            return isHourly;
        }
        
        @Override
        public String toString() {
            if (startDate.equals(endDate)) {
                if (isHourly) {
                    return String.format("%s %02d:00-%02d:59", 
                        startDate, startTime.getHour(), endTime.getHour());
                } else if (startTime.equals(LocalTime.MIN) && endTime.equals(LocalTime.MAX)) {
                    return startDate.toString();
                } else {
                    return String.format("%s %02d:%02d-%02d:%02d", 
                        startDate, 
                        startTime.getHour(), startTime.getMinute(),
                        endTime.getHour(), endTime.getMinute());
                }
            } else {
                return startDate + " to " + endDate;
            }
        }
    }
    
    // Simple volume estimator
    static class VolumeEstimator {
        private final Map<LocalDate, Integer> expectedVolumes = new ConcurrentHashMap<>();
        
        public VolumeEstimator() {
            // Add some high volume dates
            setExpectedVolume(LocalDate.of(2022, 11, 25), 15000); // Black Friday
            setExpectedVolume(LocalDate.of(2022, 11, 28), 12000); // Cyber Monday
            setExpectedVolume(LocalDate.of(2022, 12, 21), 6000);  // Holiday shopping
            setExpectedVolume(LocalDate.of(2022, 12, 22), 7000);  // Holiday shopping
            setExpectedVolume(LocalDate.of(2022, 12, 23), 11000); // Last minute holiday shopping
        }
        
        public void setExpectedVolume(LocalDate date, int volume) {
            expectedVolumes.put(date, volume);
        }
        
        public int estimateVolumeForDay(LocalDate date) {
            // Return expected volume if we have it
            if (expectedVolumes.containsKey(date)) {
                return expectedVolumes.get(date);
            }
            
            // Weekends have more volume
            if (date.getDayOfWeek().getValue() >= 6) {
                return 4000;
            }
            
            // Default is medium volume
            return 2000;
        }
    }
    
    public static void main(String[] args) {
        System.out.println("========== Enhanced Date Segmentation Demo ==========");
        
        // Let's test with a date range including some high-volume days
        LocalDate startDate = LocalDate.of(2022, 11, 20);
        LocalDate endDate = LocalDate.of(2022, 11, 30);
        
        // Create our volume estimator
        VolumeEstimator volumeEstimator = new VolumeEstimator();
        
        // Get segments with adaptive segmentation for high-volume days
        List<DateTimeSegment> segments = segmentDateRangeAdaptively(startDate, endDate, volumeEstimator);
        
        System.out.println("\nDate range: " + startDate + " to " + endDate);
        System.out.println("Created " + segments.size() + " segments");
        
        // Show segments
        System.out.println("\nSegments:");
        for (int i = 0; i < segments.size(); i++) {
            DateTimeSegment segment = segments.get(i);
            int estimatedVolume = volumeEstimator.estimateVolumeForDay(segment.getStartDate());
            
            String segmentType;
            if (segment.isHourly()) {
                segmentType = "hourly";
            } else if (segment.getStartTime().equals(LocalTime.MIN) && segment.getEndTime().equals(LocalTime.MAX)) {
                segmentType = "full-day";
            } else {
                segmentType = "half-day";
            }
            
            System.out.println((i+1) + ". " + segment + 
                               " [" + segmentType + ", est. volume: " + estimatedVolume + "]");
        }
        
        // Demonstrate parallel processing of these segments
        System.out.println("\nSimulating parallel processing of segments...");
        simulateParallelProcessing(segments, volumeEstimator);
        
        System.out.println("\n========== Demo Complete ==========");
    }
    
    /**
     * Segments a date range with adaptive granularity based on expected volume.
     */
    private static List<DateTimeSegment> segmentDateRangeAdaptively(
            LocalDate startDate, LocalDate endDate, VolumeEstimator volumeEstimator) {
        
        List<DateTimeSegment> segments = new ArrayList<>();
        final int HIGH_VOLUME_THRESHOLD = 5000;
        final int EXTREME_VOLUME_THRESHOLD = 10000;
        
        // Process each day in the range
        LocalDate currentDate = startDate;
        while (currentDate.isBefore(endDate) || currentDate.equals(endDate)) {
            int estimatedVolume = volumeEstimator.estimateVolumeForDay(currentDate);
            
            if (estimatedVolume > EXTREME_VOLUME_THRESHOLD) {
                // Extreme volume - use hourly segments
                System.out.println("Using hourly segments for " + currentDate + 
                                  " (est. volume: " + estimatedVolume + ")");
                for (int hour = 0; hour < 24; hour++) {
                    segments.add(new DateTimeSegment(currentDate, hour));
                }
            } else if (estimatedVolume > HIGH_VOLUME_THRESHOLD) {
                // High volume - use half-day segments
                System.out.println("Using half-day segments for " + currentDate + 
                                  " (est. volume: " + estimatedVolume + ")");
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
    
    /**
     * Simulates parallel processing of segments.
     */
    private static void simulateParallelProcessing(
            List<DateTimeSegment> segments, VolumeEstimator volumeEstimator) {
        
        ExecutorService executor = Executors.newFixedThreadPool(4);
        AtomicInteger totalProcessed = new AtomicInteger(0);
        
        // Process each segment in parallel
        List<Future<?>> futures = new ArrayList<>();
        
        long startTime = System.currentTimeMillis();
        
        for (DateTimeSegment segment : segments) {
            Future<?> future = executor.submit(() -> {
                String threadName = Thread.currentThread().getName();
                
                // Estimate volume for this segment
                int volume;
                if (segment.isHourly()) {
                    // Divide the daily volume by 24 for hourly segments
                    volume = volumeEstimator.estimateVolumeForDay(segment.getStartDate()) / 24;
                } else if (!segment.getStartTime().equals(LocalTime.MIN) || 
                           !segment.getEndTime().equals(LocalTime.MAX)) {
                    // For half-day segments, divide by 2
                    volume = volumeEstimator.estimateVolumeForDay(segment.getStartDate()) / 2;
                } else {
                    // Full day segment
                    volume = volumeEstimator.estimateVolumeForDay(segment.getStartDate());
                }
                
                // Simulate processing time based on volume
                try {
                    Thread.sleep(volume / 100); // 100 orders per millisecond
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                
                int processed = totalProcessed.addAndGet(volume);
                System.out.println("[" + threadName + "] Processed segment " + segment + 
                                  ": " + volume + " orders, total: " + processed);
            });
            
            futures.add(future);
        }
        
        // Wait for all futures to complete
        for (Future<?> future : futures) {
            try {
                future.get();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        
        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;
        
        System.out.println("\nTotal orders processed: " + totalProcessed.get());
        System.out.println("Total processing time: " + duration + "ms");
        
        // Shutdown executor
        executor.shutdown();
    }
}