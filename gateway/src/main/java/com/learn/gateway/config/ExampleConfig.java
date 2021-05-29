package com.learn.gateway.config;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.reactor.circuitbreaker.operator.CircuitBreakerOperator;
import org.springframework.cloud.gateway.filter.ratelimit.RedisRateLimiter;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.Set;

/**
 * @author 十三月之夜
 * @time 2021/5/24 12:29 下午
 */
@Component
public class ExampleConfig {

    @SuppressWarnings("unchecked")
    @Bean
    public RouteLocator routeLocator(RouteLocatorBuilder routeLocatorBuilder) {
        RouteLocatorBuilder.Builder predicates = routeLocatorBuilder.routes()
                .route("test1", predicateSpec -> {
                    return predicateSpec
                            // 匹配这个路径
                            .path("/s-s-o")
                            // 如果匹配，会把请求重定向到uri里面的这个路径，同时会把path拼接在后面
                            .uri("http://simple-service-one");
                })
                .route("test2", predicateSpec -> {
                    return predicateSpec
                            // 这是一个带有路径参数的匹配项
                            .path("/s-s-o/{param}")
                            .uri("http://simple-service-one");
                })
                .route("test3", predicateSpec -> {
                    return predicateSpec
                            // 匹配一个必须包含Cookie名为cookie-name的Cookie项，且值符合表达式exp定义的匹配
                            .cookie("cookie-name", "exp")
                            .uri("http://simple-service-two");
                })
                .route("test4", predicateSpec -> {
                    return predicateSpec
                            // 匹配一个包含指定请求头的请求
                            .header("token", "exp")
                            .uri("http://simple-service-two");
                })
                .route("test5", predicateSpec -> {
                    return predicateSpec
                            // 匹配一个来自指定host的请求
                            .host("127.0.0.1")
                            .uri("http://simple-service-one");
                })
                .route("test6", predicateSpec -> {
                    return predicateSpec
                            // 匹配指定的方法
                            .method("GET", "POST")
                            .uri("http://simple-service-one");
                })
                .route("test7", predicateSpec -> {
                    return predicateSpec
                            // 匹配请求时间在设定时间之后的请求
                            .after(ZonedDateTime.now())
                            .uri("http://simple-service-one");
                })
                .route("test8", predicateSpec -> {
                    return predicateSpec
                            // 默认访问
                            .alwaysTrue()
                            .filters(gatewayFilterSpec -> {
                                return gatewayFilterSpec
                                        // 向对下流的请求添加一个请求头
                                        .addRequestHeader("Header-Name", "header-value")
                                        // 向对下流的请求添加一个请求参数
                                        .addRequestParameter("username", "123456")
                                        // 向对上流的响应头中添加响应头
                                        .addResponseHeader("token", "654321")
                                        // 处理响应头中的重复响应头，第二个参数是保留策略
                                        .dedupeResponseHeader("Duplicate-Header1, Duplicate-Header2", "RETRAIN_FIRST")
                                        // 设置熔断处理，并在发生问题时转发到哪个新的URI
                                        .circuitBreaker(config -> {
                                            config.setFallbackUri("forward:/simple-service-two")
                                                    .setName("default")
                                                    // 当下流响应状态码包含这些时，也会触发熔断
                                                    .setStatusCodes(Set.of("500", "501", "502"));
                                        })
                                        // 在Gateway进行处理之前，请求的URI会被重写为另一个
                                        .rewritePath("/simple-service-one", "/back-service-one")
                                        // 把名为Time的请求头重命名为LocalTime，并保持数据不变
                                        .mapRequestHeader("Time", "LocalTime")
                                        .fallbackHeaders(config -> {
                                            // 把异常信息添加到Exception这个请求头中去了，这样在fallback中就能获取到异常信息
                                            config.setExecutionExceptionMessageHeaderName("Exception");
                                        })
                                        // 设置限流器
                                        .requestRateLimiter(config -> {
                                            // 设置限流器获取请求主键的方式，这里使用IP:PORT形式作为请求主键
                                            config.setKeyResolver(exchange -> {
                                                return Mono.just(exchange.getRequest().getURI().getHost() + ":" + exchange.getRequest().getURI().getPort());
                                            })
                                                    .setDenyEmptyKey(true)
                                                    .setEmptyKeyStatus("429")
                                                    .setStatusCode(HttpStatus.TOO_MANY_REQUESTS)
                                                    // 使用基于Redis的限流器
                                                    .setRateLimiter(redisRateLimiter());
                                        })
                                        .filter((exchange, chain) -> {
                                            // 自定义熔断器进行处理
                                            return chain.filter(exchange).transform(CircuitBreakerOperator.of(CircuitBreaker.ofDefaults("default")));
                                        })
                                        // 不论发生了什么，直接把这个请求以指定状态码重定向到指定地址
                                        .redirect(302, "https://baidu.com")
                                        // 设置重试策略
                                        .retry(retryConfig -> {
                                            retryConfig.setRetries(10)
                                                    .setExceptions(Exception.class)
                                                    .setMethods(HttpMethod.GET, HttpMethod.PUT, HttpMethod.POST)
                                                    .setSeries(HttpStatus.Series.SERVER_ERROR)
                                                    // 设置退避原则
                                                    .setBackoff(Duration.ofMillis(10), Duration.ofMillis(100), 2, false);
                                        })
                                        // 设置请求大小
                                        .setRequestSize(1024 * 1024 * 10L);
                            })
                            .uri("https://baidu.com");
                });
        return predicates.build();
    }

    @Bean
    public RedisRateLimiter redisRateLimiter() {
        // SpringCloudGateway使用令牌桶算法，相见：https://segmentfault.com/a/1190000015967922
        // 第一个参数表示当桶满时允许的请求数
        // 第二个参数指出桶容量，如果桶容量为20，每秒最大速率为10。那么可以保持两秒的峰值请求
        // 第三个参数是每个请求消耗的令牌数，默认为1
        // 峰值速率 = min(桶容量/每次请求消耗的令牌数，桶满时允许的同时请求数)
        // 第一个参数和第二个相同可以实现稳定速率，第二个参数大于第一个，可以实现临时峰值
        RedisRateLimiter redisRateLimiter = new RedisRateLimiter(100, 100, 1);
        return redisRateLimiter;
    }
}
