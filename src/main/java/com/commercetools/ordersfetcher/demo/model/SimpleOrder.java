package com.commercetools.ordersfetcher.demo.model;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

/**
 * A simplified order model for demonstration purposes.
 */
public class SimpleOrder {
    private String id;
    private ZonedDateTime createdAt;
    private String customerName;
    private double totalAmount;
    private String currency;
    private String orderStatus;

    public SimpleOrder() {
    }

    public SimpleOrder(String id, ZonedDateTime createdAt, String customerName, double totalAmount, String currency, String orderStatus) {
        this.id = id;
        this.createdAt = createdAt;
        this.customerName = customerName;
        this.totalAmount = totalAmount;
        this.currency = currency;
        this.orderStatus = orderStatus;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public ZonedDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(ZonedDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public String getCustomerName() {
        return customerName;
    }

    public void setCustomerName(String customerName) {
        this.customerName = customerName;
    }

    public double getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(double totalAmount) {
        this.totalAmount = totalAmount;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public String getOrderStatus() {
        return orderStatus;
    }

    public void setOrderStatus(String orderStatus) {
        this.orderStatus = orderStatus;
    }
    
    /**
     * Gets the formatted creation date as a string.
     * @return The formatted creation date
     */
    public String getFormattedCreatedAt() {
        return createdAt.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
    }

    @Override
    public String toString() {
        return "SimpleOrder{" +
                "id='" + id + '\'' +
                ", createdAt=" + getFormattedCreatedAt() +
                ", customerName='" + customerName + '\'' +
                ", totalAmount=" + totalAmount +
                ", currency='" + currency + '\'' +
                ", orderStatus='" + orderStatus + '\'' +
                '}';
    }
}