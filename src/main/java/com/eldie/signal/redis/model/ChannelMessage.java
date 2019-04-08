package com.eldie.signal.redis.model;

public class ChannelMessage  extends BaseMessage{

    private String channelId;

    private String messageFrom;


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


    @Override
    public String toString() {
        return "ChannelMessage{" +
                "channelId='" + channelId + '\'' +
                ", messageFrom='" + messageFrom + '\'' +
                '}';
    }
}
