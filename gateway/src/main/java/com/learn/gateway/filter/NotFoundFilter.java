package com.learn.gateway.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.reactivestreams.Publisher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.support.ServerWebExchangeUtils;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.http.server.reactive.ServerHttpResponseDecorator;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.util.HashMap;

/**
 * @author 十三月之夜
 * @time 2021/5/24 4:52 下午
 */
@Component
public class NotFoundFilter implements GatewayFilter, Ordered {

    @Autowired
    private ObjectMapper objectMapper;

    // 我的个人理解是这样的：ServerResponse负责保存响应数据，然后位于请求链的最前面的一个名为WriteFilter的全局Filter(我猜他叫这个名字)负责把
    // ServerResponse的Body写入到某一个ServerWebExchange里面去，而这个ServerWebExchange是作为参数传递到ServerResponse的writeTo方法里去的
    // 而这就是一个WebFlux的写出流程，类似Golang的写出方法，都是调用数据载体的写出方法，并传递一个参数作为写出对象
    // 我为什么大花篇幅的介绍WebFlux的写出原理呢？首先就是这和Netty是一致的（用过的都懂），其次这解释了下面这个方法是怎么获取Response的Body的
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpResponse originalResponse = exchange.getResponse();
        DataBufferFactory bufferFactory = originalResponse.bufferFactory();
        // 这里我们设置了一个ServerWebExchange#ServerHttpResponse的代理类进行写出拦截，把原本直接写出到默认ServerHttpResponse的数据转移到我们的代理类中
        // 注意⚠️这里的ServerHttpResponse是一个和ServerWebExchange绑定的ServerResponse写出对象，和数据载体ServerResponse没有关系
        ServerHttpResponseDecorator decoratedResponse = new ServerHttpResponseDecorator(originalResponse) {
            // 写出的核心方法，用过Netty应该可以理解，writeAndFlush()负责做收尾工作，一般实际数据不在这里写出
            @Override
            @SuppressWarnings("unchecked")
            public Mono<Void> writeWith(Publisher<? extends DataBuffer> body) {
                String contentType = exchange.getAttribute(ServerWebExchangeUtils.ORIGINAL_RESPONSE_CONTENT_TYPE_ATTR);
                if (!MediaType.APPLICATION_JSON_VALUE.equals(contentType)) {
                    return super.writeWith(body);
                }
                // 判断数据是否可写
                if (body instanceof Flux) {
                    Flux<DataBuffer> fluxBody = (Flux<DataBuffer>) body;
                    return super.writeWith(fluxBody.map(dataBuffer -> {
                        byte[] content = new byte[dataBuffer.readableByteCount()];
                        dataBuffer.read(content);
                        System.out.println(new String(content, 0, content.length));
                        // 释放掉内存
                        DataBufferUtils.release(dataBuffer);
                        byte[] uppedContent = write404(content, getDelegate());
                        if (uppedContent != null) {
                            // 为什么调用原ServerHttpResponse的bufferFactory，是因为代理对象仅仅是对原对象的一个引用，所以可以直接用
                            return bufferFactory.wrap(uppedContent);
                        } else {
                            return bufferFactory.wrap(new byte[0]);
                        }
                    }));
                }
                // 进行实际的写入到ServerHttpResponse
                return super.writeWith(body);
            }
        };
        // 进行代理类替换
        return chain
                .filter(exchange
                        // 生成一个代理exchange
                        .mutate()
                        // 并使用我们的代理ServerHttpResponse替换代理exchange中的ServerHttpResponse
                        .response(decoratedResponse)
                        .build()
                );
    }

    @SuppressWarnings("unchecked")
    private byte[] write404(byte[] content, ServerHttpResponse serverHttpResponse) {
        try {
            HashMap<String, Object> hashMap = objectMapper.readValue(content, HashMap.class);
            Object status = hashMap.get("status");
            if (status != null) {
                int statusCode = Integer.parseInt(status.toString());
                if (statusCode == HttpStatus.NOT_FOUND.value()) {
                    serverHttpResponse.setStatusCode(HttpStatus.FOUND);
                    serverHttpResponse.getHeaders().set(HttpHeaders.LOCATION, "/simple-service-four/404");
                    return null;
                } else {
                    return content;
                }
            } else {
                return content;
            }
        } catch (IOException e) {
            return content;
        }
    }

    @Override
    public int getOrder() {
        // 为什么是-2，是因为位于-1的正是WriteFilter，所以我们的拦截Filter要位于它前面
        return -2;
    }
}
