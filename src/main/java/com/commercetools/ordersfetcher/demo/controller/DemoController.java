package com.commercetools.ordersfetcher.demo.controller;

import com.commercetools.ordersfetcher.demo.model.SimpleOrder;
import com.commercetools.ordersfetcher.demo.service.SimpleOrderService;
import com.commercetools.ordersfetcher.model.DateRangeRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Controller for demonstrating the date range segmentation and parallel processing.
 */
@RestController
@RequestMapping("/api/demo")
public class DemoController {
    
    private static final Logger logger = Logger.getLogger(DemoController.class.getName());
    
    @Autowired
    private SimpleOrderService orderService;
    
    /**
     * Demonstrates the date range segmentation strategy.
     * 
     * @param startDate The start date
     * @param endDate The end date
     * @return Segmentation details and performance comparison
     */
    @GetMapping("/segmentation")
    public ResponseEntity<Map<String, Object>> demonstrateSegmentation(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        
        logger.info("Demonstrating segmentation for range: " + startDate + " to " + endDate);
        
        DateRangeRequest dateRange = new DateRangeRequest(startDate, endDate);
        Map<String, Object> result = orderService.demonstrateSegmentation(dateRange);
        
        return ResponseEntity.ok(result);
    }
    
    /**
     * Fetches orders using parallel processing.
     * 
     * @param startDate The start date
     * @param endDate The end date
     * @return Information about the orders and processing
     */
    @GetMapping("/parallel-fetch")
    public ResponseEntity<Map<String, Object>> fetchOrdersParallel(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        
        logger.info("Fetching orders in parallel for range: " + startDate + " to " + endDate);
        
        long startTime = System.currentTimeMillis();
        
        DateRangeRequest dateRange = new DateRangeRequest(startDate, endDate);
        List<SimpleOrder> orders = orderService.fetchAllOrdersInDateRangeParallel(dateRange);
        
        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;
        
        Map<String, Object> result = new HashMap<>();
        result.put("dateRange", Map.of(
                "startDate", startDate.toString(),
                "endDate", endDate.toString()
        ));
        result.put("orderCount", orders.size());
        result.put("processingTimeMs", duration);
        
        // Include a subset of orders for demonstration
        int sampleSize = Math.min(10, orders.size());
        result.put("sampleOrders", orders.subList(0, sampleSize));
        
        logger.info("Completed parallel fetch in " + duration + "ms, found " + orders.size() + " orders");
        
        return ResponseEntity.ok(result);
    }
    
    /**
     * Gets basic information about the demo.
     * 
     * @return Basic information
     */
    @GetMapping("/info")
    public ResponseEntity<Map<String, Object>> getDemoInfo() {
        Map<String, Object> info = new HashMap<>();
        info.put("name", "Commerce Tools Orders Fetcher Demo");
        info.put("description", "Demonstrates how to overcome the 10,000 record limitation in Commerce Tools API");
        info.put("features", List.of(
                "Date range segmentation",
                "Parallel processing",
                "Performance comparison"
        ));
        info.put("endpoints", Map.of(
                "/api/demo/info", "Get basic information about the demo",
                "/api/demo/segmentation", "Demonstrate date range segmentation (params: startDate, endDate)",
                "/api/demo/parallel-fetch", "Fetch orders in parallel (params: startDate, endDate)"
        ));
        
        return ResponseEntity.ok(info);
    }
}