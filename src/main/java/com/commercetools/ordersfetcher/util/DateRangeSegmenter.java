package com.commercetools.ordersfetcher.util;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.Period;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
public class DateRangeSegmenter {

    @Value("${commercetools.max-estimated-records-per-day:500}")
    private int maxEstimatedRecordsPerDay;
    
    @Value("${commercetools.max-results-per-query:10000}")
    private int maxResultsPerQuery;

    /**
     * Segments a date range into smaller ranges if needed,
     * ensuring that each segment likely contains fewer than the maximum 
     * allowed records per query (10,000).
     *
     * @param startDate Start date (inclusive)
     * @param endDate End date (inclusive)
     * @return List of date range segments
     */
    public List<DateRange> segmentDateRange(LocalDate startDate, LocalDate endDate) {
        List<DateRange> segments = new ArrayList<>();
        
        // Calculate total days in the range
        long totalDays = ChronoUnit.DAYS.between(startDate, endDate) + 1;
        
        // If the date range is small enough to fit in one query
        long estimatedRecords = totalDays * maxEstimatedRecordsPerDay;
        
        if (estimatedRecords <= maxResultsPerQuery) {
            segments.add(new DateRange(startDate, endDate));
            return segments;
        }
        
        // Calculate maximum days per segment based on estimated records
        int maxDaysPerSegment = (int) (maxResultsPerQuery / maxEstimatedRecordsPerDay);
        log.info("Segmenting date range: Total days = {}, estimated records = {}, max days per segment = {}", 
                totalDays, estimatedRecords, maxDaysPerSegment);
        
        // Create segments
        LocalDate currentStart = startDate;
        while (currentStart.isBefore(endDate) || currentStart.isEqual(endDate)) {
            LocalDate currentEnd = currentStart.plusDays(maxDaysPerSegment - 1);
            
            // Ensure we don't go beyond the specified end date
            if (currentEnd.isAfter(endDate)) {
                currentEnd = endDate;
            }
            
            segments.add(new DateRange(currentStart, currentEnd));
            
            // Prepare for next segment
            currentStart = currentEnd.plusDays(1);
        }
        
        return segments;
    }
    
    /**
     * Adaptive segmentation that adjusts if we hit the record limit
     * 
     * @param startDate Start date (inclusive)
     * @param endDate End date (inclusive)
     * @param actualRecordsRetrieved Number of records actually retrieved in the last attempt
     * @return List of date range segments with adjusted granularity
     */
    public List<DateRange> adaptiveSegmentation(LocalDate startDate, LocalDate endDate, int actualRecordsRetrieved) {
        // If we received exactly the max number of records, we might have hit the limit
        if (actualRecordsRetrieved == maxResultsPerQuery) {
            // Adjust our estimate of records per day based on actual results
            long days = ChronoUnit.DAYS.between(startDate, endDate) + 1;
            int newEstimate = (int) Math.ceil((double) actualRecordsRetrieved / days);
            
            // Use a slightly higher estimate to be safe
            maxEstimatedRecordsPerDay = (int) (newEstimate * 1.2);
            
            log.info("Adjusting records-per-day estimate to {} based on query results", maxEstimatedRecordsPerDay);
        }
        
        return segmentDateRange(startDate, endDate);
    }
    
    @Data
    public static class DateRange {
        private final LocalDate startDate;
        private final LocalDate endDate;
        
        public Period getDuration() {
            return Period.between(startDate, endDate);
        }
    }
}
