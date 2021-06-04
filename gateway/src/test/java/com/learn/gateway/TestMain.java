package com.learn.gateway;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.reactor.circuitbreaker.operator.CircuitBreakerOperator;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.concurrent.locks.LockSupport;

/**
 * @author 十三月之夜
 * @time 2021/5/28 11:53 下午
 */
public class TestMain {

    public static void main(String[] args) {
        // Mono/Flux就是异步+回调=流水线，然后多线程跑流水线
        new Thread(() -> {
            Mono.create(monoSink -> {
                monoSink.success(f());
            }).subscribe(System.out::println);
        }).start();
        System.out.println("run");
    }

    public static String f() {
        LockSupport.parkNanos(Duration.ofMillis(2000).toNanos());
        return "aaa";
    }
}
