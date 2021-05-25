package com.learn.simpleserviceoneclusterone.controller;

import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * @author 十三月之夜
 * @time 2021/5/23 12:47 下午
 */
@RestController
@RequestMapping("/simple-service-one")
public class BaseController {

    @GetMapping("/ok1")
    public String ok() {
        return "one: ok";
    }

    @GetMapping("/print/{str}")
    public String print(@PathVariable("str") String str) {
        System.out.println(str);
        return "one: printed";
    }

    @PostMapping("/more-info")
    public String moreInfo(@RequestParam Map<String, String> params) {
        for (Map.Entry<String, String> entry : params.entrySet()) {
            System.out.println("key: " + entry.getKey() + ", value: " + entry.getValue());
        }
        return "one: received";
    }
}
