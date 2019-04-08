package com.eldie.signal.redis.model;

public class BaseMessage {


    private Type type;
    private String messageBody;

    public enum Type {
        MESSAGE
        , CHANNEL_JOIN
        , RTC_OFFER
        , RTC_ANSWER
        , RTC_ICE_CANDIDATE
        , RTC_ICE_CANDIDATE_CHANGE
    }



    public String getMessageBody() {
        return messageBody;
    }

    public void setMessageBody(String messageBody) {
        this.messageBody = messageBody;
    }

    public Type getType() {
        return type;
    }

    public void setType(Type type) {
        this.type = type;
    }

    @Override
    public String toString() {
        return "WebSocketMessage{" +
                ", messageBody='" + messageBody + '\'' +
                ", type=" + type +
                '}';
    }
}
