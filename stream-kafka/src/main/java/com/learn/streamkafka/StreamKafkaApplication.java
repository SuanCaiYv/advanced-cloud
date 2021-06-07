package com.learn.streamkafka;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.context.annotation.Bean;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.util.MimeType;
import org.springframework.util.MimeTypeUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;
import reactor.util.function.Tuple2;
import reactor.util.function.Tuples;

import java.util.HashMap;
import java.util.function.Consumer;
import java.util.function.Function;

@SpringBootApplication
@RestController
public class StreamKafkaApplication {

    // 一个ConsumerBean只能消费一个topic，即使配置文件写了两个，也是不行的
    @Bean
    public Consumer<String> input1() {
        return input -> {
            System.out.println("get1: " + input);
        };
    }

    @Bean
    public Consumer<String> input2() {
        return input -> {
            System.out.println("get2: " + input);
        };
    }

    @Bean Consumer<String> input3() {
        return input -> {
            System.out.println("get3: " + input);
        };
    }

    @Bean
    public Consumer<String> input4() {
        return input -> {
            System.out.println("get4: " + input);
        };
    }

    @Bean
    public Consumer<String> input5() {
        return input -> {
            System.out.println("get5: " + input);
        };
    }

    @Bean
    public Function<Flux<String>, Flux<String>> function1() {
        return input -> {
            return input.map(String::toUpperCase);
        };
    }

    @Bean
    public Function<Flux<String>, Flux<String>> function2() {
        return input -> {
            return input.map(s -> s + "aaa");
        };
    }

    // 多输入情况
    @Bean
    public Function<Tuple2<Flux<String>, Flux<String>>, Flux<String>> function3() {
        return input -> {
            return input.getT1()
                    .flatMap(p1 -> {
                        return input.getT2()
                                .map(p2 -> {
                                    return p1 + "_" + p2;
                                });
                    });
        };
    }

    // 多返回值的例子
    @Bean
    public Function<Flux<String>, Tuple2<Flux<String>, Flux<String>>> function4() {
        return flux -> {
            Flux<String> connectedFlux = flux.publish().autoConnect(2);
            Sinks.Many<String> stringMany1 = Sinks.many().unicast().onBackpressureBuffer();
            Sinks.Many<String> stringMany2 = Sinks.many().unicast().onBackpressureBuffer();
            Flux<String> evenFlux = connectedFlux.doOnNext(p1 -> stringMany1.emitNext(p1 + "_1", Sinks.EmitFailureHandler.FAIL_FAST));
            Flux<String> oddFlux = connectedFlux.doOnNext(p1 -> stringMany2.emitNext(p1 + "_2", Sinks.EmitFailureHandler.FAIL_FAST));
            return Tuples.of(stringMany1.asFlux().doOnSubscribe(x -> evenFlux.subscribe()), stringMany2.asFlux().doOnSubscribe(x -> oddFlux.subscribe()));
        };
    }

    // 此外，还有批处理，就是把多个输入整合成List；输入List自动分解成多个output，比较简单，在配置文件中开启一下就行
    // 除了自动接收消息，SpringCloudStream还提供了轮询消息功能，允许消费者手动询问是否有消息可用

    // 自动调用的例子
//    @Bean
//    public Supplier<String> output1() {
//        return () -> {
//            return "aaa";
//        };
//    }

    @Autowired
    private StreamBridge streamBridge;

    @GetMapping("/send1/{val}")
    public String send1(@PathVariable("val") String val) {
        // bindingName对应的Bean是不存在的，所以会使用spring.cloud.stream.source配置的sourceName(类似BeanName)作为参数，然后去找
        // 这个sourceName下名称与bindingName匹配的binding进行发送
        streamBridge.send("source1-out-0", val);
        return "ok";
    }

    @GetMapping("/send2/{val}")
    public String send2(@PathVariable("val") String val) {
        // 如果binding不存在，会自动解析成destination，然后创建，否则使用已存在的
        // 创建的bingName可能是个随机串，但是它的destination=test_topic
        streamBridge.send("test_topic", val);
        return "ok";
    }

    @GetMapping("/send3/{val1}/{val2}")
    public String send3(@PathVariable("val1") String val1, @PathVariable("val2") String val2) {
        streamBridge.send("test_topic3", val1);
        streamBridge.send("test_topic4", val2);
        return "ok";
    }

    @GetMapping("/send4/{val}")
    public String send4(@PathVariable("val") String val) {
        HashMap<String, Object> headers = new HashMap<>();
        headers.put("key", val.hashCode());
        Message<String> message = new GenericMessage<>(val, headers);
        streamBridge.send("source1-out-0", message, MimeTypeUtils.TEXT_PLAIN);
        return "ok";
    }

    public static void main(String[] args) {
        SpringApplication.run(StreamKafkaApplication.class, args);
    }

}
