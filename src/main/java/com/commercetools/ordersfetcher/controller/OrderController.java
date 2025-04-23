package com.commercetools.ordersfetcher.controller;

import com.commercetools.api.models.order.Order;
import com.commercetools.ordersfetcher.exception.CommerceToolsException;
import com.commercetools.ordersfetcher.model.DateRangeRequest;
import com.commercetools.ordersfetcher.service.OrderService;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private static final Logger logger = Logger.getLogger(OrderController.class.getName());
    private final OrderService orderService;
    
    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    /**
     * Fetches all orders in the given date range, handling the Commerce Tools
     * API 10,000 record limit.
     *
     * @param dateRangeRequest The date range to fetch orders for
     * @return A complete list of all orders in the date range
     */
    @PostMapping("/fetch")
    public ResponseEntity<Map<String, Object>> fetchOrders(@Valid @RequestBody DateRangeRequest dateRangeRequest) {
        logger.info("Received request to fetch orders from " + dateRangeRequest.getStartDate() + 
                    " to " + dateRangeRequest.getEndDate());
        
        try {
            long startTime = System.currentTimeMillis();
            
            List<Order> orders = orderService.fetchAllOrdersInDateRange(dateRangeRequest);
            
            long duration = System.currentTimeMillis() - startTime;
            
            Map<String, Object> response = new HashMap<>();
            response.put("orders", orders);
            response.put("count", orders.size());
            response.put("startDate", dateRangeRequest.getStartDate());
            response.put("endDate", dateRangeRequest.getEndDate());
            response.put("durationMs", duration);
            
            logger.info("Successfully fetched " + orders.size() + " orders in " + duration + " ms");
            
            return ResponseEntity.ok(response);
        } catch (CommerceToolsException e) {
            logger.severe("Error fetching orders: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Asynchronously fetches all orders in the given date range.
     * This endpoint returns immediately and processes the request in the background.
     *
     * @param dateRangeRequest The date range to fetch orders for
     * @return A response containing a message that the request is being processed
     */
    @PostMapping("/fetch-async")
    public ResponseEntity<Map<String, Object>> fetchOrdersAsync(@Valid @RequestBody DateRangeRequest dateRangeRequest) {
        logger.info("Received async request to fetch orders from " + dateRangeRequest.getStartDate() + 
                    " to " + dateRangeRequest.getEndDate());
        
        CompletableFuture<List<Order>> future = orderService.fetchAllOrdersInDateRangeAsync(dateRangeRequest);
        
        // Add a callback to log when the async operation completes
        future.thenAccept(orders -> 
            logger.info("Async operation completed. Fetched " + orders.size() + " orders from " + 
                        dateRangeRequest.getStartDate() + " to " + dateRangeRequest.getEndDate())
        );
        
        return ResponseEntity.accepted()
                .body(Map.of(
                        "message", "Your request is being processed asynchronously",
                        "startDate", dateRangeRequest.getStartDate(),
                        "endDate", dateRangeRequest.getEndDate()
                ));
    }

    /**
     * Handles exceptions thrown by the validation framework.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> handleException(Exception e) {
        logger.severe("Unhandled exception: " + e.getMessage());
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", e.getMessage()));
    }
}