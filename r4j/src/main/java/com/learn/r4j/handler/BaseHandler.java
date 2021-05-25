package com.learn.r4j.handler;

import com.learn.r4j.service.BaseService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

import java.util.Random;
import java.util.concurrent.ExecutionException;

/**
 * @author 十三月之夜
 * @time 2021/5/23 10:05 下午
 */
@Component
public class BaseHandler {

    @Autowired
    private BaseService baseService;

    public Mono<ServerResponse> ok(ServerRequest serverRequest) {
        return ServerResponse
                .ok()
                .body(Mono.just("ok"), String.class);
    }

    public Mono<ServerResponse> mustFail(ServerRequest serverRequest) {
        String str = baseService.ignoreException();
        return ServerResponse
                .ok()
                .body(Mono.just(str), String.class);
    }

    public Mono<ServerResponse> mayFail(ServerRequest serverRequest) {
        Random random = new Random();
        int time = random.nextInt(1000);
        if (time % 2 == 0) {
            return ServerResponse
                    .ok()
                    .body(Mono.just(baseService.ignoreException()), String.class);
        } else {
            return ServerResponse
                    .ok()
                    .body(Mono.just("ok"), String.class);
        }
    }

    public Mono<ServerResponse> mustTimedOut(ServerRequest serverRequest) {
        return ServerResponse.ok()
                .body(Mono.fromFuture(baseService.ignoreTimeout()), String.class);
    }

    public Mono<ServerResponse> mayTimedOut(ServerRequest serverRequest) {
        Random random = new Random();
        int time = random.nextInt(1000);
        if (time % 2 == 0) {
            return ServerResponse
                    .ok()
                    .body(Mono.fromFuture(baseService.ignoreTimeout()), String.class);
        }
        return ServerResponse
                .ok()
                .body(Mono.just("ok"), String.class);
    }
}
