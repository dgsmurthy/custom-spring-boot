package com.example.ratelimit.core;

/**
 * Contains information about the current rate limit status.
 */
public class RateLimitInfo {

    private final String key;
    private final int limit;
    private final int remaining;
    private final long resetTime;
    private final boolean allowed;

    public RateLimitInfo(String key, int limit, int remaining, long resetTime, boolean allowed) {
        this.key = key;
        this.limit = limit;
        this.remaining = remaining;
        this.resetTime = resetTime;
        this.allowed = allowed;
    }

    public String getKey() {
        return key;
    }

    public int getLimit() {
        return limit;
    }

    public int getRemaining() {
        return remaining;
    }

    public long getResetTime() {
        return resetTime;
    }

    public boolean isAllowed() {
        return allowed;
    }

    public long getRetryAfterSeconds() {
        if (allowed) {
            return 0;
        }
        return Math.max(0, (resetTime - System.currentTimeMillis()) / 1000);
    }

    @Override
    public String toString() {
        return "RateLimitInfo{" +
                "key='" + key + '\'' +
                ", limit=" + limit +
                ", remaining=" + remaining +
                ", resetTime=" + resetTime +
                ", allowed=" + allowed +
                '}';
    }
}
