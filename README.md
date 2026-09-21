# custom-spring-boot
Implemented a complete custom Spring Boot starter for rate limiting. Here's a summary: \
Custom Spring Boot Starter Structure

custom-starter/ \
├── ratelimit-spring-boot-starter/     # The starter library \
│   ├── pom.xml \
│   └── src/main/java/com/example/ratelimit/ \
│       ├── RateLimitProperties.java           # @ConfigurationProperties \
│       ├── RateLimitAutoConfiguration.java    # Auto-configuration \
│       ├── annotation/RateLimit.java          # @RateLimit annotation \
│       ├── aspect/RateLimitAspect.java        # AOP interceptor \
│       ├── core/ \
│       │   ├── RateLimiter.java               # Interface \
│       │   ├── InMemoryRateLimiter.java       # Sliding window impl \
│       │   ├── RateLimitInfo.java             # Status container \
│       │   └── RateLimitKeyResolver.java      # IP-based key resolver \
│       ├── exception/RateLimitExceededException.java \
│       └── web/ \
│           ├── RateLimitInterceptor.java      # Property-based limiting \
│           └── RateLimitExceptionHandler.java # @ControllerAdvice \
│
└── ratelimit-example-app/             # Demo application \

Key Spring Boot Starter Components \
Component	Purpose \
@ConfigurationProperties	Binds ratelimit.* properties \
@AutoConfiguration	Creates beans automatically \
@ConditionalOnProperty	Only activates when enabled \
@ConditionalOnMissingBean	Allows user overrides \
META-INF/spring/*.imports	Registers auto-configuration \

To build and use: \
cd custom-starter/ratelimit-spring-boot-starter && mvn clean install \
Add the dependency to your project \
Use @RateLimit annotation or configure via properties
