package com.ptit.uno.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Đại diện cho người chơi ngồi tại 1 ghế trong ván đấu UNO.
 */
public class Player implements Serializable {
    private static final long serialVersionUID = 1L;

    private int userId;
    private String username;
    private int seatNumber; // 0, 1, 2, 3
    private List<Card> hand;
    private boolean isUno;
    private boolean isBot;
    private boolean isReady;

    public Player() {
        this.hand = new ArrayList<Card>();
        this.isUno = false;
        this.isBot = false;
        this.isReady = false;
    }

    public Player(int userId, String username, int seatNumber) {
        this();
        this.userId = userId;
        this.username = username;
        this.seatNumber = seatNumber;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public int getSeatNumber() {
        return seatNumber;
    }

    public void setSeatNumber(int seatNumber) {
        this.seatNumber = seatNumber;
    }

    public List<Card> getHand() {
        return hand;
    }

    public void setHand(List<Card> hand) {
        this.hand = hand;
    }

    public boolean isUno() {
        return isUno;
    }

    public void setUno(boolean uno) {
        isUno = uno;
    }

    public boolean isBot() {
        return isBot;
    }

    public void setBot(boolean bot) {
        isBot = bot;
    }

    public boolean isReady() {
        return isReady;
    }

    public void setReady(boolean ready) {
        isReady = ready;
    }

    public int getCardCount() {
        return hand != null ? hand.size() : 0;
    }

    public void addCard(Card card) {
        if (this.hand == null) {
            this.hand = new ArrayList<Card>();
        }
        this.hand.add(card);
    }

    public boolean removeCard(Card card) {
        if (this.hand != null) {
            return this.hand.remove(card);
        }
        return false;
    }

    @Override
    public String toString() {
        return "Player{" +
                "userId=" + userId +
                ", username='" + username + '\'' +
                ", seatNumber=" + seatNumber +
                ", cardCount=" + getCardCount() +
                ", isBot=" + isBot +
                '}';
    }
}
