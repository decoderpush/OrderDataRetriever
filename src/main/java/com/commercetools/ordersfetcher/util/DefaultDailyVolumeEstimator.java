package com.commercetools.ordersfetcher.util;

import com.commercetools.ordersfetcher.util.EnhancedDateRangeSegmenter.DailyVolumeEstimator;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.MonthDay;
import java.time.Year;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

/**
 * A default implementation of the DailyVolumeEstimator that uses a combination of:
 * 1. Known high volume dates (specific calendar dates like Black Friday)
 * 2. Day-of-week patterns (e.g., weekends vs weekdays)
 * 3. Historical data (if available)
 * 
 * This can be extended or replaced with custom implementations for specific business needs.
 */
public class DefaultDailyVolumeEstimator implements DailyVolumeEstimator {

    private static final Logger logger = Logger.getLogger(DefaultDailyVolumeEstimator.class.getName());
    
    // Map of specific dates that are known to have high or extreme volume
    private final Map<MonthDay, Integer> knownHighVolumeDates = new HashMap<>();
    
    // Map of day of week volume patterns
    private final Map<DayOfWeek, Integer> dayOfWeekPatterns = new HashMap<>();
    
    // Map for historical data (if available)
    private final Map<LocalDate, Integer> historicalData = new HashMap<>();
    
    // Set of years for special events that shift yearly (like Easter)
    private final Map<Integer, Set<LocalDate>> specialEventsByYear = new HashMap<>();
    
    /**
     * Creates a DefaultDailyVolumeEstimator with default values.
     * By default, it assumes:
     * - Black Friday (day after Thanksgiving) is an extreme volume day
     * - Cyber Monday is a high volume day
     * - The week before Christmas has higher volume
     * - Weekends have lower volume than weekdays
     */
    public DefaultDailyVolumeEstimator() {
        // Initialize with common retail high-volume dates
        
        // Black Friday (approximate - actual date varies)
        registerHighVolumeDate(MonthDay.of(11, 25), 15000); // Extreme volume
        registerHighVolumeDate(MonthDay.of(11, 26), 15000); // Extreme volume
        registerHighVolumeDate(MonthDay.of(11, 27), 15000); // Extreme volume
        registerHighVolumeDate(MonthDay.of(11, 28), 12000); // Extreme volume
        
        // Cyber Monday (approximate)
        registerHighVolumeDate(MonthDay.of(11, 29), 12000); // Extreme volume
        registerHighVolumeDate(MonthDay.of(11, 30), 8000);  // High volume
        
        // Pre-Christmas shopping
        registerHighVolumeDate(MonthDay.of(12, 20), 8000);  // High volume
        registerHighVolumeDate(MonthDay.of(12, 21), 8000);  // High volume
        registerHighVolumeDate(MonthDay.of(12, 22), 9000);  // High volume
        registerHighVolumeDate(MonthDay.of(12, 23), 10000); // Extreme volume
        
        // Default patterns by day of week
        dayOfWeekPatterns.put(DayOfWeek.MONDAY, 4000);    // Medium volume
        dayOfWeekPatterns.put(DayOfWeek.TUESDAY, 3500);   // Medium volume
        dayOfWeekPatterns.put(DayOfWeek.WEDNESDAY, 3500); // Medium volume
        dayOfWeekPatterns.put(DayOfWeek.THURSDAY, 4000);  // Medium volume
        dayOfWeekPatterns.put(DayOfWeek.FRIDAY, 5000);    // High volume
        dayOfWeekPatterns.put(DayOfWeek.SATURDAY, 6000);  // High volume
        dayOfWeekPatterns.put(DayOfWeek.SUNDAY, 3000);    // Lower volume
    }
    
    /**
     * Registers a specific calendar date (month and day) as a high volume date.
     * 
     * @param monthDay The month and day (e.g., MonthDay.of(11, 25) for November 25th)
     * @param expectedVolume The expected order volume for this date
     */
    public void registerHighVolumeDate(MonthDay monthDay, int expectedVolume) {
        knownHighVolumeDates.put(monthDay, expectedVolume);
        logger.info("Registered high volume date: " + monthDay + " with expected volume: " + expectedVolume);
    }
    
    /**
     * Registers a specific date as a high volume date.
     * This adds both to the historical data and specific date patterns.
     * 
     * @param date The specific date
     * @param expectedVolume The expected order volume for this date
     */
    public void registerHighVolumeDate(LocalDate date, int expectedVolume) {
        // Add to historical data
        historicalData.put(date, expectedVolume);
        // Also add to month-day patterns for recurring yearly events
        registerHighVolumeDate(MonthDay.from(date), expectedVolume);
    }
    
    /**
     * Registers a specific date in a specific year as a special event date.
     * This is useful for events that change dates each year (like Easter)
     * 
     * @param date The specific date
     * @param expectedVolume The expected order volume for this date
     */
    public void registerSpecialEventDate(LocalDate date, int expectedVolume) {
        int year = date.getYear();
        
        // Initialize the set for this year if it doesn't exist
        specialEventsByYear.putIfAbsent(year, new HashSet<>());
        
        // Add the date to the special events for this year
        specialEventsByYear.get(year).add(date);
        
        // Add the volume to the historical data
        historicalData.put(date, expectedVolume);
        
        logger.info("Registered special event date: " + date + " with expected volume: " + expectedVolume);
    }
    
    /**
     * Adds historical data for a specific date.
     * This could be loaded from a database of actual order volumes.
     * 
     * @param date The specific date
     * @param actualVolume The actual order volume that was processed on this date
     */
    public void addHistoricalData(LocalDate date, int actualVolume) {
        historicalData.put(date, actualVolume);
    }
    
    @Override
    public int estimateVolumeForDay(LocalDate date) {
        // Check if we have historical data for this specific date
        if (historicalData.containsKey(date)) {
            int volume = historicalData.get(date);
            logger.fine("Using historical data for " + date + ": " + volume);
            return volume;
        }
        
        // Check if this is a special event date for this year
        Set<LocalDate> specialEventsThisYear = specialEventsByYear.getOrDefault(date.getYear(), new HashSet<>());
        if (specialEventsThisYear.contains(date)) {
            int volume = historicalData.get(date);
            logger.fine("Using special event data for " + date + ": " + volume);
            return volume;
        }
        
        // Check if this is a known high-volume date (by month and day)
        MonthDay monthDay = MonthDay.from(date);
        if (knownHighVolumeDates.containsKey(monthDay)) {
            int volume = knownHighVolumeDates.get(monthDay);
            logger.fine("Using known high volume date pattern for " + date + ": " + volume);
            return volume;
        }
        
        // Fall back to day of week patterns
        DayOfWeek dayOfWeek = date.getDayOfWeek();
        if (dayOfWeekPatterns.containsKey(dayOfWeek)) {
            int volume = dayOfWeekPatterns.get(dayOfWeek);
            logger.fine("Using day of week pattern for " + date + " (" + dayOfWeek + "): " + volume);
            return volume;
        }
        
        // Default estimate if no other patterns match
        logger.fine("No pattern matched for " + date + ", using default volume estimate");
        return 2000; // Default daily volume
    }
}