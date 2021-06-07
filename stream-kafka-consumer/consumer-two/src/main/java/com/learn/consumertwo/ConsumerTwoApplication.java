package com.learn.consumertwo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.messaging.Message;

import java.util.function.Consumer;

@SpringBootApplication
public class ConsumerTwoApplication {

    @Bean
    public Consumer<Message<String>> input1() {
        return stringMessage -> {
            System.out.println("key: " + stringMessage.getHeaders().get("key"));
            System.out.println("msg: " + stringMessage.getPayload());
        };
    }

    public static void main(String[] args) {
        SpringApplication.run(ConsumerTwoApplication.class, args);
    }

}
