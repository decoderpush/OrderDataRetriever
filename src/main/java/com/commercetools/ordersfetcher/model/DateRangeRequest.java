package com.commercetools.ordersfetcher.model;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;

import java.time.LocalDate;

/**
 * Represents a date range request for fetching orders.
 * 
 * Note: In a real implementation, we would use Lombok annotations (@Data, etc.)
 * but for demonstration purposes we're using plain Java.
 */
public class DateRangeRequest {

    @NotNull(message = "Start date is required")
    @PastOrPresent(message = "Start date must be in the past or present")
    private LocalDate startDate;

    @NotNull(message = "End date is required")
    @PastOrPresent(message = "End date must be in the past or present")
    private LocalDate endDate;
    
    public DateRangeRequest() {
    }
    
    public DateRangeRequest(LocalDate startDate, LocalDate endDate) {
        this.startDate = startDate;
        this.endDate = endDate;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
    }
}