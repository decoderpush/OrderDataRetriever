package com.commercetools.ordersfetcher.controller;

import com.commercetools.api.models.order.Order;
import com.commercetools.ordersfetcher.model.DateRangeRequest;
import com.commercetools.ordersfetcher.model.OrdersResponse;
import com.commercetools.ordersfetcher.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
@Slf4j
public class OrderController {

    private final OrderService orderService;

    @GetMapping
    public ResponseEntity<OrdersResponse> getAllOrders(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        
        log.info("Received request to fetch orders from {} to {}", startDate, endDate);
        
        DateRangeRequest request = new DateRangeRequest(startDate, endDate);
        List<Order> orders = orderService.fetchAllOrdersInDateRange(request);
        
        OrdersResponse response = new OrdersResponse();
        response.setOrders(orders);
        response.setTotalCount(orders.size());
        response.setStartDate(startDate);
        response.setEndDate(endDate);
        
        return ResponseEntity.ok(response);
    }

    @PostMapping("/async")
    public ResponseEntity<String> getAllOrdersAsync(@RequestBody DateRangeRequest request) {
        log.info("Received async request to fetch orders from {} to {}", 
                request.getStartDate(), request.getEndDate());
        
        CompletableFuture<List<Order>> future = orderService.fetchAllOrdersInDateRangeAsync(request);
        
        return ResponseEntity.accepted()
                .body("Order retrieval process started. Results will be processed asynchronously.");
    }
}
