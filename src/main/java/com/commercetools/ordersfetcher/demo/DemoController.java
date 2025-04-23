package com.commercetools.ordersfetcher.demo;

import com.commercetools.ordersfetcher.model.DateRangeRequest;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Demo controller to demonstrate date range segmentation and
 * parallel processing for Commerce Tools order fetching.
 */
@RestController
@RequestMapping("/api/demo")
public class DemoController {

    private static final Logger logger = Logger.getLogger(DemoController.class.getName());
    private final DemoOrderService orderService;

    public DemoController(DemoOrderService orderService) {
        this.orderService = orderService;
    }

    /**
     * Demo endpoint to show the date range segmentation and parallel processing logic.
     * This creates a sample date range and demonstrates how it would be segmented.
     */
    @GetMapping("/segment-demo")
    public Map<String, Object> demonstrateSegmentation() {
        logger.info("Demonstrating date range segmentation");
        
        // Create a sample date range (last 6 months)
        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusMonths(6);
        
        DateRangeRequest dateRange = new DateRangeRequest(startDate, endDate);
        logger.info("Created sample date range from " + startDate + " to " + endDate);
        
        // Get segments and processing details
        Map<String, Object> result = orderService.demonstrateSegmentation(dateRange);
        
        logger.info("Returned demonstration results with " + result.get("segmentCount") + " segments");
        return result;
    }
    
    /**
     * Demo endpoint to explain the time-based cursor pagination approach.
     */
    @GetMapping("/pagination-demo")
    public Map<String, Object> demonstratePagination() {
        logger.info("Demonstrating time-based cursor pagination");
        
        Map<String, Object> result = new HashMap<>();
        result.put("title", "Time-based Cursor Pagination");
        result.put("problem", "Commerce Tools API limits results to 10,000 records per query");
        result.put("solution", "Use time-based cursor pagination to fetch all records in segments");
        
        // Add an example of how pagination works
        Map<String, Object> example = new HashMap<>();
        example.put("step1", "Query orders with createdAt >= '2023-01-01' and createdAt <= '2023-01-31'");
        example.put("step2", "Receive up to 10,000 orders, find latest timestamp (e.g., '2023-01-15T12:30:45Z')");
        example.put("step3", "Next query: createdAt >= '2023-01-15T12:30:45.001Z' and createdAt <= '2023-01-31'");
        example.put("step4", "Continue until no more results or fewer than 10,000 records returned");
        result.put("example", example);
        
        logger.info("Returned pagination demonstration");
        return result;
    }
    
    /**
     * Demo endpoint to explain the parallel processing approach.
     */
    @GetMapping("/parallel-demo")
    public Map<String, Object> demonstrateParallelProcessing() {
        logger.info("Demonstrating parallel processing");
        
        Map<String, Object> result = new HashMap<>();
        result.put("title", "Parallel Processing of Date Segments");
        result.put("approach", "Break large date ranges into smaller segments and process them in parallel");
        result.put("benefits", new String[]{
            "Improved throughput",
            "Better utilization of system resources",
            "Faster overall processing time",
            "Configurable thread pool size"
        });
        
        // Add an example
        Map<String, Object> example = new HashMap<>();
        example.put("dateRange", "2023-01-01 to 2023-06-30 (6 months)");
        example.put("segments", new String[]{
            "2023-01-01 to 2023-01-31",
            "2023-02-01 to 2023-02-28",
            "2023-03-01 to 2023-03-31",
            "2023-04-01 to 2023-04-30",
            "2023-05-01 to 2023-05-31",
            "2023-06-01 to 2023-06-30"
        });
        example.put("processing", "Each segment is processed in parallel");
        result.put("example", example);
        
        logger.info("Returned parallel processing demonstration");
        return result;
    }
}