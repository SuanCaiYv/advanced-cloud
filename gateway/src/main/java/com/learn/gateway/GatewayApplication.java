package com.learn.gateway;

import com.learn.gateway.filter.NotFoundFilter;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
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
                // 确保CB维持在打开状态的时间，之后会自动切换到半开，如果开启了自动切换的话
                .waitDurationInOpenState(Duration.ofMillis(20))
                .automaticTransitionFromOpenToHalfOpenEnabled(true)
                // 确保CB维持在半开状态的时间，在它切换到打开状态之前。时间越长，越有可能因为新的请求可用而在切换到打开状态之前重新回到关闭状态。
                // 但是时间越长，越有可能拖垮上流服务
                .maxWaitDurationInHalfOpenState(Duration.ofMillis(40))
                // 失败率阀值
                .failureRateThreshold(50)
                // 滑动窗口大小，只有请求总数到达这个大小才会进行失败率计算。
                // 此外，对于CLOSED状态的CB，只有请求至少达到这个值才会计算，此前不会限制请求数。
                // 达到这个大小会开始计算，同时继续接受请求。
                // 这个属性仅仅说明想要触发失败率计算的请求数的最小大小。
                .slidingWindowSize(2)
                // 但是半开状态的这个属性则会限制请求总数，一旦到达这个数量就会不允许新的请求加入。
                // 达到这个大小会开始计算失败率并决定状态切换，但是不会接受新的请求。
                .permittedNumberOfCallsInHalfOpenState(1024)
                // 滑动窗口类型
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
