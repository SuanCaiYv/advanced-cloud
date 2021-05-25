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
                // 确保CB维持在打开状态的时间，之后会自动切换到半开，如果开启了自动切换的话
                .waitDurationInOpenState(Duration.ofMillis(2000))
                .automaticTransitionFromOpenToHalfOpenEnabled(true)
                // 确保CB维持在半开状态的时间，在它切换到打开状态之前。时间越长，越有可能因为新的请求可用而在切换到打开状态之前重新回到关闭状态。
                // 但是时间越长，越有可能拖垮上流服务
                .maxWaitDurationInHalfOpenState(Duration.ofMillis(4000))
                // 失败率阀值
                .failureRateThreshold(30)
                // 滑动窗口大小，只有请求总数到达这个大小才会进行失败率计算。
                // 此外，对于CLOSED状态的CB，只有请求至少达到这个值才会计算，此前不会限制请求数。
                // 达到这个大小会开始计算，同时继续接受请求。
                // 这个属性仅仅说明想要触发失败率计算的请求数的最小大小。
                .slidingWindowSize(64)
                // 但是半开状态的这个属性则会限制请求总数，一旦到达这个数量就会不允许新的请求加入。
                // 达到这个大小会开始计算失败率并决定状态切换，但是不会接受新的请求。
                .permittedNumberOfCallsInHalfOpenState(1024)
                // 滑动窗口类型
                .slidingWindowType(CircuitBreakerConfig.SlidingWindowType.COUNT_BASED)
                .build();
        CircuitBreaker circuitBreaker = CircuitBreaker.of("custom", circuitBreakerConfig);
        return circuitBreaker;
    }

    @Bean
    public TimeLimiter timeLimiter() {
        TimeLimiterConfig timeLimiterConfig = TimeLimiterConfig.custom()
                // 超时时限
                .timeoutDuration(Duration.ofMillis(800))
                .build();
        return TimeLimiter.of("custom", timeLimiterConfig);
    }

    @Bean
    public RateLimiter rateLimiter() {
        RateLimiterConfig rateLimiterConfig = RateLimiterConfig.custom()
                // 如果某个线程被限流了，这是它必须等待的时间，然后才能再次请求，注意，这锁定的是线程
                .timeoutDuration(Duration.ofMillis(100))
                // 每次请求间隔允许的请求数最大值
                .limitForPeriod(100)
                // 每10ms刷新一次请求间隔
                .limitRefreshPeriod(Duration.ofMillis(10))
                .build();
        return RateLimiter.of("custom", rateLimiterConfig);
    }

    @Bean
    public Bulkhead bulkhead() {
        BulkheadConfig bulkheadConfig = BulkheadConfig.custom()
                // 最大并发调用数
                .maxConcurrentCalls(20)
                // 到达阀值时每个被阻塞的线程等待的最大时间
                .maxWaitDuration(Duration.ofMillis(100))
                .build();
        return Bulkhead.of("custom", bulkheadConfig);
    }

    @Bean
    public Retry retry() {
        RetryConfig retryConfig = RetryConfig.custom()
                // 忽略的异常
                .ignoreExceptions(IgnoredException.class)
                // 需要重试的异常
                .retryExceptions(BusinessException.class)
                .failAfterMaxAttempts(true)
                // 最大重试次数
                .maxAttempts(10)
                // 两次重试之间的间隔
                .waitDuration(Duration.ofMillis(10))
                // 在发生以下异常时开启重试
                .retryOnException(throwable -> {
                    if (throwable instanceof FileNotFoundException) {
                        return true;
                    } else {
                        return false;
                    }
                })
                // 开启重试
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
