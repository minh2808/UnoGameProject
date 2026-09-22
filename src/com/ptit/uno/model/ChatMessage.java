package com.ptit.uno.model;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Tin nhắn chat trong phòng chờ hoặc trong bàn chơi.
 */
public class ChatMessage implements Serializable {
    private static final long serialVersionUID = 1L;

    private int roomId;
    private String sender;
    private String content;
    private String timestamp;

    public ChatMessage() {
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");
        this.timestamp = sdf.format(new Date());
    }

    public ChatMessage(int roomId, String sender, String content) {
        this();
        this.roomId = roomId;
        this.sender = sender;
        this.content = content;
    }

    public int getRoomId() {
        return roomId;
    }

    public void setRoomId(int roomId) {
        this.roomId = roomId;
    }

    public String getSender() {
        return sender;
    }

    public void setSender(String sender) {
        this.sender = sender;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

    @Override
    public String toString() {
        return "[" + timestamp + "] " + sender + ": " + content;
    }
}
