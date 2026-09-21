package com.example.ratelimit.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.concurrent.TimeUnit;

/**
 * Annotation to apply rate limiting to a method or class.
 * 
 * Usage:
 * <pre>
 * {@code
 * @RateLimit(limit = 10, duration = 1, timeUnit = TimeUnit.MINUTES)
 * public ResponseEntity<User> getUser(@PathVariable Long id) {
 *     // ...
 * }
 * 
 * @RateLimit(limit = 5, duration = 1, timeUnit = TimeUnit.HOURS, key = "#userId")
 * public ResponseEntity<Order> createOrder(@RequestParam Long userId, @RequestBody Order order) {
 *     // ...
 * }
 * }
 * </pre>
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface RateLimit {

    /**
     * Maximum number of requests allowed within the specified duration.
     */
    int limit() default 100;

    /**
     * Duration value for the rate limit window.
     */
    long duration() default 1;

    /**
     * Time unit for the duration.
     */
    TimeUnit timeUnit() default TimeUnit.MINUTES;

    /**
     * SpEL expression to determine the rate limit key.
     * Default is based on client IP address.
     * 
     * Examples:
     * - "#userId" - Use method parameter named userId
     * - "#request.getHeader('X-API-Key')" - Use API key from header
     * - "T(java.util.UUID).randomUUID().toString()" - Random key (not useful, just example)
     */
    String key() default "";

    /**
     * Custom message when rate limit is exceeded.
     */
    String message() default "Rate limit exceeded";

    /**
     * Whether to skip rate limiting for this endpoint (useful at class level).
     */
    boolean skip() default false;
}
