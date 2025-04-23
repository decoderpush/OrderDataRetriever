package com.commercetools.ordersfetcher.demo.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.logging.Logger;

/**
 * Configuration for asynchronous processing.
 */
@Configuration
@EnableAsync
public class AsyncConfig {
    
    private static final Logger logger = Logger.getLogger(AsyncConfig.class.getName());
    
    /**
     * Creates a thread pool task executor for processing orders in parallel.
     * 
     * @return The configured executor
     */
    @Bean(name = "orderProcessingExecutor")
    public Executor orderProcessingExecutor() {
        logger.info("Configuring order processing executor");
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(4);        // Number of threads to keep alive even when idle
        executor.setMaxPoolSize(10);        // Maximum threads allowed when queue fills
        executor.setQueueCapacity(25);      // Queue size before creating more threads
        executor.setThreadNamePrefix("OrderProcessor-");
        executor.initialize();
        logger.info("Order processing executor configured with corePoolSize=" + executor.getCorePoolSize() + 
                    ", maxPoolSize=" + executor.getMaxPoolSize());
        return executor;
    }
}