package com.example.ratelimit.web;

import com.example.ratelimit.RateLimitProperties;
import com.example.ratelimit.core.RateLimitInfo;
import com.example.ratelimit.core.RateLimitKeyResolver;
import com.example.ratelimit.core.RateLimiter;
import com.example.ratelimit.exception.RateLimitExceededException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * Interceptor for property-based endpoint rate limiting.
 * 
 * This interceptor applies rate limits configured via properties:
 * ratelimit.endpoints./api/users=50
 * ratelimit.endpoints./api/orders=200
 */
public class RateLimitInterceptor implements HandlerInterceptor {

    private static final Logger logger = LoggerFactory.getLogger(RateLimitInterceptor.class);

    private final RateLimiter rateLimiter;
    private final RateLimitProperties properties;
    private final RateLimitKeyResolver keyResolver;

    public RateLimitInterceptor(RateLimiter rateLimiter, 
                                RateLimitProperties properties,
                                RateLimitKeyResolver keyResolver) {
        this.rateLimiter = rateLimiter;
        this.properties = properties;
        this.keyResolver = keyResolver;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, 
                            Object handler) throws Exception {
        if (!properties.isEnabled()) {
            return true;
        }

        String path = request.getRequestURI();
        Integer customLimit = findMatchingLimit(path);

        if (customLimit == null) {
            // No specific limit for this endpoint, use default if configured
            return true;
        }

        String key = keyResolver.resolveKey(path);
        RateLimitInfo info = rateLimiter.tryAcquire(key, customLimit, properties.getDefaultDuration());

        // Add headers
        if (properties.isIncludeHeaders()) {
            response.setHeader("X-RateLimit-Limit", String.valueOf(info.getLimit()));
            response.setHeader("X-RateLimit-Remaining", String.valueOf(info.getRemaining()));
            response.setHeader("X-RateLimit-Reset", String.valueOf(info.getResetTime() / 1000));
        }

        if (info.isAllowed()) {
            logger.debug("Rate limit allowed for path: {}, remaining: {}", path, info.getRemaining());
            return true;
        } else {
            logger.warn("Rate limit exceeded for path: {}", path);
            throw new RateLimitExceededException(
                properties.getExceededMessage(),
                key,
                customLimit,
                info.getRetryAfterSeconds()
            );
        }
    }

    /**
     * Finds the rate limit for the given path.
     * Supports exact match and prefix match.
     */
    private Integer findMatchingLimit(String path) {
        // First try exact match
        Integer limit = properties.getEndpoints().get(path);
        if (limit != null) {
            return limit;
        }

        // Try prefix match
        for (var entry : properties.getEndpoints().entrySet()) {
            String pattern = entry.getKey();
            if (pattern.endsWith("/**")) {
                String prefix = pattern.substring(0, pattern.length() - 3);
                if (path.startsWith(prefix)) {
                    return entry.getValue();
                }
            } else if (pattern.endsWith("/*")) {
                String prefix = pattern.substring(0, pattern.length() - 2);
                if (path.startsWith(prefix) && !path.substring(prefix.length() + 1).contains("/")) {
                    return entry.getValue();
                }
            }
        }

        return null;
    }
}
