package com.commercetools.ordersfetcher.config;

import com.commercetools.api.client.ProjectApiRoot;
import com.commercetools.ordersfetcher.mock.MockProjectApiRoot;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import java.util.logging.Logger;

/**
 * Spring configuration for dependency injection.
 */
@Configuration
public class SpringConfig {

    private static final Logger logger = Logger.getLogger(SpringConfig.class.getName());
    
    /**
     * Provides a mock ProjectApiRoot bean for development and testing.
     * @return A mock ProjectApiRoot implementation
     */
    @Bean
    @Profile({"dev", "test", "default"})
    public ProjectApiRoot mockProjectApiRoot() {
        logger.info("Creating mock ProjectApiRoot");
        return new MockProjectApiRoot();
    }
}