package com.ptit.uno.server.control;

import com.ptit.uno.model.*;
import com.ptit.uno.protocol.Message;
import com.ptit.uno.protocol.MessageType;
import com.ptit.uno.server.dao.DAO;
import com.ptit.uno.server.view.ServerView;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * ServerControl: Trung tâm điều phối hệ thống mạng của Server.
 * Quản lý ServerSocket, vòng lặp accept() đón client, điều hướng phòng chơi và kết nối CSDL.
 * Bám sát Slide b05 (TS. Nguyễn Mạnh Hùng).
 */
public class ServerControl {
    private final ServerView view;
    private ServerSocket serverSocket;
    private final int serverPort;
    private boolean isRunning;

    private final List<ClientHandler> activeClients;
    private final RoomManager roomManager;

    public ServerControl(ServerView view, int port) {
        this.view = view;
        this.serverPort = port;
        this.activeClients = new CopyOnWriteArrayList<ClientHandler>();
        this.roomManager = new RoomManager();

        // Khởi tạo kết nối CSDL (Slide b03)
        DAO.getConnection();

        // Mở ServerSocket và lắng nghe (Slide b05)
        openServer(serverPort);
    }

    private void openServer(int port) {
        try {
            serverSocket = new ServerSocket(port);
            isRunning = true;
            view.showMessage("Đã mở ServerSocket thành công tại cổng: " + port);

            // Chạy luồng lắng nghe accept() không chặn luồng giao diện
            new Thread(this::listenConnections).start();
        } catch (IOException e) {
            view.showMessage("LỖI không thể mở cổng " + port + ": " + e.getMessage());
        }
    }

    private void listenConnections() {
        while (isRunning) {
            try {
                Socket clientSocket = serverSocket.accept();
                ClientHandler handler = new ClientHandler(clientSocket, this);
                handler.start();
            } catch (IOException e) {
                if (!isRunning) break;
                view.showMessage("Lỗi khi chấp nhận kết nối: " + e.getMessage());
            }
        }
    }

    public synchronized void addOnlineUser(ClientHandler handler) {
        activeClients.add(handler);
        view.updateClientCount(activeClients.size());
    }

    public synchronized void removeOnlineUser(ClientHandler handler) {
        activeClients.remove(handler);
        view.updateClientCount(activeClients.size());
    }

    public List<User> getOnlineUsers() {
        List<User> list = new ArrayList<User>();
        for (ClientHandler ch : activeClients) {
            if (ch.getCurrentUser() != null) {
                list.add(ch.getCurrentUser());
            }
        }
        return list;
    }

    public ClientHandler getClientByUsername(String username) {
        if (username == null) return null;
        for (ClientHandler ch : activeClients) {
            if (ch.getCurrentUser() != null && username.equalsIgnoreCase(ch.getCurrentUser().getUsername())) {
                return ch;
            }
        }
        return null;
    }

    public void broadcastOnlineUsers() {
        List<User> list = getOnlineUsers();
        Message msg = new Message(MessageType.ONLINE_USERS_RESPONSE, list);
        for (ClientHandler ch : activeClients) {
            ch.sendMessage(msg);
        }
    }

    public void broadcastRoomList() {
        List<Room> rooms = roomManager.getAllRooms();
        view.updateRoomCount(rooms.size());
        Message msg = new Message(MessageType.ROOM_LIST_RESPONSE, rooms);
        for (ClientHandler ch : activeClients) {
            ch.sendMessage(msg);
        }
    }

    public void broadcastToRoom(int roomId, Message message) {
        for (ClientHandler ch : activeClients) {
            if (ch.getCurrentRoomId() == roomId) {
                ch.sendMessage(message);
            }
        }
    }

    public void broadcastToRoom(int roomId, GameState state) {
        Message msg = new Message(MessageType.GAME_STATE_BROADCAST, state);
        
        GameManager gm = roomManager.getGameManager(roomId);
        Room room = roomManager.getRoom(roomId);
        Message specMsg = null;
        if (gm != null && room != null && room.getPlayers().size() >= 2) {
            List<Card> hostHand = room.getPlayers().get(0).getHand();
            List<Card> guestHand = room.getPlayers().get(1).getHand();
            SpectatorState specState = new SpectatorState(state, hostHand, guestHand);
            specMsg = new Message(MessageType.SPECTATOR_STATE_BROADCAST, specState);
        }

        for (ClientHandler ch : activeClients) {
            if (ch.getCurrentRoomId() == roomId) {
                if (ch.isSpectator() && specMsg != null) {
                    ch.sendMessage(specMsg);
                } else if (!ch.isSpectator()) {
                    ch.sendMessage(msg);
                }
            }
        }
    }

    public void sendHandUpdate(int userId, List<Card> hand) {
        for (ClientHandler ch : activeClients) {
            if (ch.getCurrentUser() != null && ch.getCurrentUser().getId() == userId) {
                ch.sendMessage(new Message(MessageType.PLAYER_HAND_UPDATE, hand));
                break;
            }
        }
    }

    public void stopServer() {
        isRunning = false;
        try {
            for (ClientHandler ch : activeClients) {
                ch.close();
            }
            if (serverSocket != null) serverSocket.close();
            DAO.closeConnection();
        } catch (IOException ignored) {}
    }

    public ServerView getView() {
        return view;
    }

    public RoomManager getRoomManager() {
        return roomManager;
    }

    public void updateRoomUsersStatus(int roomId, String status) {
        for (ClientHandler ch : activeClients) {
            if (ch.getCurrentRoomId() == roomId && ch.getCurrentUser() != null) {
                if (!ch.isSpectator()) {
                    ch.getCurrentUser().setStatus(status);
                } else {
                    ch.getCurrentUser().setStatus("Đang xem");
                }
            }
        }
        broadcastOnlineUsers();
    }
}
