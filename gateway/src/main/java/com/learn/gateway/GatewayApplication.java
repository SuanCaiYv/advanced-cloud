package com.learn.gateway;

import com.learn.gateway.filter.NotFoundFilter;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.ratelimiter.RateLimiterConfig;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import io.github.resilience4j.retry.RetryConfig;
import io.github.resilience4j.retry.RetryRegistry;
import io.github.resilience4j.timelimiter.TimeLimiterConfig;
import io.github.resilience4j.timelimiter.TimeLimiterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;

import java.time.Duration;

@SpringBootApplication
public class GatewayApplication {

    private final Logger logger = LoggerFactory.getLogger(GatewayApplication.class);

    @Autowired
    private NotFoundFilter notFoundFilter;

    @Bean
    public CircuitBreakerRegistry customCircuitBreakerRegistry() {
        return CircuitBreakerRegistry.of(customCircuitBreakerConfig());
    }

    @Bean
    public CircuitBreakerConfig customCircuitBreakerConfig() {
        return CircuitBreakerConfig.custom()
                .waitDurationInOpenState(Duration.ofMillis(20))
                .automaticTransitionFromOpenToHalfOpenEnabled(true)
                .maxWaitDurationInHalfOpenState(Duration.ofMillis(40))
                .failureRateThreshold(50)
                .permittedNumberOfCallsInHalfOpenState(1024)
                .slidingWindowType(CircuitBreakerConfig.SlidingWindowType.COUNT_BASED)
                .build();
    }

    @Bean
    public CircuitBreaker customCircuitBreaker() {
        return CircuitBreaker.of("custom", customCircuitBreakerConfig());
    }

    @Bean
    public TimeLimiterRegistry customTimeLimiterRegistry() {
        return TimeLimiterRegistry.of(customTimeLimiterConfig());
    }

    @Bean
    public TimeLimiterConfig customTimeLimiterConfig() {
        return TimeLimiterConfig.custom()
                .timeoutDuration(Duration.ofMillis(500))
                .build();
    }

    @Bean
    public RateLimiterConfig customRateLimiterConfig() {
        return RateLimiterConfig.custom()
                .timeoutDuration(Duration.ofMillis(30000))
                .limitForPeriod(100)
                .limitRefreshPeriod(Duration.ofMillis(10))
                .build();
    }

    @Bean
    public RateLimiterRegistry customRateLimiterRegistry() {
        return RateLimiterRegistry.of(customRateLimiterConfig());
    }

    @Bean
    public RetryConfig customRetryConfig() {
        return RetryConfig.custom()
                .failAfterMaxAttempts(true)
                .maxAttempts(5)
                .waitDuration(Duration.ofMillis(50))
                .retryOnResult(object -> true)
                .build();
    }

    @Bean
    public RetryRegistry customRetryRegistry() {
        return RetryRegistry.of(customRetryConfig());
    }

    @Bean
    public RouteLocator routeLocator(RouteLocatorBuilder routeLocatorBuilder) {
        return routeLocatorBuilder.routes()
                .route("service-one", predicateSpec -> {
                    return predicateSpec
                            .path("/simple-service-one/**")
                            .uri("lb://simple-service-one");
                })
                .route("service-two", predicateSpec -> {
                    return predicateSpec
                            .path("/simple-service-two/**")
                            .uri("lb://simple-service-two");
                })
                .route("service-three", predicateSpec -> {
                    return predicateSpec
                            .path("/simple-service-three/**")
                            .uri("lb://simple-service-three");
                })
                .route("service-four", predicateSpec -> {
                    return predicateSpec
                            .path("/simple-service-four/**")
                            .filters(gatewayFilterSpec -> {
                                return gatewayFilterSpec
                                        .filter(notFoundFilter);
                            })
                            .uri("lb://simple-service-four");
                })
                .route("service-five", predicateSpec -> {
                    return predicateSpec
                            .path("/simple-service-five/**")
                            .uri("lb://simple-service-five");
                })
                .build();
    }

    public static void main(String[] args) {
        SpringApplication.run(GatewayApplication.class, args);
    }

}
