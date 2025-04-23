package com.commercetools.ordersfetcher.service;

import com.commercetools.api.models.order.Order;
import com.commercetools.ordersfetcher.model.DateRangeRequest;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public interface OrderService {
    
    /**
     * Fetch all orders within the given date range.
     * This method segments the date range if needed and handles pagination.
     *
     * @param dateRangeRequest The date range parameters for order retrieval
     * @return List of all orders within the date range
     */
    List<Order> fetchAllOrdersInDateRange(DateRangeRequest dateRangeRequest);
    
    /**
     * Asynchronously fetch all orders within the given date range.
     *
     * @param dateRangeRequest The date range parameters for order retrieval
     * @return CompletableFuture containing the list of all orders within the date range
     */
    CompletableFuture<List<Order>> fetchAllOrdersInDateRangeAsync(DateRangeRequest dateRangeRequest);
}
