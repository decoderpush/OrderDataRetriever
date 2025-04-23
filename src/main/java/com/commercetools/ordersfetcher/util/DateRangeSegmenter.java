package com.commercetools.ordersfetcher.util;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * Utility class to segment a date range into smaller chunks for parallel processing.
 */
public class DateRangeSegmenter {

    private static final Logger logger = Logger.getLogger(DateRangeSegmenter.class.getName());
    
    // Default segment size in days (can be adjusted based on expected data volume)
    private static final int DEFAULT_SEGMENT_SIZE_DAYS = 30;
    
    /**
     * Segments a date range into smaller chunks for parallel processing.
     * @param startDate The start date of the range
     * @param endDate The end date of the range
     * @return A list of date segments
     */
    public List<DateSegment> segmentDateRange(LocalDate startDate, LocalDate endDate) {
        logger.info("Segmenting date range from " + startDate + " to " + endDate);
        
        List<DateSegment> segments = new ArrayList<>();
        
        // Calculate total days in the range
        long totalDays = ChronoUnit.DAYS.between(startDate, endDate) + 1;
        
        // If the range is small enough, don't segment
        if (totalDays <= DEFAULT_SEGMENT_SIZE_DAYS) {
            segments.add(new DateSegment(startDate, endDate));
            logger.info("Date range small enough for single segment: " + totalDays + " days");
            return segments;
        }
        
        // Calculate number of segments
        int segmentCount = (int) Math.ceil((double) totalDays / DEFAULT_SEGMENT_SIZE_DAYS);
        logger.info("Creating " + segmentCount + " segments for " + totalDays + " days");
        
        // Create segments
        LocalDate currentStart = startDate;
        for (int i = 0; i < segmentCount; i++) {
            // Calculate segment end date
            LocalDate currentEnd;
            if (i == segmentCount - 1) {
                // Last segment ends with the original end date
                currentEnd = endDate;
            } else {
                // Calculate end date for this segment
                currentEnd = currentStart.plusDays(DEFAULT_SEGMENT_SIZE_DAYS - 1);
                // Ensure we don't go beyond the original end date
                if (currentEnd.isAfter(endDate)) {
                    currentEnd = endDate;
                }
            }
            
            // Add segment
            segments.add(new DateSegment(currentStart, currentEnd));
            logger.fine("Created segment " + (i + 1) + ": " + currentStart + " to " + currentEnd);
            
            // Set start date for next segment
            currentStart = currentEnd.plusDays(1);
            
            // Break if we've reached the end date
            if (currentStart.isAfter(endDate)) {
                break;
            }
        }
        
        logger.info("Created " + segments.size() + " date segments");
        return segments;
    }
    
    /**
     * Represents a segment of a date range.
     */
    public static class DateSegment {
        private final LocalDate startDate;
        private final LocalDate endDate;
        
        public DateSegment(LocalDate startDate, LocalDate endDate) {
            this.startDate = startDate;
            this.endDate = endDate;
        }
        
        public LocalDate getStartDate() {
            return startDate;
        }
        
        public LocalDate getEndDate() {
            return endDate;
        }
        
        @Override
        public String toString() {
            return "DateSegment{" +
                    "startDate=" + startDate +
                    ", endDate=" + endDate +
                    '}';
        }
    }
}