package com.eldie.signal.redis.listener;

import com.eldie.signal.redis.model.ChannelMessage;
import org.springframework.data.redis.connection.ReactiveSubscription;
import org.springframework.data.redis.core.ReactiveRedisOperations;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

@Component
public class ChannelListener{


    private final Flux< ? extends ReactiveSubscription.Message<String, ChannelMessage>> channelPublisher;

    public ChannelListener(ReactiveRedisOperations<String, ChannelMessage> redisOperations){
        this.channelPublisher =  redisOperations.listenToChannel("channels");
    }

    public Flux< ? extends ReactiveSubscription.Message<String, ChannelMessage>> listen(){
     return this.channelPublisher;
    }
}
