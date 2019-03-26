package com.eldie.signal.redis;

import com.eldie.signal.redis.model.ChannelMessage;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory;
import org.springframework.data.redis.connection.ReactiveSubscription;
import org.springframework.data.redis.core.ReactiveRedisOperations;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import reactor.core.publisher.Flux;

@Configuration
public class RedisConfig {


    @Bean
    ReactiveRedisOperations<String, ChannelMessage> redisOperations(ReactiveRedisConnectionFactory factory) {

        Jackson2JsonRedisSerializer<ChannelMessage> serializer = new Jackson2JsonRedisSerializer<>(ChannelMessage.class);
        RedisSerializationContext.RedisSerializationContextBuilder<String, ChannelMessage> builder =
                RedisSerializationContext.newSerializationContext(new StringRedisSerializer());

        RedisSerializationContext<String, ChannelMessage> context = builder.value(serializer).build();

        return new ReactiveRedisTemplate<>(factory, context);
    }


    @Bean("channelListen")
    public Flux<? extends ReactiveSubscription.Message<String, ChannelMessage>> test(ReactiveRedisOperations<String, ChannelMessage> redisOperations){
        return redisOperations.listenToChannel("channels");
    }

}
