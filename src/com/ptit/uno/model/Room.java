package com.ptit.uno.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Thực thể Room lưu trữ thông tin phòng chờ và bàn chơi.
 */
public class Room implements Serializable {
    private static final long serialVersionUID = 1L;

    public static final String STATUS_WAITING = "WAITING";
    public static final String STATUS_PLAYING = "PLAYING";

    private int id;
    private String name;
    private int hostId;
    private String hostName;
    private int maxPlayers;
    private String status;
    private List<Player> players;

    public Room() {
        this.maxPlayers = 4;
        this.status = STATUS_WAITING;
        this.players = new ArrayList<Player>();
    }

    public Room(int id, String name, int hostId, String hostName, int maxPlayers) {
        this();
        this.id = id;
        this.name = name;
        this.hostId = hostId;
        this.hostName = hostName;
        this.maxPlayers = maxPlayers;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getHostId() {
        return hostId;
    }

    public void setHostId(int hostId) {
        this.hostId = hostId;
    }

    public String getHostName() {
        return hostName;
    }

    public void setHostName(String hostName) {
        this.hostName = hostName;
    }

    public int getMaxPlayers() {
        return maxPlayers;
    }

    public void setMaxPlayers(int maxPlayers) {
        this.maxPlayers = maxPlayers;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public List<Player> getPlayers() {
        return players;
    }

    public void setPlayers(List<Player> players) {
        this.players = players;
    }

    public int getCurrentPlayerCount() {
        return players != null ? players.size() : 0;
    }

    public boolean isFull() {
        return getCurrentPlayerCount() >= maxPlayers;
    }

    public boolean addPlayer(Player player) {
        if (!isFull()) {
            player.setSeatNumber(players.size());
            players.add(player);
            return true;
        }
        return false;
    }

    public boolean removePlayer(int userId) {
        if (players != null) {
            for (int i = 0; i < players.size(); i++) {
                if (players.get(i).getUserId() == userId) {
                    players.remove(i);
                    // Cập nhật lại số ghế
                    for (int j = 0; j < players.size(); j++) {
                        players.get(j).setSeatNumber(j);
                    }
                    return true;
                }
            }
        }
        return false;
    }

    public Player getPlayer(int userId) {
        if (players != null) {
            for (Player p : players) {
                if (p.getUserId() == userId) {
                    return p;
                }
            }
        }
        return null;
    }

    public Player getPlayerBySeat(int seatNumber) {
        if (players != null && seatNumber >= 0 && seatNumber < players.size()) {
            return players.get(seatNumber);
        }
        return null;
    }

    @Override
    public String toString() {
        return "Room #" + id + " - " + name + " (" + getCurrentPlayerCount() + "/" + maxPlayers + ") [" + status + "]";
    }
}
