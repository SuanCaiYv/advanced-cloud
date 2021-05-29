package com.learn.gateway;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.reactor.circuitbreaker.operator.CircuitBreakerOperator;
import reactor.core.publisher.Mono;

/**
 * @author 十三月之夜
 * @time 2021/5/28 11:53 下午
 */
public class TestMain {

    public static void main(String[] args) {
        for (int i = 0; i < 20; ++ i) {
            ;
        }
    }

    public static Mono<String> f() {
        return Mono.error(new Throwable("aaa"));
    }

    public static Mono<String> ff() {
        CircuitBreakerConfig circuitBreakerConfig = CircuitBreakerConfig.from(CircuitBreakerConfig.ofDefaults())
                .slidingWindowType(CircuitBreakerConfig.SlidingWindowType.COUNT_BASED)
                .slidingWindowSize(10)
                .failureRateThreshold(50)
                .build();
        CircuitBreaker circuitBreaker = CircuitBreaker.of("custom", circuitBreakerConfig);
        return f().transformDeferred(CircuitBreakerOperator.of(circuitBreaker))
                .onErrorResume(p1 -> {
                    return Mono.just("get err");
                });
    }
}
