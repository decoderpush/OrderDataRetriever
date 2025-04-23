package com.commercetools.ordersfetcher.mock;

import com.commercetools.api.client.ByProjectKeyOrdersGet;
import com.commercetools.api.models.order.Order;
import com.commercetools.api.models.order.OrderImpl;
import com.commercetools.api.models.order.OrderPagedQueryResponse;
import com.commercetools.api.models.order.OrderPagedQueryResponseBuilder;

import io.vrap.rmf.base.client.ApiHttpResponse;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;

/**
 * Mock implementation of ByProjectKeyOrdersGet for development without actual Commerce Tools credentials.
 * This is only used in development and testing environments.
 */
public class MockOrdersGet implements ByProjectKeyOrdersGet {
    
    private static final Logger logger = Logger.getLogger(MockOrdersGet.class.getName());

    private String where;
    private String sort;
    private Integer offset = 0;
    private Integer limit = 20;

    @Override
    public ByProjectKeyOrdersGet withWhere(String where) {
        this.where = where;
        return this;
    }

    @Override
    public ByProjectKeyOrdersGet withSort(String sort) {
        this.sort = sort;
        return this;
    }

    @Override
    public ByProjectKeyOrdersGet withOffset(Integer offset) {
        this.offset = offset;
        return this;
    }

    @Override
    public ByProjectKeyOrdersGet withLimit(Integer limit) {
        this.limit = limit;
        return this;
    }

    @Override
    public CompletableFuture<ApiHttpResponse<OrderPagedQueryResponse>> execute() {
        logger.info("Executing mock orders query with: where=" + where + ", sort=" + sort + 
                    ", offset=" + offset + ", limit=" + limit);
        
        // Generate mock results
        List<Order> mockOrders = generateMockOrders(limit);
        
        // Build the response
        OrderPagedQueryResponse response = OrderPagedQueryResponseBuilder.of()
                .limit(limit)
                .offset(offset)
                .count(mockOrders.size())
                .total(offset + mockOrders.size() < 10000 ? offset + mockOrders.size() : 10000)
                .results(mockOrders)
                .build();
                
        // Create the API response
        ApiHttpResponse<OrderPagedQueryResponse> apiResponse = 
                new ApiHttpResponse<>(200, null, response);
                
        // Return a completed future with the response
        CompletableFuture<ApiHttpResponse<OrderPagedQueryResponse>> future = new CompletableFuture<>();
        future.complete(apiResponse);
        return future;
    }

    @Override
    public ApiHttpResponse<OrderPagedQueryResponse> executeBlocking() {
        try {
            return execute().get();
        } catch (Exception e) {
            logger.severe("Error executing mock order query: " + e.getMessage());
            throw new RuntimeException("Error executing mock order query", e);
        }
    }
    
    /**
     * Generates mock order data for testing.
     * @param count Number of orders to generate
     * @return List of mock orders
     */
    private List<Order> generateMockOrders(int count) {
        List<Order> mockOrders = new ArrayList<>();
        
        // Get the date range from the where clause if available
        ZonedDateTime startDate = ZonedDateTime.now().minusDays(30);
        ZonedDateTime endDate = ZonedDateTime.now();
        
        if (where != null && where.contains("createdAt")) {
            // Attempt to parse the date range from the where clause
            try {
                // This is a simple parsing logic, in a real implementation you'd use a proper query parser
                String[] parts = where.split("and");
                if (parts.length >= 2) {
                    String startPart = parts[0].trim();
                    String endPart = parts[1].trim();
                    
                    // Extract dates from the query parts
                    if (startPart.contains(">=")) {
                        String dateStr = startPart.split(">=")[1].trim().replace("\"", "");
                        startDate = ZonedDateTime.parse(dateStr, DateTimeFormatter.ISO_OFFSET_DATE_TIME);
                    }
                    
                    if (endPart.contains("<=")) {
                        String dateStr = endPart.split("<=")[1].trim().replace("\"", "");
                        endDate = ZonedDateTime.parse(dateStr, DateTimeFormatter.ISO_OFFSET_DATE_TIME);
                    }
                }
            } catch (Exception e) {
                logger.warning("Failed to parse date range from where clause: " + where + ", error: " + e.getMessage());
            }
        }
        
        // Generate mock orders
        for (int i = 0; i < count; i++) {
            // Create a mock order with a unique ID
            OrderImpl order = new OrderImpl();
            
            // Set a unique ID
            String orderId = UUID.randomUUID().toString();
            order.setId(orderId);
            
            // Set a creation date within the range
            ZonedDateTime createdAt = getRandomDateBetween(startDate, endDate);
            // Convert to string because the OrderImpl expects a string representation
            order.setCreatedAt(createdAt.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));
            
            // Add the order to the list
            mockOrders.add(order);
        }
        
        return mockOrders;
    }
    
    /**
     * Generates a random date between two dates.
     * @param startInclusive Start date (inclusive)
     * @param endInclusive End date (inclusive)
     * @return Random date between start and end
     */
    private ZonedDateTime getRandomDateBetween(ZonedDateTime startInclusive, ZonedDateTime endInclusive) {
        // Get the difference in seconds
        long startSeconds = startInclusive.toEpochSecond();
        long endSeconds = endInclusive.toEpochSecond();
        long randomSeconds = startSeconds + (long) (Math.random() * (endSeconds - startSeconds));
        
        // Return a date at the random number of seconds
        return ZonedDateTime.ofInstant(java.time.Instant.ofEpochSecond(randomSeconds), startInclusive.getZone());
    }
}