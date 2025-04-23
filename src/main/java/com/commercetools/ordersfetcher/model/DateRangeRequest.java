package com.commercetools.ordersfetcher.model;

import java.time.LocalDate;

/**
 * Represents a date range request for fetching orders.
 */
public class DateRangeRequest {
    private LocalDate startDate;
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