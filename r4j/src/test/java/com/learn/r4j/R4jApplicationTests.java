package com.learn.r4j;

import com.learn.r4j.service.BaseService;
import io.github.resilience4j.timelimiter.TimeLimiter;
import io.vavr.control.Try;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.function.Consumer;
import java.util.function.Supplier;

@SpringBootTest
class R4jApplicationTests {

    @Autowired
    private BaseService baseService;

    @Autowired
    private TimeLimiter custom;

    @Test
    void contextLoads() throws Exception {
        ExecutorService executorService = Executors.newSingleThreadExecutor();
        Callable<String> callable = TimeLimiter.decorateFutureSupplier(custom, new Supplier<Future<String>>() {
            @Override
            public Future<String> get() {
                return executorService.submit(() -> {
                    return "aaa";
                });
            }
        });
    }

}
