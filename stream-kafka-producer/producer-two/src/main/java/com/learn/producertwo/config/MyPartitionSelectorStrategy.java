package com.learn.producertwo.config;

import org.springframework.cloud.stream.binder.PartitionSelectorStrategy;
import org.springframework.stereotype.Component;

/**
 * @author CodeWithBuff(给代码来点Buff)
 * @device MacBookPro
 * @time 2021/6/7 13:59
 */
@Component
public class MyPartitionSelectorStrategy implements PartitionSelectorStrategy {
    @Override
    public int selectPartition(Object key, int partitionCount) {
        System.out.println("count: " + partitionCount);
        return key.hashCode() % partitionCount;
    }
}
