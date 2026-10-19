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

    public synchronized boolean isRoomNameExists(String roomName) {
        if (roomName == null) {
            return false;
        }
        String normalized = roomName.trim();
        for (Room r : rooms.values()) {
            if (!Room.STATUS_FINISHED.equals(r.getStatus())
                    && r.getName() != null
                    && r.getName().trim().equalsIgnoreCase(normalized)) {
                return true;
            }
        }
        return false;
    }

    public synchronized Room createRoom(String roomName, int hostId, String hostName, int maxPlayers) {
        if (roomName == null || roomName.trim().isEmpty() || isRoomNameExists(roomName)) {
            return null;
        }
        String normalizedName = roomName.trim();
        int id = roomIdCounter++;
        int actualMax = (maxPlayers > 0) ? Math.min(maxPlayers, 2) : 2;
        Room r = new Room(id, normalizedName, hostId, hostName, actualMax);
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
                // Đang trong trận: người chơi rời bàn / mất kết nối -> người còn lại thắng luôn
                gm.handlePlayerDisconnect(userId);
                rooms.remove(roomId);
                gameManagers.remove(roomId);
            } else if (Room.STATUS_FINISHED.equals(r.getStatus())) {
                // Trận đấu đã kết thúc: khi 1 trong 2 người rời đi thì giải tán phòng luôn
                r.removePlayer(userId);
                if (gm != null) {
                    gm.stopGame();
                }
                rooms.remove(roomId);
                gameManagers.remove(roomId);
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
        List<Room> activeRooms = new ArrayList<Room>();
        for (Room r : rooms.values()) {
            if (!Room.STATUS_FINISHED.equals(r.getStatus())) {
                activeRooms.add(r);
            }
        }
        return activeRooms;
    }

    public int getRoomCount() {
        return getAllRooms().size();
    }

    public void setGameManager(int roomId, GameManager gm) {
        gameManagers.put(roomId, gm);
    }

    public GameManager getGameManager(int roomId) {
        return gameManagers.get(roomId);
    }

    public void removeGameManager(int roomId) {
        GameManager gm = gameManagers.remove(roomId);
        if (gm != null) {
            gm.stopGame();
        }
    }

    public void removeRoom(int roomId) {
        removeGameManager(roomId);
        rooms.remove(roomId);
    }
}
