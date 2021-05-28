package com.learn.gateway.filter;

import com.learn.gateway.util.ExchangeUtils;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.reactor.circuitbreaker.operator.CircuitBreakerOperator;
import io.github.resilience4j.reactor.timelimiter.TimeLimiterOperator;
import io.github.resilience4j.timelimiter.TimeLimiter;
import io.github.resilience4j.timelimiter.TimeLimiterConfig;
import io.github.resilience4j.timelimiter.TimeLimiterRegistry;
import io.vavr.CheckedFunction0;
import io.vavr.control.Try;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.cloud.gateway.filter.ReactiveLoadBalancerClientFilter;
import org.springframework.cloud.gateway.support.ServerWebExchangeUtils;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerResponse;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.util.function.Function;

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
        // System.out.println(circuitBreaker.getName());
        TimeLimiter timeLimiter = customTimeLimiterRegistry.timeLimiter(id, customTimeLimiterConfig);
        // System.out.println(id);
        // 在这里配置我们需要的自定义R4J熔断机制，作为全局过滤器被添加到了每一个Route上
        System.out.println(circuitBreaker.getState());
        return chain.filter(exchange)
                .transformDeferred(CircuitBreakerOperator.of(circuitBreaker))
                .transformDeferred(TimeLimiterOperator.of(timeLimiter))
                .onErrorResume(throwable -> {
                    // do fallback
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
