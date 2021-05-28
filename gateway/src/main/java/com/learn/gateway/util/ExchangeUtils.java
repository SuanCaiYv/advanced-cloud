package com.learn.gateway.util;

import org.reactivestreams.Publisher;
import org.springframework.cloud.gateway.support.ServerWebExchangeUtils;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.http.server.reactive.ServerHttpResponseDecorator;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * @author 十三月之夜
 * @time 2021/5/28 3:06 下午
 */
public class ExchangeUtils {

    public static ServerWebExchange overrideResponse(byte[] content, ServerWebExchange exchange) {
        ServerHttpResponse originalResponse = exchange.getResponse();
        DataBufferFactory bufferFactory = originalResponse.bufferFactory();
        ServerHttpResponseDecorator decoratedResponse = new ServerHttpResponseDecorator(originalResponse) {
            @Override
            @SuppressWarnings("unchecked")
            public Mono<Void> writeWith(Publisher<? extends DataBuffer> body) {
                Flux<DataBuffer> fluxBody = (Flux<DataBuffer>) body;
                return super.writeWith(fluxBody.map(dataBuffer -> {
                    DataBufferUtils.release(dataBuffer);
                    return bufferFactory.wrap(content);
                }));
            }
        };
        return exchange
                .mutate()
                .response(decoratedResponse)
                .build();
    }
}
