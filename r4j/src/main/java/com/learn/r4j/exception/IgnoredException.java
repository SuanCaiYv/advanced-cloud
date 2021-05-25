package com.learn.r4j.exception;

/**
 * @author 十三月之夜
 * @time 2021/4/21 3:34 下午
 * 可以被忽略
 */
public class IgnoredException extends RuntimeException {

    private String msg;

    public IgnoredException(String msg) {
        super(msg);
        this.msg = msg;
    }
}
