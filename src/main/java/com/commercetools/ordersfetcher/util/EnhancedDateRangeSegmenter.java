package com.commercetools.ordersfetcher.util;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * Enhanced date range segmenter that provides more granular segmentation
 * for high-volume periods, including hourly segmentation when needed.
 */
public class EnhancedDateRangeSegmenter extends DateRangeSegmenter {

    private static final Logger logger = Logger.getLogger(EnhancedDateRangeSegmenter.class.getName());
    
    /**
     * Configuration for volume thresholds
     */
    private final int highVolumeDailyThreshold;
    private final int extremeVolumeDailyThreshold;
    
    /**
     * Creates an EnhancedDateRangeSegmenter with default thresholds.
     * Default high volume: 5,000 orders/day (will use half-day segments)
     * Default extreme volume: 10,000 orders/day (will use hourly segments)
     */
    public EnhancedDateRangeSegmenter() {
        this(5000, 10000);
    }
    
    /**
     * Creates an EnhancedDateRangeSegmenter with custom thresholds.
     * 
     * @param highVolumeDailyThreshold The threshold for high volume days (will use half-day segments)
     * @param extremeVolumeDailyThreshold The threshold for extreme volume days (will use hourly segments)
     */
    public EnhancedDateRangeSegmenter(int highVolumeDailyThreshold, int extremeVolumeDailyThreshold) {
        this.highVolumeDailyThreshold = highVolumeDailyThreshold;
        this.extremeVolumeDailyThreshold = extremeVolumeDailyThreshold;
    }
    
    /**
     * A date-time segment that includes a date and optional time components.
     * This extends the regular DateSegment to support time-based segments.
     */
    public static class DateTimeSegment extends DateSegment {
        private final LocalTime startTime;
        private final LocalTime endTime;
        private final boolean isHourly;
        
        /**
         * Creates a full-day segment (from 00:00:00 to 23:59:59)
         */
        public DateTimeSegment(LocalDate startDate, LocalDate endDate) {
            super(startDate, endDate);
            this.startTime = LocalTime.MIN;
            this.endTime = LocalTime.MAX;
            this.isHourly = false;
        }
        
        /**
         * Creates a segment with specific start and end times
         */
        public DateTimeSegment(LocalDate startDate, LocalDate endDate, LocalTime startTime, LocalTime endTime) {
            super(startDate, endDate);
            this.startTime = startTime;
            this.endTime = endTime;
            this.isHourly = startTime.getHour() == endTime.getHour() && 
                           startTime.equals(LocalTime.of(startTime.getHour(), 0, 0)) &&
                           endTime.equals(LocalTime.of(endTime.getHour(), 59, 59, 999_999_999));
        }
        
        /**
         * Creates an hourly segment for a specific hour of the day
         */
        public DateTimeSegment(LocalDate date, int hour) {
            super(date, date);
            this.startTime = LocalTime.of(hour, 0, 0);
            this.endTime = LocalTime.of(hour, 59, 59, 999_999_999);
            this.isHourly = true;
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
            if (getStartDate().equals(getEndDate())) {
                if (startTime.equals(LocalTime.MIN) && endTime.equals(LocalTime.MAX)) {
                    // Full day segment
                    return getStartDate().toString();
                } else {
                    // Partial day segment
                    return getStartDate() + " " + startTime + " to " + endTime;
                }
            } else {
                if (startTime.equals(LocalTime.MIN) && endTime.equals(LocalTime.MAX)) {
                    // Multi-day segment (full days)
                    return getStartDate() + " to " + getEndDate();
                } else {
                    // Multi-day segment with specific times
                    return getStartDate() + " " + startTime + " to " + 
                           getEndDate() + " " + endTime;
                }
            }
        }
    }
    
