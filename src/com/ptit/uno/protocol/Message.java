package com.ptit.uno.protocol;

import java.io.Serializable;

/**
 * Gói tin tổng quát bọc dữ liệu truyền tải giữa Client và Server qua TCP Socket.
 * Bám sát Slide b05 (sử dụng ObjectOutputStream / ObjectInputStream).
 */
public class Message implements Serializable {
    private static final long serialVersionUID = 1L;

    private MessageType type;
    private Object payload;
    private boolean success;
    private String message;

    public Message() {
        this.success = true;
    }

    public Message(MessageType type) {
        this();
        this.type = type;
    }

    public Message(MessageType type, Object payload) {
        this(type);
        this.payload = payload;
    }

    public Message(MessageType type, boolean success, String message) {
        this(type);
        this.success = success;
        this.message = message;
    }

    public Message(MessageType type, Object payload, boolean success, String message) {
        this.type = type;
        this.payload = payload;
        this.success = success;
        this.message = message;
    }

    public MessageType getType() {
        return type;
    }

    public void setType(MessageType type) {
        this.type = type;
    }

    public Object getPayload() {
        return payload;
    }

    public void setPayload(Object payload) {
        this.payload = payload;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    @Override
    public String toString() {
        return "Message{" +
                "type=" + type +
                ", success=" + success +
                ", message='" + message + '\'' +
                '}';
    }
}
