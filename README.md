# webflux-websocket-signaling

--- 

### dependency 
```groovy
implementation 'org.springframework.boot:spring-boot-starter-data-redis-reactive'
implementation 'org.springframework.boot:spring-boot-starter-webflux'
```

### 목적
- Spring Webflux 기반의 WebSocket 서버 구축
- 최종적으론 WebRtc에 사용할 signaling 서버를 구축 목적 (peer 간 메타 정보 전달을 보장하기 위해 peer 간 1:1 채널이 필요)

### 기록
- 실행 일단 gradle로 
```shell
gradle bootRun
```
- 채널 메시지 경우 멀티인스턴스 간 메시지 브로커 역할의 Redis 필요 
```shell
docker run --name some-redis -p 6379:6379 -d redis
```
- reactive redis 사용으로 channel을 listen 하는 Publisher를 websocket handler에서 사용한다.
- WebSocketHandlerAdapter를 handle 메소드 재정의와 HandshakeWebSocketService 에 predicate 추가하여 
  websocket 요청의 http handshake 시에 요청정보를 해당 WebSocketSession 의 Attribute로 넘길 수 있다.
```java
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
```
