package com.commercetools.ordersfetcher.exception;

/**
 * Exception thrown when there's an error interacting with Commerce Tools API.
 */
public class CommerceToolsException extends RuntimeException {

    /**
     * Creates a new Commerce Tools exception with the specified message.
     * 
     * @param message The error message
     */
    public CommerceToolsException(String message) {
        super(message);
    }
    
    /**
     * Creates a new Commerce Tools exception with the specified message and cause.
     * 
     * @param message The error message
     * @param cause The cause of the exception
     */
    public CommerceToolsException(String message, Throwable cause) {
        super(message, cause);
    }
}