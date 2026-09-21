package com.example.ratelimit.aspect;

import com.example.ratelimit.RateLimitProperties;
import com.example.ratelimit.annotation.RateLimit;
import com.example.ratelimit.core.RateLimitInfo;
import com.example.ratelimit.core.RateLimitKeyResolver;
import com.example.ratelimit.core.RateLimiter;
import com.example.ratelimit.exception.RateLimitExceededException;
import jakarta.servlet.http.HttpServletResponse;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.core.ParameterNameDiscoverer;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.reflect.Method;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

/**
 * Aspect that intercepts methods annotated with @RateLimit.
 */
@Aspect
public class RateLimitAspect {

    private static final Logger logger = LoggerFactory.getLogger(RateLimitAspect.class);

    private final RateLimiter rateLimiter;
    private final RateLimitProperties properties;
    private final RateLimitKeyResolver keyResolver;
    private final SpelExpressionParser spelParser = new SpelExpressionParser();
    private final ParameterNameDiscoverer parameterNameDiscoverer = new DefaultParameterNameDiscoverer();

    public RateLimitAspect(RateLimiter rateLimiter, RateLimitProperties properties, 
                          RateLimitKeyResolver keyResolver) {
        this.rateLimiter = rateLimiter;
        this.properties = properties;
        this.keyResolver = keyResolver;
    }

    @Around("@annotation(rateLimit)")
    public Object handleRateLimit(ProceedingJoinPoint joinPoint, RateLimit rateLimit) throws Throwable {
        // Check if rate limiting is enabled globally
        if (!properties.isEnabled()) {
            return joinPoint.proceed();
        }

        // Check if this specific annotation should be skipped
        if (rateLimit.skip()) {
            return joinPoint.proceed();
        }

        // Resolve the rate limit key
        String key = resolveKey(joinPoint, rateLimit);

        // Calculate duration
        Duration duration = Duration.of(rateLimit.duration(), toChronoUnit(rateLimit.timeUnit()));

        // Try to acquire
        RateLimitInfo info = rateLimiter.tryAcquire(key, rateLimit.limit(), duration);

        // Add headers to response
        addRateLimitHeaders(info);

        if (info.isAllowed()) {
            logger.debug("Rate limit allowed for key: {}, remaining: {}", key, info.getRemaining());
            return joinPoint.proceed();
        } else {
            logger.warn("Rate limit exceeded for key: {}", key);
            throw new RateLimitExceededException(
                rateLimit.message(),
                key,
                rateLimit.limit(),
                info.getRetryAfterSeconds()
            );
        }
    }

    /**
     * Resolves the rate limit key from the annotation or default resolver.
     */
    private String resolveKey(ProceedingJoinPoint joinPoint, RateLimit rateLimit) {
        String keyExpression = rateLimit.key();

        if (keyExpression == null || keyExpression.isEmpty()) {
            // Use default key resolver (IP-based)
            MethodSignature signature = (MethodSignature) joinPoint.getSignature();
            String methodName = signature.getDeclaringType().getSimpleName() + "." + signature.getName();
            return keyResolver.resolveKey(methodName);
        }

        // Parse SpEL expression
        try {
            MethodSignature signature = (MethodSignature) joinPoint.getSignature();
            Method method = signature.getMethod();
            Object[] args = joinPoint.getArgs();

            EvaluationContext context = createEvaluationContext(method, args);
            Expression expression = spelParser.parseExpression(keyExpression);
            Object keyValue = expression.getValue(context);

            if (keyValue == null) {
                return keyResolver.resolveKey();
            }

            String methodName = signature.getDeclaringType().getSimpleName() + "." + signature.getName();
            return keyResolver.resolveKey() + ":" + methodName + ":" + keyValue.toString();
        } catch (Exception e) {
            logger.warn("Failed to evaluate SpEL expression: {}. Using default key.", keyExpression, e);
            return keyResolver.resolveKey();
        }
    }

    /**
     * Creates SpEL evaluation context with method parameters.
     */
    private EvaluationContext createEvaluationContext(Method method, Object[] args) {
        StandardEvaluationContext context = new StandardEvaluationContext();

        String[] paramNames = parameterNameDiscoverer.getParameterNames(method);
        if (paramNames != null) {
            for (int i = 0; i < paramNames.length; i++) {
                context.setVariable(paramNames[i], args[i]);
            }
        }

        // Also add request if available
        ServletRequestAttributes attributes = 
            (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes != null) {
            context.setVariable("request", attributes.getRequest());
        }

        return context;
    }

    /**
     * Adds rate limit information to response headers.
     */
    private void addRateLimitHeaders(RateLimitInfo info) {
        if (!properties.isIncludeHeaders()) {
            return;
        }

        ServletRequestAttributes attributes = 
            (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            return;
        }

        HttpServletResponse response = attributes.getResponse();
        if (response == null) {
            return;
        }

        response.setHeader("X-RateLimit-Limit", String.valueOf(info.getLimit()));
        response.setHeader("X-RateLimit-Remaining", String.valueOf(info.getRemaining()));
        response.setHeader("X-RateLimit-Reset", String.valueOf(info.getResetTime() / 1000));

        if (!info.isAllowed()) {
            response.setHeader("Retry-After", String.valueOf(info.getRetryAfterSeconds()));
        }
    }

    /**
     * Converts TimeUnit to ChronoUnit.
     */
    private java.time.temporal.ChronoUnit toChronoUnit(TimeUnit timeUnit) {
        return switch (timeUnit) {
            case NANOSECONDS -> java.time.temporal.ChronoUnit.NANOS;
            case MICROSECONDS -> java.time.temporal.ChronoUnit.MICROS;
            case MILLISECONDS -> java.time.temporal.ChronoUnit.MILLIS;
            case SECONDS -> java.time.temporal.ChronoUnit.SECONDS;
            case MINUTES -> java.time.temporal.ChronoUnit.MINUTES;
            case HOURS -> java.time.temporal.ChronoUnit.HOURS;
            case DAYS -> java.time.temporal.ChronoUnit.DAYS;
        };
    }
}
