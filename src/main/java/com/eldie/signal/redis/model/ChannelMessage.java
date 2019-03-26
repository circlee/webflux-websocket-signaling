package com.eldie.signal.redis.model;

public class ChannelMessage {

    private String channelId;

    private String messageFrom;

    private String messageBody;

    private Action action;

    private Type type;

    public enum Action {
        IN
        , OUT
    }

    public enum Type {
        MESSAGE
        , RTC_META
    }

    public String getChannelId() {
        return channelId;
    }

    public void setChannelId(String channelId) {
        this.channelId = channelId;
    }

    public String getMessageFrom() {
        return messageFrom;
    }

    public void setMessageFrom(String messageFrom) {
        this.messageFrom = messageFrom;
    }

    public String getMessageBody() {
        return messageBody;
    }

    public void setMessageBody(String messageBody) {
        this.messageBody = messageBody;
    }

    public Action getAction() {
        return action;
    }

    public void setAction(Action action) {
        this.action = action;
    }

    public Type getType() {
        return type;
    }

    public void setType(Type type) {
        this.type = type;
    }

    @Override
    public String toString() {
        return "ChannelMessage{" +
                "channelId='" + channelId + '\'' +
                ", messageFrom='" + messageFrom + '\'' +
                ", messageBody='" + messageBody + '\'' +
                ", action=" + action +
                ", type=" + type +
                '}';
    }
}
