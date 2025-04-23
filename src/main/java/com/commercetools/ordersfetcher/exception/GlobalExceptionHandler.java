package com.commercetools.ordersfetcher.exception;

import io.vrap.rmf.base.client.error.BadGatewayException;
import io.vrap.rmf.base.client.error.BadRequestException;
import io.vrap.rmf.base.client.error.ForbiddenException;
import io.vrap.rmf.base.client.error.NotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@ControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(CommerceToolsException.class)
    public ResponseEntity<Object> handleCommerceToolsException(CommerceToolsException ex, WebRequest request) {
        log.error("Commerce Tools API error: {}", ex.getMessage(), ex);
        return buildErrorResponse(ex, "Commerce Tools API Error", HttpStatus.INTERNAL_SERVER_ERROR, request);
    }

    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<Object> handleBadRequestException(BadRequestException ex, WebRequest request) {
        log.error("Bad request error: {}", ex.getMessage(), ex);
        return buildErrorResponse(ex, "Invalid Request", HttpStatus.BAD_REQUEST, request);
    }

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<Object> handleNotFoundException(NotFoundException ex, WebRequest request) {
        log.error("Resource not found: {}", ex.getMessage(), ex);
        return buildErrorResponse(ex, "Resource Not Found", HttpStatus.NOT_FOUND, request);
    }

    @ExceptionHandler(ForbiddenException.class)
    public ResponseEntity<Object> handleForbiddenException(ForbiddenException ex, WebRequest request) {
        log.error("Access forbidden: {}", ex.getMessage(), ex);
        return buildErrorResponse(ex, "Access Forbidden", HttpStatus.FORBIDDEN, request);
    }

    @ExceptionHandler(BadGatewayException.class)
    public ResponseEntity<Object> handleBadGatewayException(BadGatewayException ex, WebRequest request) {
        log.error("Bad gateway error: {}", ex.getMessage(), ex);
        return buildErrorResponse(ex, "API Gateway Error", HttpStatus.BAD_GATEWAY, request);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Object> handleGenericException(Exception ex, WebRequest request) {
        log.error("Unexpected error: {}", ex.getMessage(), ex);
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
