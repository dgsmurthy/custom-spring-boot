package com.example.ratelimit.exception;

/**
 * Exception thrown when rate limit is exceeded.
 */
public class RateLimitExceededException extends RuntimeException {

    private final String key;
    private final int limit;
    private final long retryAfterSeconds;

    public RateLimitExceededException(String message) {
        super(message);
        this.key = null;
        this.limit = 0;
        this.retryAfterSeconds = 0;
    }

    public RateLimitExceededException(String message, String key, int limit, long retryAfterSeconds) {
        super(message);
        this.key = key;
        this.limit = limit;
        this.retryAfterSeconds = retryAfterSeconds;
    }

    public String getKey() {
        return key;
    }

    public int getLimit() {
        return limit;
    }

    public long getRetryAfterSeconds() {
        return retryAfterSeconds;
    }
}
