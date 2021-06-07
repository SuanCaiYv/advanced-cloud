package com.learn.producertwo;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;

@SpringBootApplication
@RestController
public class ProducerTwoApplication {

    @Autowired
    private StreamBridge streamBridge;

    @GetMapping("/send/{partition}/{val}")
    public String send(@PathVariable("partition") Integer partition, @PathVariable("val") String val) {
        HashMap<String, Object> headers = new HashMap<>();
        headers.put("key", partition);
        streamBridge.send("source1-out-0", new GenericMessage<>(val, headers));
        return "ok";
    }

    public static void main(String[] args) {
        SpringApplication.run(ProducerTwoApplication.class, args);
    }

}
