package com.ptit.uno.server.control;

import com.ptit.uno.model.Player;
import com.ptit.uno.model.Room;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Quản lý danh sách phòng chờ và các ván chơi đang diễn ra trên Server.
 */
public class RoomManager {
    private final Map<Integer, Room> rooms;
    private final Map<Integer, GameManager> gameManagers;
    private int roomIdCounter;

    public RoomManager() {
        this.rooms = new ConcurrentHashMap<Integer, Room>();
        this.gameManagers = new ConcurrentHashMap<Integer, GameManager>();
        this.roomIdCounter = 1;
    }

    public synchronized Room createRoom(String roomName, int hostId, String hostName, int maxPlayers) {
        int id = roomIdCounter++;
        Room r = new Room(id, roomName, hostId, hostName, maxPlayers);
        Player hostPlayer = new Player(hostId, hostName, 0);
        hostPlayer.setReady(true);
        r.addPlayer(hostPlayer);
        rooms.put(id, r);
        return r;
    }

    public synchronized boolean joinRoom(int roomId, int userId, String username) {
        Room r = rooms.get(roomId);
        if (r != null && !r.isFull() && Room.STATUS_WAITING.equals(r.getStatus())) {
            Player p = new Player(userId, username, r.getCurrentPlayerCount());
            return r.addPlayer(p);
        }
        return false;
    }

    public synchronized void leaveRoom(int roomId, int userId) {
        Room r = rooms.get(roomId);
        if (r != null) {
            GameManager gm = gameManagers.get(roomId);
            if (gm != null && Room.STATUS_PLAYING.equals(r.getStatus())) {
                // Đang trong trận: người chơi rời bàn sẽ bị chuyển thành Bot
                gm.handlePlayerDisconnect(userId);
            } else {
                r.removePlayer(userId);
                if (r.getCurrentPlayerCount() == 0) {
                    rooms.remove(roomId);
                    gameManagers.remove(roomId);
                } else if (r.getHostId() == userId) {
                    // Chuyển quyền chủ phòng cho người tiếp theo
                    Player newHost = r.getPlayers().get(0);
                    r.setHostId(newHost.getUserId());
                    r.setHostName(newHost.getUsername());
                }
            }
        }
    }

    public Room getRoom(int roomId) {
        return rooms.get(roomId);
    }

    public List<Room> getAllRooms() {
        return new ArrayList<Room>(rooms.values());
    }

    public int getRoomCount() {
        return rooms.size();
    }

    public void setGameManager(int roomId, GameManager gm) {
        gameManagers.put(roomId, gm);
    }

    public GameManager getGameManager(int roomId) {
        return gameManagers.get(roomId);
    }
}
