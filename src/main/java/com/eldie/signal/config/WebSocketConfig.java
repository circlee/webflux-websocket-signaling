package com.eldie.signal.config;

import com.eldie.signal.handlers.ReactiveWebSocketChannelHandler;
import com.eldie.signal.handlers.ReactiveWebSocketEchoHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.HandlerMapping;
import org.springframework.web.reactive.HandlerResult;
import org.springframework.web.reactive.handler.SimpleUrlHandlerMapping;
import org.springframework.web.reactive.socket.WebSocketHandler;
import org.springframework.web.reactive.socket.server.support.HandshakeWebSocketService;
import org.springframework.web.reactive.socket.server.support.WebSocketHandlerAdapter;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class WebSocketConfig {


    private final Logger log = LoggerFactory.getLogger(WebSocketConfig.class);

    @Autowired
    ReactiveWebSocketChannelHandler reactiveWebSocketChannelHandler;

    @Autowired
    ReactiveWebSocketEchoHandler reactiveWebSocketEchoHandler;

    @Bean
    public HandlerMapping webSocketHandlerxMapping() {
        Map<String, WebSocketHandler> map = new HashMap<>();
        map.put("/echo", reactiveWebSocketEchoHandler);

        map.put("/channels/{channel}", reactiveWebSocketChannelHandler);

        SimpleUrlHandlerMapping handlerMapping = new SimpleUrlHandlerMapping() {

        };

        handlerMapping.setOrder(1);
        handlerMapping.setUrlMap(map);




        return handlerMapping;
    }

    @Bean
    WebSocketHandlerAdapter getWebsocketHandlerAdapter(){


        HandshakeWebSocketService handshakeWebSocketService = new HandshakeWebSocketService();
        handshakeWebSocketService.setSessionAttributePredicate( k -> true);

        WebSocketHandlerAdapter wsha = new WebSocketHandlerAdapter(handshakeWebSocketService){
            @Override
            public Mono<HandlerResult> handle(ServerWebExchange exchange, Object handler) {

                Map<String, Object> attributes = exchange.getAttributes();

                exchange.getSession().subscribe((session) -> {
                    session.getAttributes().putAll(attributes);
                });

                return super.handle(exchange, handler);
            }
        };

        return wsha;
    }
}
