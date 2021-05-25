package com.learn.simpleservicefourclusterone.controller;

import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.locks.LockSupport;

/**
 * @author 十三月之夜
 * @time 2021/5/23 12:47 下午
 */
@RestController
@RequestMapping("/simple-service-four")
public class BaseController {

    @GetMapping("/ok1")
    public String ok() {
        LockSupport.parkNanos(Duration.ofSeconds(10).toNanos());
        return "ok";
    }

    @GetMapping("/print/{str}")
    public String print(@PathVariable("str") String str) {
        System.out.println(str);
        return "printed";
    }

    @PostMapping("/more-info")
    public String moreInfo(@RequestParam Map<String, String> params) {
        for (Map.Entry<String, String> entry : params.entrySet()) {
            System.out.println("key: " + entry.getKey() + ", value: " + entry.getValue());
        }
        return "received";
    }

    @GetMapping("/404")
    public String notFound() {
        return "404 not found";
    }
}
