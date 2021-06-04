package com.learn.streamkafka;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.kafka.annotation.EnableKafka;

import java.util.function.Consumer;
import java.util.function.Supplier;

@SpringBootApplication
@EnableKafka
public class StreamKafkaApplication {

    @Bean
    public Consumer<String> input1() {
        return input -> {
            System.out.println("get: " + input);
        };
    }

    @Bean
    public Supplier<String> output1() {
        return () -> {
            return "aaa";
        };
    }

    public static void main(String[] args) {
        SpringApplication.run(StreamKafkaApplication.class, args);
    }

}
