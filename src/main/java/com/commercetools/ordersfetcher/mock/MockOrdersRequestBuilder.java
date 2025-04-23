package com.commercetools.ordersfetcher.mock;

import com.commercetools.api.client.ByProjectKeyOrdersGet;
import com.commercetools.api.client.ByProjectKeyOrdersRequestBuilder;

import java.util.logging.Logger;

/**
 * Mock implementation of ByProjectKeyOrdersRequestBuilder for development without actual Commerce Tools credentials.
 * This is only used in development and testing environments.
 */
public class MockOrdersRequestBuilder implements ByProjectKeyOrdersRequestBuilder {

    private static final Logger logger = Logger.getLogger(MockOrdersRequestBuilder.class.getName());

    @Override
    public ByProjectKeyOrdersGet get() {
        logger.info("Creating mock ByProjectKeyOrdersGet");
        return new MockOrdersGet();
    }
}