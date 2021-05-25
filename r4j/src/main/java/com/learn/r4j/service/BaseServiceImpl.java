package com.learn.r4j.service;

import com.learn.r4j.exception.BusinessException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.timelimiter.annotation.TimeLimiter;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.locks.LockSupport;

/**
 * @author 十三月之夜
 * @time 2021/5/23 11:46 下午
 */
@Service
public class BaseServiceImpl implements BaseService {

    @Override
    @CircuitBreaker(name = "custom", fallbackMethod = "ignoreException")
    public String ignoreException() {
        throw new BusinessException("business exception");
    }

    public String ignoreException(Throwable throwable) {
        return "has ignored this exception";
    }

    @Override
    @TimeLimiter(name = "custom", fallbackMethod = "ignoreTimeout")
    public CompletableFuture<String> ignoreTimeout() {
        return CompletableFuture.supplyAsync(() -> {
            LockSupport.parkNanos(Duration.ofSeconds(5).toNanos());
            return "ok";
        });
    }

    public CompletableFuture<String> ignoreTimeout(Throwable throwable) {
        return CompletableFuture.supplyAsync(() -> {
            return "has ignored this timeout";
        });
    }
}
