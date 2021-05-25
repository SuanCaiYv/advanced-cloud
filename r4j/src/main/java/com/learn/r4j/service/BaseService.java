package com.learn.r4j.service;

import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

/**
 * @author 十三月之夜
 * @time 2021/5/23 11:45 下午
 */
@Service
public interface BaseService {
    String ignoreException();

    CompletableFuture<String> ignoreTimeout();
}
