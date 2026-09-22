package com.ptit.uno.server.view;

/**
 * Interface ServerView định nghĩa các phương thức hiển thị thông báo của Server.
 * Bám sát Slide b05 (TS. Nguyễn Mạnh Hùng).
 */
public interface ServerView {
    void showMessage(String msg);
    void updateClientCount(int count);
    void updateRoomCount(int count);
}
