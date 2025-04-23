package com.commercetools.ordersfetcher;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

import java.util.logging.Logger;

/**
 * Main application class for the Commerce Tools Orders Fetcher.
 * 
 * This application demonstrates how to overcome the Commerce Tools API's 
 * 10,000 record limit by implementing date range segmentation and parallel
 * processing for complete order data retrieval.
 */
@SpringBootApplication
@EnableAsync
public class OrdersFetcherApplication {

    private static final Logger logger = Logger.getLogger(OrdersFetcherApplication.class.getName());

    public static void main(String[] args) {
        logger.info("Starting Commerce Tools Orders Fetcher application");
        SpringApplication.run(OrdersFetcherApplication.class, args);
        logger.info("Commerce Tools Orders Fetcher application is running");
    }
}