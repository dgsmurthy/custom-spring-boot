package com.example.ratelimit.core;

import java.time.Duration;

/**
 * Interface for rate limiting implementations.
 */
public interface RateLimiter {

    /**
     * Attempts to acquire permission for a request.
     *
     * @param key      Unique identifier for the rate limit (e.g., IP address, user ID)
     * @param limit    Maximum number of requests allowed
     * @param duration Time window for the rate limit
     * @return RateLimitInfo containing the result and current status
     */
    RateLimitInfo tryAcquire(String key, int limit, Duration duration);

    /**
     * Gets the current rate limit status without consuming a request.
     *
     * @param key      Unique identifier for the rate limit
     * @param limit    Maximum number of requests allowed
     * @param duration Time window for the rate limit
     * @return RateLimitInfo containing the current status
     */
    RateLimitInfo getStatus(String key, int limit, Duration duration);

    /**
     * Resets the rate limit for a specific key.
     *
     * @param key Unique identifier for the rate limit
     */
    void reset(String key);
}
