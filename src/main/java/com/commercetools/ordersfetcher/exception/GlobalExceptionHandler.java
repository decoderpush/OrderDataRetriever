package com.commercetools.ordersfetcher.exception;

import io.vrap.rmf.base.client.error.BadGatewayException;
import io.vrap.rmf.base.client.error.BadRequestException;
import io.vrap.rmf.base.client.error.ForbiddenException;
import io.vrap.rmf.base.client.error.NotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;
import java.util.logging.Level;

@ControllerAdvice
public class GlobalExceptionHandler {
    
    private static final Logger logger = Logger.getLogger(GlobalExceptionHandler.class.getName());

    @ExceptionHandler(CommerceToolsException.class)
    public ResponseEntity<Object> handleCommerceToolsException(CommerceToolsException ex, WebRequest request) {
        logger.severe("Commerce Tools API error: " + ex.getMessage());
        return buildErrorResponse(ex, "Commerce Tools API Error", HttpStatus.INTERNAL_SERVER_ERROR, request);
    }

    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<Object> handleBadRequestException(BadRequestException ex, WebRequest request) {
        logger.severe("Bad request error: " + ex.getMessage());
        return buildErrorResponse(ex, "Invalid Request", HttpStatus.BAD_REQUEST, request);
    }

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<Object> handleNotFoundException(NotFoundException ex, WebRequest request) {
        logger.severe("Resource not found: " + ex.getMessage());
        return buildErrorResponse(ex, "Resource Not Found", HttpStatus.NOT_FOUND, request);
    }

    @ExceptionHandler(ForbiddenException.class)
    public ResponseEntity<Object> handleForbiddenException(ForbiddenException ex, WebRequest request) {
        logger.severe("Access forbidden: " + ex.getMessage());
        return buildErrorResponse(ex, "Access Forbidden", HttpStatus.FORBIDDEN, request);
    }

    @ExceptionHandler(BadGatewayException.class)
    public ResponseEntity<Object> handleBadGatewayException(BadGatewayException ex, WebRequest request) {
        logger.severe("Bad gateway error: " + ex.getMessage());
        return buildErrorResponse(ex, "API Gateway Error", HttpStatus.BAD_GATEWAY, request);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Object> handleGenericException(Exception ex, WebRequest request) {
        logger.severe("Unexpected error: " + ex.getMessage());
        return buildErrorResponse(ex, "Unexpected Error", HttpStatus.INTERNAL_SERVER_ERROR, request);
    }

    private ResponseEntity<Object> buildErrorResponse(Exception ex, String title, HttpStatus status, WebRequest request) {
        Map<String, Object> body = new HashMap<>();
        body.put("timestamp", LocalDateTime.now());
        body.put("status", status.value());
        body.put("title", title);
        body.put("detail", ex.getMessage());
        body.put("path", request.getDescription(false).replace("uri=", ""));
        
        return new ResponseEntity<>(body, status);
    }
}
