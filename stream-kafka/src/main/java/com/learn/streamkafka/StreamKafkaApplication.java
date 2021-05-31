package com.learn.streamkafka;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import java.util.function.Function;

@SpringBootApplication
public class StreamKafkaApplication {

    @Bean
    public Function<String, String> toUpperCase() {
        return val -> val.toUpperCase();
    }

    public static void main(String[] args) {
        SpringApplication.run(StreamKafkaApplication.class, args);
    }

}
