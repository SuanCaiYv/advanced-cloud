package com.learn.r4j;

import com.learn.r4j.exception.BusinessException;
import com.learn.r4j.exception.IgnoredException;
import com.learn.r4j.handler.BaseHandler;
import io.github.resilience4j.bulkhead.Bulkhead;
import io.github.resilience4j.bulkhead.BulkheadConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterConfig;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import io.github.resilience4j.timelimiter.TimeLimiter;
import io.github.resilience4j.timelimiter.TimeLimiterConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerResponse;

import java.io.FileNotFoundException;
import java.time.Duration;
import java.util.function.Predicate;

@SpringBootApplication
@EnableAspectJAutoProxy
public class R4jApplication {

    @Autowired
    private BaseHandler baseHandler;

    @Bean
    public RouterFunction<ServerResponse> routerFunction() {
        return RouterFunctions
                .route()
                .GET("/ok", baseHandler::ok)
                .GET("/fail/must", baseHandler::mustFail)
                .GET("/fail/may", baseHandler::mayFail)
                .GET("/timeout/must", baseHandler::mustTimedOut)
                .GET("/timeout/may", baseHandler::mayTimedOut)
                .build();
    }

    @Bean
    public CircuitBreaker customCircuitBreaker() {
        Predicate<Throwable> throwablePredicate = throwable -> {
            if (throwable.getClass().equals(IgnoredException.class)) {
                return true;
            } else {
                return false;
            }
        };
        CircuitBreakerConfig circuitBreakerConfig = CircuitBreakerConfig.custom()
                .ignoreException(throwablePredicate)
                // ??????CB???????????????????????????????????????????????????????????????????????????????????????????????????
                .waitDurationInOpenState(Duration.ofMillis(2000))
                .automaticTransitionFromOpenToHalfOpenEnabled(true)
                // ??????CB????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????
                // ???????????????????????????????????????????????????
                .maxWaitDurationInHalfOpenState(Duration.ofMillis(4000))
                // ???????????????
                .failureRateThreshold(30)
                // ???????????????????????????????????????????????????????????????????????????????????????
                // ???????????????CLOSED?????????CB?????????????????????????????????????????????????????????????????????????????????
                // ???????????????????????????????????????????????????????????????
                // ?????????????????????????????????????????????????????????????????????????????????
                .slidingWindowSize(64)
                // ????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????
                // ???????????????????????????????????????????????????????????????????????????????????????????????????
                .permittedNumberOfCallsInHalfOpenState(1024)
                // ??????????????????
                .slidingWindowType(CircuitBreakerConfig.SlidingWindowType.COUNT_BASED)
                .build();
        CircuitBreaker circuitBreaker = CircuitBreaker.of("custom", circuitBreakerConfig);
        return circuitBreaker;
    }

    @Bean
    public TimeLimiter timeLimiter() {
        TimeLimiterConfig timeLimiterConfig = TimeLimiterConfig.custom()
                // ????????????
                .timeoutDuration(Duration.ofMillis(800))
                .build();
        return TimeLimiter.of("custom", timeLimiterConfig);
    }

    @Bean
    public RateLimiter rateLimiter() {
        RateLimiterConfig rateLimiterConfig = RateLimiterConfig.custom()
                // ????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????
                // ?????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????
                // ????????????IP:PORT??????????????????????????????????????????????????????IP:PORT?????????????????????????????????????????????????????????
                .timeoutDuration(Duration.ofMillis(100))
                // ?????????????????????????????????????????????
                .limitForPeriod(100)
                // ???10ms????????????????????????
                .limitRefreshPeriod(Duration.ofMillis(10))
                .build();
        return RateLimiter.of("custom", rateLimiterConfig);
    }

    @Bean
    public Bulkhead bulkhead() {
        BulkheadConfig bulkheadConfig = BulkheadConfig.custom()
                // ?????????????????????
                .maxConcurrentCalls(20)
                // ????????????????????????????????????????????????????????????
                .maxWaitDuration(Duration.ofMillis(100))
                .build();
        return Bulkhead.of("custom", bulkheadConfig);
    }

    @Bean
    public Retry retry() {
        RetryConfig retryConfig = RetryConfig.custom()
                // ???????????????
                .ignoreExceptions(IgnoredException.class)
                // ?????????????????????
                .retryExceptions(BusinessException.class)
                .failAfterMaxAttempts(true)
                // ??????????????????
                .maxAttempts(10)
                // ???????????????????????????
                .waitDuration(Duration.ofMillis(10))
                // ????????????????????????????????????
                .retryOnException(throwable -> {
                    if (throwable instanceof FileNotFoundException) {
                        return true;
                    } else {
                        return false;
                    }
                })
                // ????????????
                .retryOnResult(obj -> {
                    return true;
                })
                .build();
        return Retry.of("custom", retryConfig);
    }

    public static void main(String[] args) {
        SpringApplication.run(R4jApplication.class, args);
    }

}
