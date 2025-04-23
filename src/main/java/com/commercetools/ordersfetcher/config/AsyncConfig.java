package com.commercetools.ordersfetcher.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.logging.Logger;

/**
 * Configuration for asynchronous processing capabilities.
 */
@Configuration
@EnableAsync
public class AsyncConfig {

    private static final Logger logger = Logger.getLogger(AsyncConfig.class.getName());
    
    /**
     * Creates a thread pool task executor for parallel processing.
     * @return The configured executor
     */
    @Bean(name = "orderProcessingExecutor")
    public Executor orderProcessingExecutor() {
        logger.info("Configuring order processing executor");
        
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(4);
        executor.setMaxPoolSize(10);
        executor.setQueueCapacity(25);
        executor.setThreadNamePrefix("OrderProcessor-");
        executor.initialize();
        
        logger.info("Order processing executor configured with core pool size: 4, max pool size: 10");
        return executor;
    }
}