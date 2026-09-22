package com.ptit.uno.model;

import java.io.Serializable;
import java.sql.Timestamp;

/**
 * Lớp thực thể User lưu thông tin người chơi.
 * Bám sát Slide b02-2 và b05 (JavaBean thuần implements Serializable).
 */
public class User implements Serializable {
    private static final long serialVersionUID = 1L;

    private int id;
    private String username;
    private String password;
    private int score;
    private int winMatches;
    private int totalMatches;
    private String status; // ONLINE, IN_ROOM, PLAYING, OFFLINE
    private Timestamp createdAt;

    public User() {
        this.score = 1000;
        this.status = "ONLINE";
    }

    public User(String username, String password) {
        this();
        this.username = username;
        this.password = password;
    }

    public User(int id, String username, String password, int score, int winMatches, int totalMatches) {
        this.id = id;
        this.username = username;
        this.password = password;
        this.score = score;
        this.winMatches = winMatches;
        this.totalMatches = totalMatches;
        this.status = "ONLINE";
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public int getScore() {
        return score;
    }

    public void setScore(int score) {
        this.score = score;
    }

    public int getWinMatches() {
        return winMatches;
    }

    public void setWinMatches(int winMatches) {
        this.winMatches = winMatches;
    }

    public int getTotalMatches() {
        return totalMatches;
    }

    public void setTotalMatches(int totalMatches) {
        this.totalMatches = totalMatches;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public String toString() {
        return "User{" +
                "id=" + id +
                ", username='" + username + '\'' +
                ", score=" + score +
                ", winMatches=" + winMatches +
                ", totalMatches=" + totalMatches +
                ", status='" + status + '\'' +
                '}';
    }
}
