package com.commercetools.ordersfetcher.util;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

/**
 * Utility class for segmenting date ranges into smaller chunks.
 * This is useful when handling large date ranges that might exceed the 10,000 record limit.
 */
public class DateRangeSegmenter {

    /**
     * Represents a segment of dates within a larger date range.
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
            return startDate + " to " + endDate;
        }
    }

    /**
     * Segments a date range into smaller chunks.
     * The segmentation strategy depends on the total number of days in the range:
     * - For ranges less than 30 days: no segmentation (returns the original range as a single segment)
     * - For ranges 30-90 days: segments into weekly chunks
     * - For ranges 90-365 days: segments into bi-weekly chunks
     * - For ranges over 365 days: segments into monthly chunks
     *
     * @param startDate The start date of the range
     * @param endDate The end date of the range
     * @return A list of date segments
     */
    public List<DateSegment> segmentDateRange(LocalDate startDate, LocalDate endDate) {
        List<DateSegment> segments = new ArrayList<>();
        
        // Calculate total days in the range
        long totalDays = ChronoUnit.DAYS.between(startDate, endDate) + 1; // +1 to include end date
        
        // For small ranges, don't segment
        if (totalDays <= 30) {
            segments.add(new DateSegment(startDate, endDate));
            return segments;
        }
        
        // Determine segment size based on the total range
        int segmentDays;
        if (totalDays <= 90) {
            segmentDays = 7; // Weekly chunks for 1-3 months
        } else if (totalDays <= 365) {
            segmentDays = 14; // Bi-weekly chunks for 3-12 months
        } else {
            segmentDays = 30; // Monthly chunks for > 1 year
        }
        
        // Create the segments
        LocalDate currentStart = startDate;
        while (currentStart.isBefore(endDate) || currentStart.isEqual(endDate)) {
            LocalDate currentEnd = currentStart.plusDays(segmentDays - 1);
            
            // Ensure we don't go past the overall end date
            if (currentEnd.isAfter(endDate)) {
                currentEnd = endDate;
            }
            
            segments.add(new DateSegment(currentStart, currentEnd));
            
            // Move to the next segment
            currentStart = currentEnd.plusDays(1);
        }
        
        return segments;
    }
}