package com.learn.producertwo.config;

import org.springframework.cloud.stream.binder.PartitionKeyExtractorStrategy;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Component;

/**
 * @author CodeWithBuff(给代码来点Buff)
 * @device MacBookPro
 * @time 2021/6/7 14:00
 */
@Component
public class MyPartitionKeyExtractorStrategy implements PartitionKeyExtractorStrategy {
    @Override
    public Object extractKey(Message<?> message) {
        return message.getHeaders().get("key");
    }
}
