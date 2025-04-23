package com.commercetools.ordersfetcher.mock;

import com.commercetools.api.client.ProjectApiRoot;
import com.commercetools.api.client.ByProjectKeyOrdersRequestBuilder;

import java.util.logging.Logger;

/**
 * Mock implementation of ProjectApiRoot for development without actual Commerce Tools credentials.
 * This is only used in development and testing environments.
 */
public class MockProjectApiRoot implements ProjectApiRoot {

    private static final Logger logger = Logger.getLogger(MockProjectApiRoot.class.getName());
    private final MockOrdersRequestBuilder ordersRequestBuilder;

    public MockProjectApiRoot() {
        this.ordersRequestBuilder = new MockOrdersRequestBuilder();
        logger.info("Initialized MockProjectApiRoot");
    }

    @Override
    public ByProjectKeyOrdersRequestBuilder orders() {
        return ordersRequestBuilder;
    }

    // Implement other methods as needed with empty or mock implementations
    
    @Override
    public void close() {
        // No resources to close
        logger.info("Closing MockProjectApiRoot");
    }
}