package com.learn.streamkafka;

import reactor.core.publisher.ConnectableFlux;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;

import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.LockSupport;

/**
 * @author CodeWithBuff(给代码来点Buff)
 * @device MBP
 * @time 2021/6/1 08:46
 */
public class TestMain {
    public static void main(String[] args) {
        // publish会动态的满足来自下流的每个请求，比如A请求一个，B请求0个这样的操作。
        // replay会重播到达第一个订阅者的数据，并发送给下一个订阅者，它拥有一个缓冲区，用来缓冲数据
        // publish和replay都会产生Hot版Publisher，同时replay只会缓存最新的数据
        // 我们通过ConnectableFlux来实现多订阅者机制
        // connect：当它被调用时，会把现已有的所有订阅者订阅到发布者上
        // autoConnect(n)：当拥有的足够多的订阅者时，会触发订阅
        // refCount(n)：当拥有足够多的订阅者时，触发订阅；当订阅者数量不够时，取消与上流的连接
        // refCount(n, timeout)：当数量不足时，等到timeout时间，然后在此检查数量，如果还是不足，取消连接
        ExecutorService executorService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
        Flux<String> flux0 = Flux.push(p1 -> {
            for (int i = 0; i < 10; ++ i) {
                LockSupport.parkNanos(Duration.ofMillis(1000).toNanos());
                p1.next(UUID.randomUUID().toString().substring(0, 5) + "_" + i);
            }
            p1.complete();
        });
        ConnectableFlux<String> publish0 = flux0.subscribeOn(Schedulers.boundedElastic()).publish();
        System.out.println("begin run");
        executorService.submit(() -> {
            System.out.println("haven subscribed 1");
            publish0.subscribe(p1 -> {
                System.out.println("s1: " + p1);
            });
        });
        new Thread(() -> {
            LockSupport.parkNanos(Duration.ofMillis(2500).toNanos());
            System.out.println("haven subscribed 2");
            publish0.subscribe(p2 -> {
                System.out.println("s2: " + p2);
            });
        }).start();
        // 给第一个订阅者点运行时间
        LockSupport.parkNanos(Duration.ofMillis(200).toNanos());
        // 只有当调用connect()时才会触发消息的发布，此时会接连不断的发布消息，而忽略是否被订阅
        publish0.connect();
        // 使用replay会导致新的订阅者可能只会订阅到新的数据，而无法得知之前的数据。
        // replay也会得到一个Hot版发布者
        ConnectableFlux<String> publish1 = flux0.subscribeOn(Schedulers.boundedElastic()).replay(5);
        new Thread(() -> {
            System.out.println("haven subscribed 3");
            publish1.subscribe(p1 -> {
                System.out.println("s3: " + p1);
            });
        }).start();
        // 一旦connect被调用，就会忽略是否有订阅者而开始数据的发布，所以是Hot版
        publish1.connect();
        LockSupport.parkNanos(Duration.ofMillis(10100).toNanos());
        new Thread(() -> {
            System.out.println("haven subscribed 4");
            publish1.subscribe(p2 -> {
                System.out.println("s4: " + p2);
            });
        }).start();
        ConnectableFlux<String> publish2 = flux0.subscribeOn(Schedulers.boundedElastic()).publish();
        publish2.autoConnect(3);
        LockSupport.park();
    }
}
