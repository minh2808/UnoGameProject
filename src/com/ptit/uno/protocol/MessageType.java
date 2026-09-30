package com.ptit.uno.protocol;

import java.io.Serializable;

/**
 * Danh mục các mã lệnh giao tiếp giữa Client và Server qua Socket TCP.
 */
public enum MessageType implements Serializable {
    // --- Xác thực tài khoản (Auth) ---
    LOGIN_REQUEST,
    LOGIN_RESPONSE,
    REGISTER_REQUEST,
    REGISTER_RESPONSE,
    LOGOUT_REQUEST,

    // --- Sảnh chờ & Quản lý người chơi (Lobby) ---
    GET_ONLINE_USERS_REQUEST,
    ONLINE_USERS_RESPONSE,
    GET_ROOMS_REQUEST,
    ROOM_LIST_RESPONSE,
    CREATE_ROOM_REQUEST,
    CREATE_ROOM_RESPONSE,
    JOIN_ROOM_REQUEST,
    JOIN_ROOM_RESPONSE,
    LEAVE_ROOM_REQUEST,
    ROOM_UPDATE_BROADCAST,
    
    SPECTATE_ROOM_REQUEST,
    SPECTATE_ROOM_RESPONSE,
    SPECTATOR_STATE_BROADCAST,

    // --- Trong phòng chờ (Room Waiting) ---
    PLAYER_READY_REQUEST,
    START_GAME_REQUEST,

    // --- Trong ván đấu UNO (In-Game) ---
    GAME_START_INIT,           // Gửi trạng thái ván đấu ban đầu + 7 lá bài
    PLAY_CARD_REQUEST,         // Người chơi yêu cầu đánh 1 lá bài
    DRAW_CARD_REQUEST,         // Người chơi yêu cầu bốc 1 lá bài
    CALL_UNO_REQUEST,          // Người chơi bấm hô UNO
    GAME_STATE_BROADCAST,      // Server gửi cập nhật bàn cờ tới tất cả người chơi
    PLAYER_HAND_UPDATE,        // Server gửi riêng danh sách bài trên tay mới cho người vừa đánh/bốc
    TURN_TIMEOUT_BROADCAST,    // Hết 15 giây, server tự động xử lý
    GAME_OVER_BROADCAST,       // Kết thúc ván đấu

    // --- Bảng xếp hạng & Lịch sử ---
    GET_LEADERBOARD_REQUEST,
    LEADERBOARD_RESPONSE,
    GET_MATCH_HISTORY_REQUEST,
    MATCH_HISTORY_RESPONSE,

    // --- Giao tiếp & Trò chuyện ---
    CHAT_MESSAGE,
    ERROR_NOTIFICATION
}
