package com.example.ratelimit.web;

import com.example.ratelimit.RateLimitProperties;
import com.example.ratelimit.exception.RateLimitExceededException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Global exception handler for rate limit exceeded errors.
 */
@RestControllerAdvice
public class RateLimitExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(RateLimitExceptionHandler.class);

    private final RateLimitProperties properties;

    public RateLimitExceptionHandler(RateLimitProperties properties) {
        this.properties = properties;
    }

    @ExceptionHandler(RateLimitExceededException.class)
    public ResponseEntity<Map<String, Object>> handleRateLimitExceeded(RateLimitExceededException ex) {
        logger.warn("Rate limit exceeded: {}", ex.getMessage());

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", Instant.now().toString());
        body.put("status", properties.getExceededStatusCode());
        body.put("error", "Too Many Requests");
        body.put("message", properties.getExceededMessage());
        
        if (ex.getRetryAfterSeconds() > 0) {
            body.put("retryAfter", ex.getRetryAfterSeconds());
        }

        return ResponseEntity
                .status(HttpStatus.valueOf(properties.getExceededStatusCode()))
                .header("Retry-After", String.valueOf(ex.getRetryAfterSeconds()))
                .body(body);
    }
}
