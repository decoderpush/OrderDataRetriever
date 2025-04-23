package com.commercetools.ordersfetcher.model;

import com.commercetools.api.models.order.Order;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
public class OrdersResponse {
    
    private List<Order> orders;
    private int totalCount;
    
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate startDate;
    
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate endDate;
    
    private String message;
}
