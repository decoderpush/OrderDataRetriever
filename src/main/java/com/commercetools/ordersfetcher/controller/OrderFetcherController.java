package com.commercetools.ordersfetcher.controller;

import com.commercetools.api.models.order.Order;
import com.commercetools.ordersfetcher.model.DateRangeRequest;
import com.commercetools.ordersfetcher.service.EnhancedOrderServiceImpl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.async.DeferredResult;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ForkJoinPool;
import java.util.logging.Logger;

/**
 * REST controller for fetching orders.
 */
@RestController
@RequestMapping("/api/orders")
public class OrderFetcherController {

    private static final Logger logger = Logger.getLogger(OrderFetcherController.class.getName());
    
    @Autowired
    private EnhancedOrderServiceImpl orderService;
    
    /**
     * Fetches all orders in the given date range.
     * Uses parallel processing for better performance.
     * 
     * @param startDate The start date (inclusive)
     * @param endDate The end date (inclusive)
     * @return A response containing the orders and metadata
     */
    @GetMapping
    public ResponseEntity<Map<String, Object>> fetchOrders(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        
        logger.info("REST request to fetch orders from " + startDate + " to " + endDate);
        
        DateRangeRequest dateRange = new DateRangeRequest(startDate, endDate);
        
        long startTime = System.currentTimeMillis();
        List<Order> orders = orderService.fetchAllOrdersInDateRangeParallel(dateRange);
        long duration = System.currentTimeMillis() - startTime;
        
        logger.info("Fetched " + orders.size() + " orders in " + duration + "ms");
        
        Map<String, Object> response = Map.of(
                "startDate", startDate.toString(),
                "endDate", endDate.toString(),
                "orderCount", orders.size(),
                "processingTimeMs", duration,
                "orders", orders
        );
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Asynchronously fetches all orders in the given date range.
     * This uses non-blocking processing so it won't tie up server threads.
     * 
     * @param startDate The start date (inclusive)
     * @param endDate The end date (inclusive)
     * @return A deferred result that will be populated when the fetch completes
     */
    @GetMapping("/async")
    public DeferredResult<ResponseEntity<Map<String, Object>>> fetchOrdersAsync(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        
        logger.info("Async REST request to fetch orders from " + startDate + " to " + endDate);
        
        DeferredResult<ResponseEntity<Map<String, Object>>> deferredResult = new DeferredResult<>();
        DateRangeRequest dateRange = new DateRangeRequest(startDate, endDate);
        
        long startTime = System.currentTimeMillis();
        
        // Use common fork-join pool for the callback processing
        ForkJoinPool.commonPool().submit(() -> {
            try {
                List<Order> orders = orderService.fetchAllOrdersInDateRangeParallel(dateRange);
                long duration = System.currentTimeMillis() - startTime;
                
                logger.info("Async request completed: Fetched " + orders.size() + " orders in " + duration + "ms");
                
                Map<String, Object> response = Map.of(
                        "startDate", startDate.toString(),
                        "endDate", endDate.toString(),
                        "orderCount", orders.size(),
                        "processingTimeMs", duration,
                        "orders", orders
                );
                
                deferredResult.setResult(ResponseEntity.ok(response));
                
            } catch (Exception e) {
                logger.severe("Error processing async request: " + e.getMessage());
                deferredResult.setErrorResult(e);
            }
        });
        
        return deferredResult;
    }
}