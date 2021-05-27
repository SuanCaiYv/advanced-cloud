package com.learn.gateway;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class GatewayApplicationTests {

    @Autowired
    private CircuitBreaker circuitBreaker;

    @Test
    void contextLoads() {
        for (int i = 0; i < 10; ++ i) {
            System.out.println(circuitBreaker.getState());
            try {
                circuitBreaker.decorateCheckedSupplier(() -> {
                    throw new NoSuchFieldException("aaa");
                }).apply();
            } catch (Throwable throwable) {
                ;
            }
        }
    }

}
