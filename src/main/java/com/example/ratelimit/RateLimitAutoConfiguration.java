package com.example.ratelimit;

import com.example.ratelimit.aspect.RateLimitAspect;
import com.example.ratelimit.core.InMemoryRateLimiter;
import com.example.ratelimit.core.RateLimitKeyResolver;
import com.example.ratelimit.core.RateLimiter;
import com.example.ratelimit.web.RateLimitExceptionHandler;
import com.example.ratelimit.web.RateLimitInterceptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Auto-configuration for Rate Limiting.
 * 
 * This configuration is automatically applied when:
 * - The application has Spring Web on classpath
 * - The property ratelimit.enabled is not set to false
 */
@AutoConfiguration
@EnableConfigurationProperties(RateLimitProperties.class)
@ConditionalOnProperty(prefix = "ratelimit", name = "enabled", havingValue = "true", matchIfMissing = true)
@EnableScheduling
public class RateLimitAutoConfiguration {

    private static final Logger logger = LoggerFactory.getLogger(RateLimitAutoConfiguration.class);

    public RateLimitAutoConfiguration() {
        logger.info("Rate Limit Auto-Configuration initialized");
    }

    /**
     * Creates the default in-memory rate limiter.
     * Users can provide their own RateLimiter bean (e.g., Redis-based) to override.
     */
    @Bean
    @ConditionalOnMissingBean(RateLimiter.class)
    public RateLimiter rateLimiter(RateLimitProperties properties) {
        logger.info("Creating InMemoryRateLimiter with strategy: {}", properties.getStrategy());
        return new InMemoryRateLimiter(properties);
    }

    /**
     * Creates the key resolver for determining rate limit keys.
     */
    @Bean
    @ConditionalOnMissingBean(RateLimitKeyResolver.class)
    public RateLimitKeyResolver rateLimitKeyResolver() {
        return new RateLimitKeyResolver();
    }

    /**
     * Creates the AOP aspect for @RateLimit annotation support.
     */
    @Bean
    @ConditionalOnMissingBean(RateLimitAspect.class)
    public RateLimitAspect rateLimitAspect(RateLimiter rateLimiter, 
                                            RateLimitProperties properties,
                                            RateLimitKeyResolver keyResolver) {
        logger.info("Creating RateLimitAspect for annotation-based rate limiting");
        return new RateLimitAspect(rateLimiter, properties, keyResolver);
    }

    /**
     * Web-specific configuration for rate limiting.
     */
    @ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
    @ConditionalOnClass(name = "jakarta.servlet.http.HttpServletRequest")
    public static class WebConfiguration implements WebMvcConfigurer {

        private final RateLimiter rateLimiter;
        private final RateLimitProperties properties;
        private final RateLimitKeyResolver keyResolver;

        public WebConfiguration(RateLimiter rateLimiter, 
                               RateLimitProperties properties,
                               RateLimitKeyResolver keyResolver) {
            this.rateLimiter = rateLimiter;
            this.properties = properties;
            this.keyResolver = keyResolver;
        }

        /**
         * Creates the exception handler for rate limit exceeded errors.
         */
        @Bean
        @ConditionalOnMissingBean(RateLimitExceptionHandler.class)
        public RateLimitExceptionHandler rateLimitExceptionHandler() {
            return new RateLimitExceptionHandler(properties);
        }

        /**
         * Creates the interceptor for property-based endpoint rate limiting.
         */
        @Bean
        @ConditionalOnMissingBean(RateLimitInterceptor.class)
        public RateLimitInterceptor rateLimitInterceptor() {
            return new RateLimitInterceptor(rateLimiter, properties, keyResolver);
        }

        @Override
        public void addInterceptors(InterceptorRegistry registry) {
            if (properties.isEnabled() && !properties.getEndpoints().isEmpty()) {
                logger.info("Registering RateLimitInterceptor for {} endpoints", 
                           properties.getEndpoints().size());
                registry.addInterceptor(rateLimitInterceptor())
                        .addPathPatterns("/**");
            }
        }
    }

    /**
     * Scheduled task to clean up expired rate limit buckets.
     */
    @Bean
    public RateLimitCleanupTask rateLimitCleanupTask(RateLimiter rateLimiter) {
        return new RateLimitCleanupTask(rateLimiter);
    }

    /**
     * Cleanup task for in-memory rate limiter.
     */
    public static class RateLimitCleanupTask {

        private final RateLimiter rateLimiter;

        public RateLimitCleanupTask(RateLimiter rateLimiter) {
            this.rateLimiter = rateLimiter;
        }

        @Scheduled(fixedRateString = "${ratelimit.cleanup-interval:60000}")
        public void cleanup() {
            if (rateLimiter instanceof InMemoryRateLimiter) {
                ((InMemoryRateLimiter) rateLimiter).cleanup();
            }
        }
    }
}
