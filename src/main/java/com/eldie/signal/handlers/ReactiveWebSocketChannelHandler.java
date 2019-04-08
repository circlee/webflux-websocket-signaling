package com.eldie.signal.handlers;

import com.eldie.signal.redis.listener.ChannelListener;
import com.eldie.signal.redis.listener.ChannelPublisher;
import com.eldie.signal.redis.model.BaseMessage;
import com.eldie.signal.redis.model.ChannelMessage;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.HandlerMapping;
import org.springframework.web.reactive.socket.WebSocketHandler;
import org.springframework.web.reactive.socket.WebSocketMessage;
import org.springframework.web.reactive.socket.WebSocketSession;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.util.UUID;


@Component
public class ReactiveWebSocketChannelHandler implements WebSocketHandler {

    private final Logger log = LoggerFactory.getLogger(ReactiveWebSocketChannelHandler.class);


    ObjectMapper om = new ObjectMapper();

    @Autowired
    ChannelListener channelListener;

    @Autowired
    ChannelPublisher channelPublisher;

    @Autowired
    ObjectMapper objectMapper;



    @Override
    public Mono<Void> handle(WebSocketSession webSocketSession) {


        log.debug("HANDLE --> " + webSocketSession);

        webSocketSession.getAttributes().put("CHANNEL_ID", webSocketSession.getAttributes().get(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE));

        webSocketSession.getAttributes().put("I", UUID.randomUUID().toString());

        RedisSubPubliser redisSubPubliser = new RedisSubPubliser(webSocketSession , channelListener, channelPublisher, objectMapper);

        webSocketSession.receive()
                .limitRate(25)
                .doOnTerminate(()-> { log.debug("doOnTerminate"); })
                .doFinally(a -> {
                    log.debug("doFinally");
                    webSocketSession.close();
                })
                .subscribe(redisSubPubliser);


        return webSocketSession.send(redisSubPubliser);
    }

    private static class RedisSubPubliser implements Publisher<WebSocketMessage>, Subscriber<WebSocketMessage>, Subscription {

        private final Logger log = LoggerFactory.getLogger(RedisSubPubliser.class);

        Subscriber<? super WebSocketMessage> subscriber;

        WebSocketSession webSocketSession;

        Subscription receiveSubscrition;

        ChannelListener channelListener;

        ChannelPublisher channelPublisher;

        ObjectMapper objectMapper;

        String channelId;
        String channelPath;
        String me;

        public RedisSubPubliser(WebSocketSession webSocketSession , ChannelListener channelListener, ChannelPublisher channelPublisher, ObjectMapper objectMapper) {
            this.webSocketSession = webSocketSession;
            this.channelListener = channelListener;
            this.channelPublisher = channelPublisher;
            this.objectMapper = objectMapper;
            channelId = webSocketSession.getAttributes().get("CHANNEL_ID").toString();
            channelPath = "channels/"+ channelId;
            me = webSocketSession.getAttributes().get("I").toString();
            channelListener.listen()
                    .filter(m -> m.getChannel().equals(channelPath))
                    .map(message -> message.getMessage())
                    .filter(message -> !message.getMessageFrom().equals(me))
                    .subscribe((message) -> {
                        try {
                            subscriber.onNext(webSocketSession.textMessage(objectMapper.writeValueAsString(message)));
                        } catch (JsonProcessingException e) {
                            e.printStackTrace();
                        }
                    });
        }

        @Override
        public void subscribe(Subscriber<? super WebSocketMessage> s) {
            log.debug("subscribe : " + s);
            subscriber = s;
            subscriber.onSubscribe(this);
        }

        @Override
        public void onSubscribe(Subscription receiveSubscrition) {
            log.debug("onSubscribe : " + receiveSubscrition);
            this.receiveSubscrition = receiveSubscrition;
            this.receiveSubscrition.request(1);
        }



        public void onNext(WebSocketMessage message) {


            ChannelMessage cm = new ChannelMessage();

            WebSocketMessage.Type type = message.getType();
            String payloadText = message.getPayloadAsText();

            cm.setChannelId(channelId);
            cm.setMessageFrom(me);

            log.debug("TEXT : {}" ,payloadText);

            try {
                BaseMessage baseMessage = objectMapper.readValue(payloadText, BaseMessage.class);

                cm.setMessageBody(baseMessage.getMessageBody());
                cm.setType(baseMessage.getType());

            } catch (IOException e) {
                e.printStackTrace();
            }

            channelPublisher.convertAndSend(channelPath, cm).subscribe((r) -> log.debug("send result : {}", r));

            this.receiveSubscrition.request(1);
        }

        @Override
        public void onError(Throwable t) {

            log.error("Error : {}", t);
            subscriber.onError(t);
        }

        @Override
        public void onComplete() {
            subscriber.onComplete();
        }

        @Override
        public void request(long n) {
            log.debug("from outbound send subscriber request : {}" , n);
        }

        @Override
        public void cancel() {
            log.debug("from outbound send subscriber cancel");
        }
    };

}