    /**
     * Segments a date range with adaptive granularity based on expected volume.
     * For high-volume days, it will create more granular segments (half-day or hourly).
     * 
     * @param startDate The start date
     * @param endDate The end date
     * @param volumeEstimator A function to estimate the volume for a given date
     * @return A list of date-time segments
     */
    public List<DateTimeSegment> segmentDateRangeAdaptively(LocalDate startDate, LocalDate endDate, 
                                                        DailyVolumeEstimator volumeEstimator) {
        logger.info("Segmenting date range adaptively from " + startDate + " to " + endDate);
        
        List<DateTimeSegment> segments = new ArrayList<>();
        
        // Calculate total days in the range
        long totalDays = ChronoUnit.DAYS.between(startDate, endDate) + 1;
        
        // For a single day, check if we need finer granularity
        if (totalDays == 1) {
            int estimatedVolume = volumeEstimator.estimateVolumeForDay(startDate);
            logger.info("Single day segment: " + startDate + ", estimated volume: " + estimatedVolume);
            
            if (estimatedVolume > extremeVolumeDailyThreshold) {
                // Extreme volume - use hourly segments
                logger.info("Using hourly segments for extreme volume day: " + startDate);
                for (int hour = 0; hour < 24; hour++) {
                    segments.add(new DateTimeSegment(startDate, hour));
                }
            } else if (estimatedVolume > highVolumeDailyThreshold) {
                // High volume - use half-day segments
                logger.info("Using half-day segments for high volume day: " + startDate);
                segments.add(new DateTimeSegment(startDate, startDate, 
                        LocalTime.of(0, 0, 0), LocalTime.of(11, 59, 59, 999_999_999)));
                segments.add(new DateTimeSegment(startDate, startDate, 
                        LocalTime.of(12, 0, 0), LocalTime.of(23, 59, 59, 999_999_999)));
            } else {
                // Normal volume - just use the whole day
                segments.add(new DateTimeSegment(startDate, endDate));
            }
            return segments;
        }
        
        // For multi-day periods, use the standard segmentation but check each segment for high-volume days
        List<DateSegment> standardSegments = super.segmentDateRange(startDate, endDate);
        
        for (DateSegment standardSegment : standardSegments) {
            LocalDate segStartDate = standardSegment.getStartDate();
            LocalDate segEndDate = standardSegment.getEndDate();
            
            // Check if this is a multi-day segment
            if (segStartDate.equals(segEndDate)) {
                // Single-day segment - check volume
                int estimatedVolume = volumeEstimator.estimateVolumeForDay(segStartDate);
                
                if (estimatedVolume > extremeVolumeDailyThreshold) {
                    // Extreme volume - use hourly segments
                    logger.info("Using hourly segments for extreme volume day: " + segStartDate);
                    for (int hour = 0; hour < 24; hour++) {
                        segments.add(new DateTimeSegment(segStartDate, hour));
                    }
                } else if (estimatedVolume > highVolumeDailyThreshold) {
                    // High volume - use half-day segments
                    logger.info("Using half-day segments for high volume day: " + segStartDate);
                    segments.add(new DateTimeSegment(segStartDate, segStartDate, 
                            LocalTime.of(0, 0, 0), LocalTime.of(11, 59, 59, 999_999_999)));
                    segments.add(new DateTimeSegment(segStartDate, segStartDate, 
                            LocalTime.of(12, 0, 0), LocalTime.of(23, 59, 59, 999_999_999)));
                } else {
                    // Normal volume - just use the whole day
                    segments.add(new DateTimeSegment(segStartDate, segEndDate));
                }
            } else {
                // Multi-day segment
                // Since our regular segmenter already creates reasonably sized chunks,
                // we'll check each day in the segment for high volume
                LocalDate currentDate = segStartDate;
                while (currentDate.isBefore(segEndDate) || currentDate.equals(segEndDate)) {
                    int estimatedVolume = volumeEstimator.estimateVolumeForDay(currentDate);
                    
                    if (estimatedVolume > extremeVolumeDailyThreshold) {
                        // Extreme volume - use hourly segments
                        logger.info("Using hourly segments for extreme volume day: " + currentDate);
                        for (int hour = 0; hour < 24; hour++) {
                            segments.add(new DateTimeSegment(currentDate, hour));
                        }
                    } else if (estimatedVolume > highVolumeDailyThreshold) {
                        // High volume - use half-day segments
                        logger.info("Using half-day segments for high volume day: " + currentDate);
                        segments.add(new DateTimeSegment(currentDate, currentDate, 
                                LocalTime.of(0, 0, 0), LocalTime.of(11, 59, 59, 999_999_999)));
                        segments.add(new DateTimeSegment(currentDate, currentDate, 
                                LocalTime.of(12, 0, 0), LocalTime.of(23, 59, 59, 999_999_999)));
                    } else {
                        // Normal volume - just use the whole day
                        segments.add(new DateTimeSegment(currentDate, currentDate));
                    }
                    
                    currentDate = currentDate.plusDays(1);
                }
            }
        }
        
        logger.info("Created " + segments.size() + " adaptive segments for " + totalDays + " days");
        return segments;
    }
    
    /**
     * Simple implementation that segments a date range with known high-volume dates.
     * This is useful when you know specific dates that have high order volumes.
     * 
     * @param startDate The start date
     * @param endDate The end date
     * @param highVolumeDates List of dates known to have high volume (will use half-day segments)
     * @param extremeVolumeDates List of dates known to have extreme volume (will use hourly segments)
     * @return A list of date-time segments
     */
    public List<DateTimeSegment> segmentDateRangeWithKnownHighVolumeDates(
            LocalDate startDate, 
            LocalDate endDate,
            List<LocalDate> highVolumeDates,
            List<LocalDate> extremeVolumeDates) {
        
        return segmentDateRangeAdaptively(startDate, endDate, date -> {
            if (extremeVolumeDates.contains(date)) {
                return extremeVolumeDailyThreshold + 1; // Ensure it's treated as extreme
            } else if (highVolumeDates.contains(date)) {
                return highVolumeDailyThreshold + 1; // Ensure it's treated as high volume
            } else {
                return 0; // Normal volume
            }
        });
    }
    
    /**
     * Functional interface for estimating the volume for a specific day.
     * Implementations could use historical data, day-of-week patterns, 
     * or other heuristics to estimate volume.
     */
    @FunctionalInterface
    public interface DailyVolumeEstimator {
        /**
         * Estimates the expected volume for a specific day.
         * 
         * @param date The date to estimate for
         * @return The estimated number of orders for that day
         */
        int estimateVolumeForDay(LocalDate date);
    }
}