package com.commercetools.ordersfetcher.demo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

import java.util.logging.Logger;

/**
 * Standalone application that demonstrates the date range segmentation and parallel
 * processing capabilities without requiring Commerce Tools API credentials.
 */
@SpringBootApplication
@EnableAsync
public class StandaloneApplication {

    private static final Logger logger = Logger.getLogger(StandaloneApplication.class.getName());

    public static void main(String[] args) {
        logger.info("Starting Standalone Demo Application");
        SpringApplication.run(StandaloneApplication.class, args);
        logger.info("Standalone Demo Application is running");
    }
}