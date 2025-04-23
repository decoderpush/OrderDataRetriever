package com.commercetools.ordersfetcher.exception;

public class CommerceToolsException extends RuntimeException {
    
    public CommerceToolsException(String message) {
        super(message);
    }
    
    public CommerceToolsException(String message, Throwable cause) {
        super(message, cause);
    }
}
