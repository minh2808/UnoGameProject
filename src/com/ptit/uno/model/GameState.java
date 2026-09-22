package com.ptit.uno.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * GameState chứa toàn bộ thông tin trạng thái bàn cờ đồng bộ theo thời gian thực giữa Server và Client.
 */
public class GameState implements Serializable {
    private static final long serialVersionUID = 1L;

    private int roomId;
    private Card topCard;             // Lá bài ngửa trên bàn (Discard pile)
    private CardColor activeColor;    // Màu sắc đang có hiệu lực (đặc biệt khi đổi màu Wild)
    private int currentTurnSeat;      // Ghế của người đang tới lượt đi (0, 1, 2, 3)
    private int direction;            // 1: Xuôi chiều kim đồng hồ, -1: Ngược chiều
    private int remainingSeconds;     // Đếm ngược 15 giây
    private int drawPileCount;        // Số lượng lá bài còn trong nọc rút
    private int drawPenaltyCount;     // Số lá bài phạt tích lũy (+2, +4)
    private boolean isGameOver;
    private String winnerUsername;
    private List<PlayerSummary> playerSummaries; // Tóm tắt bài của đối thủ (để không lộ bài trên tay)
    private String lastActionLog;

    public GameState() {
        this.direction = 1;
        this.remainingSeconds = 15;
        this.isGameOver = false;
        this.playerSummaries = new ArrayList<PlayerSummary>();
    }

    public int getRoomId() {
        return roomId;
    }

    public void setRoomId(int roomId) {
        this.roomId = roomId;
    }

    public Card getTopCard() {
        return topCard;
    }

    public void setTopCard(Card topCard) {
        this.topCard = topCard;
    }

    public CardColor getActiveColor() {
        return activeColor;
    }

    public void setActiveColor(CardColor activeColor) {
        this.activeColor = activeColor;
    }

    public int getCurrentTurnSeat() {
        return currentTurnSeat;
    }

    public void setCurrentTurnSeat(int currentTurnSeat) {
        this.currentTurnSeat = currentTurnSeat;
    }

    public int getDirection() {
        return direction;
    }

    public void setDirection(int direction) {
        this.direction = direction;
    }

    public int getRemainingSeconds() {
        return remainingSeconds;
    }

    public void setRemainingSeconds(int remainingSeconds) {
        this.remainingSeconds = remainingSeconds;
    }

    public int getDrawPileCount() {
        return drawPileCount;
    }

    public void setDrawPileCount(int drawPileCount) {
        this.drawPileCount = drawPileCount;
    }

    public int getDrawPenaltyCount() {
        return drawPenaltyCount;
    }

    public void setDrawPenaltyCount(int drawPenaltyCount) {
        this.drawPenaltyCount = drawPenaltyCount;
    }

    public boolean isGameOver() {
        return isGameOver;
    }

    public void setGameOver(boolean gameOver) {
        isGameOver = gameOver;
    }

    public String getWinnerUsername() {
        return winnerUsername;
    }

    public void setWinnerUsername(String winnerUsername) {
        this.winnerUsername = winnerUsername;
    }

    public List<PlayerSummary> getPlayerSummaries() {
        return playerSummaries;
    }

    public void setPlayerSummaries(List<PlayerSummary> playerSummaries) {
        this.playerSummaries = playerSummaries;
    }

    public String getLastActionLog() {
        return lastActionLog;
    }

    public void setLastActionLog(String lastActionLog) {
        this.lastActionLog = lastActionLog;
    }

    /**
     * DTO đại diện thông tin hiển thị của một người chơi trên bàn (chỉ gửi số lượng bài, không gửi chi tiết lá bài).
     */
    public static class PlayerSummary implements Serializable {
        private static final long serialVersionUID = 1L;

        private int userId;
        private String username;
        private int seatNumber;
        private int cardCount;
        private boolean isUno;
        private boolean isBot;

        public PlayerSummary() {}

        public PlayerSummary(int userId, String username, int seatNumber, int cardCount, boolean isUno, boolean isBot) {
            this.userId = userId;
            this.username = username;
            this.seatNumber = seatNumber;
            this.cardCount = cardCount;
            this.isUno = isUno;
            this.isBot = isBot;
        }

        public int getUserId() {
            return userId;
        }

        public String getUsername() {
            return username;
        }

        public int getSeatNumber() {
            return seatNumber;
        }

        public int getCardCount() {
            return cardCount;
        }

        public boolean isUno() {
            return isUno;
        }

        public boolean isBot() {
            return isBot;
        }
    }
}
