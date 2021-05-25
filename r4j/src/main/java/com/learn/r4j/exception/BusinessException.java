package com.learn.r4j.exception;

/**
 * 不可以被忽略
 */
public class BusinessException extends RuntimeException {

    public BusinessException(String message) {
        super(message);
    }
}
