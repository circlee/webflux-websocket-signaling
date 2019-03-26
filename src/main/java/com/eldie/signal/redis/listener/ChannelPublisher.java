package com.eldie.signal.redis.listener;

import com.eldie.signal.redis.model.ChannelMessage;
import org.springframework.data.redis.core.ReactiveRedisOperations;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
public class ChannelPublisher {

    private final ReactiveRedisOperations<String, ChannelMessage> redisOperations;

    public ChannelPublisher(ReactiveRedisOperations<String, ChannelMessage> redisOperations){
        this.redisOperations = redisOperations;
    }

    public Mono<Long> convertAndSend(String destination, ChannelMessage message){
        return redisOperations.convertAndSend(destination, message);
    }
}
