package com.eldie.signal.handlers;

import com.eldie.signal.redis.listener.ChannelListener;
import com.eldie.signal.redis.listener.ChannelPublisher;
import com.eldie.signal.redis.model.ChannelMessage;
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

import java.util.UUID;


@Component
public class ReactiveWebSocketChannelHandler implements WebSocketHandler {

    private final Logger log = LoggerFactory.getLogger(ReactiveWebSocketChannelHandler.class);


    @Autowired
    ChannelListener channelListener;

    @Autowired
    ChannelPublisher channelPublisher;

    @Override
    public Mono<Void> handle(WebSocketSession webSocketSession) {


        log.debug("HANDLE --> " + webSocketSession);

        webSocketSession.getAttributes().put("I", UUID.randomUUID().toString());

        RedisSubPubliser redisSubPubliser = new RedisSubPubliser(webSocketSession , channelListener, channelPublisher);

        webSocketSession.receive()
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

        public RedisSubPubliser(WebSocketSession webSocketSession , ChannelListener channelListener, ChannelPublisher channelPublisher) {
            this.webSocketSession = webSocketSession;
            this.channelListener = channelListener;
            this.channelPublisher = channelPublisher;

            channelListener.listen().subscribe((s) -> {
                subscriber.onNext(webSocketSession.textMessage(s.getMessage().toString()));
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

        public void customSend(String text){
            subscriber.onNext(webSocketSession.textMessage(text));
        }

        public void onNext(WebSocketMessage message) {

            String text = message.getPayloadAsText();

            log.debug("TEXT : {}" ,text);

            ChannelMessage cm = new ChannelMessage();
            cm.setChannelId(webSocketSession.getAttributes().get(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE).toString());
            cm.setMessageBody(text);
            cm.setMessageFrom(webSocketSession.getAttributes().get("I").toString());
            cm.setAction(ChannelMessage.Action.IN);
            cm.setType(ChannelMessage.Type.MESSAGE);
            channelPublisher.convertAndSend("channels", cm).subscribe((r) -> log.debug("send result : {}", r));

            this.receiveSubscrition.request(1);
        }

        @Override
        public void onError(Throwable t) {
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