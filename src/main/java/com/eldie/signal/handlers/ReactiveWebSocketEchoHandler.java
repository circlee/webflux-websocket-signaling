package com.eldie.signal.handlers;

import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.socket.WebSocketHandler;
import org.springframework.web.reactive.socket.WebSocketMessage;
import org.springframework.web.reactive.socket.WebSocketSession;
import reactor.core.publisher.Mono;

import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@Component
public class ReactiveWebSocketEchoHandler implements WebSocketHandler {

    private final Logger log = LoggerFactory.getLogger(ReactiveWebSocketEchoHandler.class);


    @Override
    public Mono<Void> handle(WebSocketSession webSocketSession) {


        log.debug("HANDLE --> " + webSocketSession);

        log.debug("handShakeInfo : {}" , webSocketSession.getHandshakeInfo());

        log.debug("attribute : {}", webSocketSession.getAttributes());


        EchoSubPublisher echoSubPublisher = new EchoSubPublisher(webSocketSession);
        webSocketSession.receive()
                .doOnTerminate(()-> {log.debug("doOnTerminate");})
                .doFinally(a -> {
                    log.debug("doFinally");
                    webSocketSession.close();
                })
                .subscribe(echoSubPublisher);

        Executors.newSingleThreadExecutor().execute(() -> {
            while(true) {

                try {
                    TimeUnit.SECONDS.sleep(5);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                echoSubPublisher.customSend(" From Server Ping " + System.currentTimeMillis());
            }
        });



        webSocketSession.textMessage("");


        return webSocketSession.send(echoSubPublisher);
    }

    private static class EchoSubPublisher implements Publisher<WebSocketMessage>, Subscriber<WebSocketMessage>, Subscription {

        private final Logger log = LoggerFactory.getLogger(EchoSubPublisher.class);

        Subscriber<? super WebSocketMessage> subscriber;

        WebSocketSession webSocketSession;

        Subscription receiveSubscrition;

        public EchoSubPublisher(WebSocketSession webSocketSession) {
            this.webSocketSession = webSocketSession;
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
            subscriber.onNext(webSocketSession.textMessage(text));

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
