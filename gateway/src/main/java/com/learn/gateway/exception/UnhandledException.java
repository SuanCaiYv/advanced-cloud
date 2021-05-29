package com.learn.gateway.exception;

import com.usthe.sureness.processor.exception.SurenessAuthenticationException;
import lombok.*;

import java.time.LocalDateTime;

/**
 * @author 十三月之夜
 * @time 2021/4/22 1:07 下午
 */
@Data
@Builder
@With
@EqualsAndHashCode(callSuper = true)
public class UnhandledException extends SurenessAuthenticationException {

    public UnhandledException(String msg) {
        super(msg);
    }

    public UnhandledException() {
        super(null);
    }

    private int errCode;

    private String errMsg;

    private StackTraceElement errStackTraceElement;

    private LocalDateTime errTime;
}
