package com.example.ratelimit.core;

import com.example.ratelimit.RateLimitProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * In-memory implementation of RateLimiter.
 * Suitable for single-instance applications.
 * 
 * For distributed applications, use Redis-based implementation.
 */
public class InMemoryRateLimiter implements RateLimiter {

    private static final Logger logger = LoggerFactory.getLogger(InMemoryRateLimiter.class);

    private final RateLimitProperties properties;
    private final ConcurrentMap<String, RateLimitBucket> buckets = new ConcurrentHashMap<>();

    public InMemoryRateLimiter(RateLimitProperties properties) {
        this.properties = properties;
    }

    @Override
    public RateLimitInfo tryAcquire(String key, int limit, Duration duration) {
        String fullKey = properties.getKeyPrefix() + ":" + key;
        long now = System.currentTimeMillis();
        long windowDuration = duration.toMillis();

        RateLimitBucket bucket = buckets.compute(fullKey, (k, existingBucket) -> {
            if (existingBucket == null || existingBucket.isExpired(now)) {
                return new RateLimitBucket(now, windowDuration, limit);
            }
            return existingBucket;
        });

        return bucket.tryAcquire(fullKey, limit, now);
    }

    @Override
    public RateLimitInfo getStatus(String key, int limit, Duration duration) {
        String fullKey = properties.getKeyPrefix() + ":" + key;
        long now = System.currentTimeMillis();

        RateLimitBucket bucket = buckets.get(fullKey);
        if (bucket == null || bucket.isExpired(now)) {
            return new RateLimitInfo(fullKey, limit, limit, now + duration.toMillis(), true);
        }

        return bucket.getStatus(fullKey, limit, now);
    }

    @Override
    public void reset(String key) {
        String fullKey = properties.getKeyPrefix() + ":" + key;
        buckets.remove(fullKey);
        logger.debug("Reset rate limit for key: {}", fullKey);
    }

    /**
     * Cleans up expired buckets to prevent memory leaks.
     */
    public void cleanup() {
        long now = System.currentTimeMillis();
        buckets.entrySet().removeIf(entry -> entry.getValue().isExpired(now));
        logger.debug("Cleaned up expired rate limit buckets. Remaining: {}", buckets.size());
    }

    /**
     * Internal class representing a rate limit bucket using sliding window algorithm.
     */
    private static class RateLimitBucket {
        private final AtomicLong windowStart;
        private final long windowDuration;
        private final AtomicInteger requestCount;
        private final int maxRequests;

        RateLimitBucket(long startTime, long windowDuration, int maxRequests) {
            this.windowStart = new AtomicLong(startTime);
            this.windowDuration = windowDuration;
            this.requestCount = new AtomicInteger(0);
            this.maxRequests = maxRequests;
        }

        boolean isExpired(long now) {
            return now - windowStart.get() >= windowDuration;
        }

        synchronized RateLimitInfo tryAcquire(String key, int limit, long now) {
            // Slide the window if needed
            if (isExpired(now)) {
                windowStart.set(now);
                requestCount.set(0);
            }

            int currentCount = requestCount.get();
            long resetTime = windowStart.get() + windowDuration;

            if (currentCount < limit) {
                requestCount.incrementAndGet();
                int remaining = limit - currentCount - 1;
                return new RateLimitInfo(key, limit, remaining, resetTime, true);
            } else {
                return new RateLimitInfo(key, limit, 0, resetTime, false);
            }
        }

        RateLimitInfo getStatus(String key, int limit, long now) {
            int currentCount = requestCount.get();
            long resetTime = windowStart.get() + windowDuration;
            int remaining = Math.max(0, limit - currentCount);
            return new RateLimitInfo(key, limit, remaining, resetTime, remaining > 0);
        }
    }
}
