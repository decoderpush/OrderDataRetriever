package com.commercetools.ordersfetcher.model;

import java.time.LocalDate;

/**
 * A request for fetching orders in a date range.
 */
public class DateRangeRequest {
    private final LocalDate startDate;
    private final LocalDate endDate;
    
    /**
     * Creates a new date range request.
     * 
     * @param startDate The start date (inclusive)
     * @param endDate The end date (inclusive)
     */
    public DateRangeRequest(LocalDate startDate, LocalDate endDate) {
        this.startDate = startDate;
        this.endDate = endDate;
    }
    
    /**
     * Gets the start date.
     * 
     * @return The start date
     */
    public LocalDate getStartDate() {
        return startDate;
    }
    
    /**
     * Gets the end date.
     * 
     * @return The end date
     */
    public LocalDate getEndDate() {
        return endDate;
    }
}