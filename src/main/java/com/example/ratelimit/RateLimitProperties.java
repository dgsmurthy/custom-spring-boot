package com.example.ratelimit;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * Configuration properties for Rate Limiting.
 * 
 * These properties can be configured in application.properties or application.yml:
 * 
 * ratelimit.enabled=true
 * ratelimit.default-limit=100
 * ratelimit.default-duration=1m
 * ratelimit.strategy=SLIDING_WINDOW
 * ratelimit.key-prefix=rate_limit
 * ratelimit.endpoints./api/users=50
 * ratelimit.endpoints./api/orders=200
 */
@ConfigurationProperties(prefix = "ratelimit")
public class RateLimitProperties {

    /**
     * Enable or disable rate limiting globally.
     */
    private boolean enabled = true;

    /**
     * Default number of requests allowed within the duration.
     */
    private int defaultLimit = 100;

    /**
     * Default time window for rate limiting.
     */
    private Duration defaultDuration = Duration.ofMinutes(1);

    /**
     * Rate limiting strategy to use.
     */
    private Strategy strategy = Strategy.SLIDING_WINDOW;

    /**
     * Prefix for rate limit keys (useful for Redis-based implementations).
     */
    private String keyPrefix = "rate_limit";

    /**
     * Custom limits for specific endpoints.
     * Key: endpoint path, Value: max requests per duration
     */
    private Map<String, Integer> endpoints = new HashMap<>();

    /**
     * Headers to include rate limit information in response.
     */
    private boolean includeHeaders = true;

    /**
     * HTTP status code to return when rate limit is exceeded.
     */
    private int exceededStatusCode = 429;

    /**
     * Message to return when rate limit is exceeded.
     */
    private String exceededMessage = "Rate limit exceeded. Please try again later.";

    // Getters and Setters

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public int getDefaultLimit() {
        return defaultLimit;
    }

    public void setDefaultLimit(int defaultLimit) {
        this.defaultLimit = defaultLimit;
    }

    public Duration getDefaultDuration() {
        return defaultDuration;
    }

    public void setDefaultDuration(Duration defaultDuration) {
        this.defaultDuration = defaultDuration;
    }

    public Strategy getStrategy() {
        return strategy;
    }

    public void setStrategy(Strategy strategy) {
        this.strategy = strategy;
    }

    public String getKeyPrefix() {
        return keyPrefix;
    }

    public void setKeyPrefix(String keyPrefix) {
        this.keyPrefix = keyPrefix;
    }

    public Map<String, Integer> getEndpoints() {
        return endpoints;
    }

    public void setEndpoints(Map<String, Integer> endpoints) {
        this.endpoints = endpoints;
    }

    public boolean isIncludeHeaders() {
        return includeHeaders;
    }

    public void setIncludeHeaders(boolean includeHeaders) {
        this.includeHeaders = includeHeaders;
    }

    public int getExceededStatusCode() {
        return exceededStatusCode;
    }

    public void setExceededStatusCode(int exceededStatusCode) {
        this.exceededStatusCode = exceededStatusCode;
    }

    public String getExceededMessage() {
        return exceededMessage;
    }

    public void setExceededMessage(String exceededMessage) {
        this.exceededMessage = exceededMessage;
    }

    /**
     * Rate limiting strategies.
     */
    public enum Strategy {
        /**
         * Fixed window: Resets counter at fixed intervals.
         */
        FIXED_WINDOW,

        /**
         * Sliding window: Smoothly moves the time window.
         */
        SLIDING_WINDOW,

        /**
         * Token bucket: Allows bursts up to bucket size.
         */
        TOKEN_BUCKET
    }
}
