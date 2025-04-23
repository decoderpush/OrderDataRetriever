package com.commercetools.ordersfetcher.service;

import com.commercetools.api.models.order.Order;
import com.commercetools.ordersfetcher.model.DateRangeRequest;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Service for fetching orders from CommerceTool API.
 */
public interface OrderService {

    /**
     * Fetches all orders in the given date range (sequential processing)
     * 
     * @param dateRange The date range to fetch orders for
     * @return A list of all orders in the date range
     */
    List<Order> fetchAllOrdersInDateRange(DateRangeRequest dateRange);
    
    /**
     * Fetches all orders in the given date range using parallel processing
     * 
     * @param dateRange The date range to fetch orders for
     * @return A list of all orders in the date range
     */
    List<Order> fetchAllOrdersInDateRangeParallel(DateRangeRequest dateRange);
    
    /**
     * Asynchronously fetches all orders in the given date range
     * 
     * @param dateRange The date range to fetch orders for
     * @return A future that will complete with the list of all orders in the date range
     */
    CompletableFuture<List<Order>> fetchAllOrdersInDateRangeAsync(DateRangeRequest dateRange);
}