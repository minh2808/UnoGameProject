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
 */
public class ServerControl {
    private final ServerView view;
    private ServerSocket serverSocket;
    private final int serverPort;
    private boolean isRunning;

    private final List<ClientHandler> activeClients;
    private final RoomManager roomManager;

    // Hàm khởi tạo bộ điều khiển Server: nạp cấu hình cổng, danh sách client, RoomManager, kết nối CSDL và mở ServerSocket
    public ServerControl(ServerView view, int port) {
        this.view = view;
        this.serverPort = port;
        this.activeClients = new CopyOnWriteArrayList<ClientHandler>(); // danh sách các CLient đang hoạt động
        this.roomManager = new RoomManager(); // khởi tạo đối tượng để quản lý các phòng trong ứng dụng

        // Khởi tạo kết nối CSDL
        DAO.getConnection();

        // Mở ServerSocket và lắng nghe
        openServer(serverPort);
    }

    // Mở ServerSocket tại cổng chỉ định và tách riêng một Thread lắng nghe để không làm đơ giao diện Server
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

    // Vòng lặp liên tục chờ kết nối từ Client qua accept() và sinh một ClientHandler riêng (Mô hình Thread-per-Client)
    private void listenConnections() {
        while (isRunning) {
            try {
                Socket clientSocket = serverSocket.accept();
                ClientHandler handler = new ClientHandler(clientSocket, this);
                handler.start(); // Kích hoạt phương thức run của ClientHandler
            } catch (IOException e) {
                if (!isRunning) break;
                view.showMessage("Lỗi khi chấp nhận kết nối: " + e.getMessage());
            }
        }
    }

    // Thêm Client vào danh sách hoạt động, kiểm tra chặn đăng nhập trùng tài khoản (Duplicate Login) và cập nhật số lượng
    public synchronized boolean addOnlineUser(ClientHandler handler) {
        if (handler.getCurrentUser() != null) {
            String username = handler.getCurrentUser().getUsername();
            for (ClientHandler ch : activeClients) {
                if (ch != handler && ch.getCurrentUser() != null
                        && ch.getCurrentUser().getUsername().equalsIgnoreCase(username)) {
                    return false;
                }
            }
        }
        if (!activeClients.contains(handler)) {
            activeClients.add(handler);
        }
        view.updateClientCount(activeClients.size());
        return true;
    }

    // Xóa Client khỏi danh sách kết nối khi đăng xuất hoặc mất mạng, đồng thời cập nhật lại số lượng trên giao diện Server
    public synchronized void removeOnlineUser(ClientHandler handler) {
        activeClients.remove(handler);
        view.updateClientCount(activeClients.size());
    }

    // Lấy danh sách toàn bộ các luồng ClientHandler hiện đang kết nối với Server
    public List<ClientHandler> getActiveClients() {
        return activeClients;
    }

    // Lọc và lấy danh sách các đối tượng User đang online thực tế trong hệ thống
    public List<User> getOnlineUsers() {
        List<User> list = new ArrayList<User>();
        for (ClientHandler ch : activeClients) {
            if (ch.getCurrentUser() != null) {
                list.add(ch.getCurrentUser());
            }
        }
        return list;
    }

    // Tìm kiếm luồng ClientHandler tương ứng theo tên đăng nhập (username)
    public ClientHandler getClientByUsername(String username) {
        if (username == null) return null;
        for (ClientHandler ch : activeClients) {
            if (ch.getCurrentUser() != null && username.equalsIgnoreCase(ch.getCurrentUser().getUsername())) {
                return ch;
            }
        }
        return null;
    }

    // Tìm kiếm luồng ClientHandler tương ứng theo ID của người dùng (userId)
    public ClientHandler getClientById(int userId) {
        for (ClientHandler ch : activeClients) {
            if (ch.getCurrentUser() != null && ch.getCurrentUser().getId() == userId) {
                return ch;
            }
        }
        return null;
    }

    // Broadcast danh sách người dùng online tới tất cả Client để cập nhật giao diện Sảnh chính (Lobby)
    public void broadcastOnlineUsers() {
        List<User> list = getOnlineUsers();
        Message msg = new Message(MessageType.ONLINE_USERS_RESPONSE, list);
        for (ClientHandler ch : activeClients) {
            ch.sendMessage(msg);
        }
    }

    // Phát thanh (Broadcast) danh sách tất cả các phòng chơi tới toàn bộ Client ở sảnh
    public void broadcastRoomList() {
        List<Room> rooms = roomManager.getAllRooms();
        view.updateRoomCount(rooms.size());
        Message msg = new Message(MessageType.ROOM_LIST_RESPONSE, rooms);
        for (ClientHandler ch : activeClients) {
            ch.sendMessage(msg);
        }
    }

    // Gửi một gói tin (Message) tới tất cả người chơi và khán giả đang ở trong một phòng cụ thể (Multicast/Room Broadcast)
    public void broadcastToRoom(int roomId, Message message) {
        for (ClientHandler ch : activeClients) {
            if (ch.getCurrentRoomId() == roomId) {
                ch.sendMessage(message);
            }
        }
    }

    // Đồng bộ trạng thái bàn cờ (GameState) tới phòng chơi, phân biệt gói tin cho người chơi và gói tin cho khán giả (SpectatorState)
    public void broadcastToRoom(int roomId, GameState state) {
        if (state == null) {
            return;
        }
        Message msg = new Message(MessageType.GAME_STATE_BROADCAST, state);

        Room room = roomManager.getRoom(roomId);
        Message specMsg = null;
        if (room != null && !room.getPlayers().isEmpty()) {
            List<Card> hostHand = new ArrayList<Card>(room.getPlayers().get(0).getHand());
            List<Card> guestHand = room.getPlayers().size() >= 2
                    ? new ArrayList<Card>(room.getPlayers().get(1).getHand())
                    : new ArrayList<Card>();
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

    // Gửi riêng danh sách bài trên tay mới (bí mật) cho đúng một người chơi cụ thể (Bảo mật thông tin lá bài, chống hack)
    public void sendHandUpdate(int userId, List<Card> hand) {
        for (ClientHandler ch : activeClients) {
            if (ch.getCurrentUser() != null && ch.getCurrentUser().getId() == userId) {
                ch.sendMessage(new Message(MessageType.PLAYER_HAND_UPDATE, hand));
                break;
            }
        }
    }

    // Dừng hoạt động Server, đóng tất cả kết nối Socket của các Client và đóng ServerSocket an toàn (Graceful Shutdown)
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

    // Lấy đối tượng giao diện ServerView (Dashboard) để ghi nhật ký log hoặc cập nhật thống kê
    public ServerView getView() {
        return view;
    }

    // Lấy đối tượng quản lý danh sách phòng chơi (RoomManager)
    public RoomManager getRoomManager() {
        return roomManager;
    }

    // Cập nhật trạng thái hoạt động ("Đang chơi", "Đang xem", "Rảnh") cho người dùng trong phòng và phát thanh lại toàn server
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
