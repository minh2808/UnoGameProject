package com.ptit.uno.client.control;

import com.ptit.uno.protocol.Message;

import java.io.ObjectInputStream;

/**
 * Luồng chạy ngầm liên tục đọc gói tin từ Socket Server gửi về máy khách.
 * Giúp giao diện Java Swing không bao giờ bị treo/đơ khi chờ phản hồi mạng.
 * Bám sát Slide b02-1 & b05.
 */
public class ClientReceiverThread extends Thread {
    private final ObjectInputStream ois;
    private final ClientControl clientControl;
    private boolean isRunning;

    public ClientReceiverThread(ObjectInputStream ois, ClientControl clientControl) {
        this.ois = ois;
        this.clientControl = clientControl;
        this.isRunning = true;
    }

    @Override
    public void run() {
        while (isRunning) {
            try {
                Object obj = ois.readObject();
                if (obj instanceof Message) {
                    Message msg = (Message) obj;
                    clientControl.handleServerMessage(msg);
                }
            } catch (Exception e) {
                if (isRunning) {
                    clientControl.handleDisconnect("Mất kết nối tới Server: " + e.getMessage());
                }
                break;
            }
        }
    }

    public void stopThread() {
        isRunning = false;
    }
}
