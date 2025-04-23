package com.commercetools.ordersfetcher.exception;

/**
 * Exception thrown when there is an error interacting with the Commerce Tools API.
 */
public class CommerceToolsException extends RuntimeException {

    public CommerceToolsException(String message) {
        super(message);
    }

    public CommerceToolsException(String message, Throwable cause) {
        super(message, cause);
    }
}