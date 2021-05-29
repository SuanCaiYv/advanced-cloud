package com.learn.gateway.filter;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterConfig;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import io.github.resilience4j.reactor.circuitbreaker.operator.CircuitBreakerOperator;
import io.github.resilience4j.reactor.ratelimiter.operator.RateLimiterOperator;
import io.github.resilience4j.reactor.retry.RetryOperator;
import io.github.resilience4j.reactor.timelimiter.TimeLimiterOperator;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import io.github.resilience4j.retry.RetryRegistry;
import io.github.resilience4j.timelimiter.TimeLimiter;
import io.github.resilience4j.timelimiter.TimeLimiterConfig;
import io.github.resilience4j.timelimiter.TimeLimiterRegistry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.cloud.gateway.filter.ReactiveLoadBalancerClientFilter;
import org.springframework.cloud.gateway.support.ServerWebExchangeUtils;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.net.URI;

/**
 * @author 十三月之夜
 * @time 2021/5/23 9:28 下午
 */
@Component
public class GlobalFilterLearnOne implements GlobalFilter, Ordered {

    @Autowired
    private CircuitBreakerRegistry customCircuitBreakerRegistry;

    @Autowired
    private CircuitBreakerConfig customCircuitBreakerConfig;

    @Autowired
    private TimeLimiterRegistry customTimeLimiterRegistry;

    @Autowired
    private TimeLimiterConfig customTimeLimiterConfig;

    @Autowired
    private RateLimiterRegistry customRateLimiterRegistry;

    @Autowired
    private RateLimiterConfig customRateLimiterConfig;

    @Autowired
    private RetryRegistry customRetryRegistry;

    @Autowired
    private RetryConfig customRetryConfig;

    // GlobalFilter会被所有的Routes调用，排列顺序是全局Filter和特定于某一Route的Filter一起排序
    // 有一些Spring默认的GlobalFilter，比如ForwardFilter，会在URI前缀为forward://时触发重定向操作
    // LoadBalancerClient是一个用于负载均衡的全局Filter，在URI前缀为lb:时启用
    // LoadBalancerClient现已不再使用，取而代之的是在WebFlux工作的ReactiveLoadBalancerClient
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        // 获取负载均衡之后的新的IP:PORT
        URI uri = exchange.getAttribute(ServerWebExchangeUtils.GATEWAY_REQUEST_URL_ATTR);
        String id = uri.getHost() + ":" + uri.getPort();
        CircuitBreaker circuitBreaker = customCircuitBreakerRegistry.circuitBreaker(id, customCircuitBreakerConfig);
        TimeLimiter timeLimiter = customTimeLimiterRegistry.timeLimiter(id, customTimeLimiterConfig);
        RateLimiter rateLimiter = customRateLimiterRegistry.rateLimiter(id, customRateLimiterConfig);
        Retry retry = customRetryRegistry.retry(id, customRetryConfig);
        // System.out.println(id);
        // 在这里配置我们需要的自定义R4J熔断机制，作为全局过滤器被添加到了每一个Route上
        System.out.println(circuitBreaker.getState());
        return chain.filter(exchange)
                .transformDeferred(CircuitBreakerOperator.of(circuitBreaker))
                .transformDeferred(TimeLimiterOperator.of(timeLimiter))
                .transformDeferred(RateLimiterOperator.of(rateLimiter))
                .transformDeferred(RetryOperator.of(retry))
                // 在这里我们不再进行舱壁隔离，因为WebFlux是基于NIO的，实际的耗时任务是开辟新的线程处理的，我们在这里能拦截的仅仅是进行读写请求的线程
                // 如果了解Netty应该知道这里的线程仅仅负责读接收和写发送，所以是多个请求共用一个线程，如果此时进行隔离，可能造成多个请求不可用
                // 会极大的影响系统吞吐量个可用性，反而起到了反作用，舱壁隔离最小单位是线程，所以我们再次关闭了它
                .onErrorResume(throwable -> {
                    // TODO do fallback
                    exchange.getResponse().setStatusCode(HttpStatus.INTERNAL_SERVER_ERROR);
                    return Mono.empty().then();
                });
    }

    private static Mono<Void> getVoidMono() {
        return Mono.empty().then();
    }

    // 重置顺序，数字越小，越在前，但是这是针对从上流到下流来说的；当接收响应时，数字越大越在前
    @Override
    public int getOrder() {
        // 所以这里我们设置熔断器在负载均衡器的后面，因为此时得到的新的URI已经是实际要请求的服务的地址，所以想要对实例熔断，就必须获得实际的地址。
        return ReactiveLoadBalancerClientFilter.LOAD_BALANCER_CLIENT_FILTER_ORDER + 1;
    }
}
