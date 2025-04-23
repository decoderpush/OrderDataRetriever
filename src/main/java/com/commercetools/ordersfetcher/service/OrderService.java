package com.commercetools.ordersfetcher.service;

import com.commercetools.api.models.order.Order;
import com.commercetools.ordersfetcher.model.DateRangeRequest;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Service for fetching orders from Commerce Tools
 */
public interface OrderService {

    /**
     * Fetches all orders in the given date range, handling the Commerce Tools
     * API 10,000 record limit.
     *
     * @param dateRangeRequest The date range to fetch orders for
     * @return A complete list of all orders in the date range
     */
    List<Order> fetchAllOrdersInDateRange(DateRangeRequest dateRangeRequest);
    
    /**
     * Asynchronously fetches all orders in the given date range, handling 
     * the Commerce Tools API 10,000 record limit.
     *
     * @param dateRangeRequest The date range to fetch orders for
     * @return A CompletableFuture containing the complete list of all orders in the date range
     */
    CompletableFuture<List<Order>> fetchAllOrdersInDateRangeAsync(DateRangeRequest dateRangeRequest);
}