package com.commercetools.ordersfetcher;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.retry.annotation.EnableRetry;

@SpringBootApplication
@EnableRetry
@EnableAsync
public class OrdersFetcherApplication {

    public static void main(String[] args) {
        SpringApplication.run(OrdersFetcherApplication.class, args);
    }
}
