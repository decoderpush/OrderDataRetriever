package com.commercetools.ordersfetcher.demo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.scheduling.annotation.EnableAsync;

import java.util.logging.Logger;

/**
 * Simplified application to demonstrate date range segmentation and parallel processing
 * of order data that overcomes the Commerce Tools API's 10,000 record limitation.
 */
@SpringBootApplication
@EnableAsync
@ComponentScan(basePackages = {
    "com.commercetools.ordersfetcher.demo", 
    "com.commercetools.ordersfetcher.model",
    "com.commercetools.ordersfetcher.util"
})
public class SimpleOrdersApplication {

    private static final Logger logger = Logger.getLogger(SimpleOrdersApplication.class.getName());

    public static void main(String[] args) {
        logger.info("Starting Simple Orders Fetcher Demo Application");
        SpringApplication.run(SimpleOrdersApplication.class, args);
        logger.info("Simple Orders Fetcher Demo Application is running");
    }
}