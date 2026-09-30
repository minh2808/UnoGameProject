package com.ptit.uno.client.control;

import com.ptit.uno.client.view.*;
import com.ptit.uno.client.view.fx.auth.LoginFXView;
import com.ptit.uno.client.view.fx.auth.RegisterFXView;
import com.ptit.uno.client.view.fx.game.GameBoardUIBridge;
import com.ptit.uno.client.view.fx.game.UnoGameFXView;
import com.ptit.uno.client.view.fx.lobby.LobbyFXView;
import com.ptit.uno.client.view.fx.room.RoomWaitingFXView;
import com.ptit.uno.model.*;
import com.ptit.uno.protocol.Message;
import com.ptit.uno.protocol.MessageType;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.List;
import java.util.Map;

/**
 * ClientControl: Trung tâm điều khiển toàn bộ logic và mạng phía Client.
 * Áp dụng triệt để Mô hình MVC Cải tiến (Slide b02-2 & b05):
 * - Giành quyền điều khiển toàn bộ các View.
 * - Khởi tạo các View, đăng ký bộ lắng nghe sự kiện (Inner Classes ActionListener).
 * - Quản lý kết nối Socket TCP, ObjectStream và luồng ngầm ClientReceiverThread.
 */
public class ClientControl {
    private Socket socket;
    private ObjectOutputStream oos;
    private ObjectInputStream ois;
    private ClientReceiverThread receiverThread;

    private User currentUser;
    private Room currentRoom;

    // Các View thuộc quyền quản lý của Controller
    private final LoginFrm loginFrm;
    private final RegisterFrm registerFrm;
    private final LobbyFrm lobbyFrm;
    private final RoomWaitingFrm roomWaitingFrm;
    private final UnoGameFXView gameView;
    private final com.ptit.uno.client.view.fx.game.SpectatorFXView spectatorView;
    private final LeaderboardFrm leaderboardFrm;
    private final MatchHistoryFrm matchHistoryFrm;

    public ClientControl(LoginFrm loginFrm) {
        this.loginFrm = loginFrm;
        this.registerFrm = (loginFrm instanceof LoginFXView) ? new RegisterFXView() : new RegisterFrm();
        this.lobbyFrm = (loginFrm instanceof LoginFXView) ? new LobbyFXView() : new LobbyFrm();
        this.roomWaitingFrm = (loginFrm instanceof LoginFXView) ? new RoomWaitingFXView() : new RoomWaitingFrm();
        this.gameView = new UnoGameFXView();
        this.spectatorView = new com.ptit.uno.client.view.fx.game.SpectatorFXView();
        this.leaderboardFrm = (loginFrm instanceof LoginFXView) ? new com.ptit.uno.client.view.fx.lobby.LeaderboardFXView() : new LeaderboardFrm();
        this.matchHistoryFrm = (loginFrm instanceof LoginFXView) ? new com.ptit.uno.client.view.fx.lobby.MatchHistoryFXView() : new MatchHistoryFrm();

        // Đăng ký toàn bộ các Listener từ Control vào View (Chuẩn Slide b02-2)
        initListeners();
    }

    private void initListeners() {
        // --- 1. Sự kiện trên LoginFrm ---
        loginFrm.addLoginListener(new LoginListener());
        loginFrm.addToRegisterListener(e -> {
            loginFrm.setVisible(false);
            registerFrm.setVisible(true);
        });

        // --- 2. Sự kiện trên RegisterFrm ---
        registerFrm.addRegisterListener(new RegisterListener());
        registerFrm.addBackListener(e -> {
            registerFrm.setVisible(false);
            loginFrm.setVisible(true);
        });

        // --- 3. Sự kiện trên LobbyFrm ---
        lobbyFrm.addCreateRoomListener(new CreateRoomListener());
        lobbyFrm.addJoinRoomListener(new JoinRoomListener());
        lobbyFrm.addRefreshListener(e -> {
            sendData(new Message(MessageType.GET_ROOMS_REQUEST));
            sendData(new Message(MessageType.GET_ONLINE_USERS_REQUEST));
        });
        lobbyFrm.addLeaderboardListener(e -> {
            sendData(new Message(MessageType.GET_LEADERBOARD_REQUEST));
            leaderboardFrm.setVisible(true);
        });
        lobbyFrm.addHistoryListener(e -> {
            sendData(new Message(MessageType.GET_MATCH_HISTORY_REQUEST));
            if (matchHistoryFrm instanceof com.ptit.uno.client.view.fx.lobby.MatchHistoryFXView) {
                ((com.ptit.uno.client.view.fx.lobby.MatchHistoryFXView) matchHistoryFrm).setCurrentUsername(currentUser != null ? currentUser.getUsername() : "");
            }
            matchHistoryFrm.setVisible(true);
        });
        lobbyFrm.addLogoutListener(e -> {
            closeConnection();
            lobbyFrm.setVisible(false);
            loginFrm.setVisible(true);
        });
        if (lobbyFrm instanceof LobbyFXView) {
            ((LobbyFXView) lobbyFrm).addSpectateRoomListener(new SpectateRoomListener());
        }

        // --- 4. Sự kiện trên RoomWaitingFrm ---
        roomWaitingFrm.addStartGameListener(new StartGameListener());
        roomWaitingFrm.addLeaveRoomListener(new LeaveRoomListener());
        roomWaitingFrm.addSendChatListener(new SendChatListener());

        // --- 5. Sự kiện trên GameBoard JavaFX ---
        gameView.setActionListener(new GameBoardUIBridge.GameActionListener() {
            @Override
            public void onPlayCard(Card card, CardColor chosenColor) {
                Object[] payload = new Object[]{card, chosenColor};
                sendData(new Message(MessageType.PLAY_CARD_REQUEST, payload));
            }

            @Override
            public void onDrawCard() {
                sendData(new Message(MessageType.DRAW_CARD_REQUEST));
            }

            @Override
            public void onCallUno() {
                sendData(new Message(MessageType.CALL_UNO_REQUEST));
            }

            @Override
            public void onSendMessage(String message) {
                if (message != null && !message.trim().isEmpty() && currentRoom != null && currentUser != null) {
                    ChatMessage chat = new ChatMessage(currentRoom.getId(), currentUser.getUsername(), message.trim());
                    sendData(new Message(MessageType.CHAT_MESSAGE, chat));
                }
            }

            @Override
            public void onLeaveGame() {
                int confirm = JOptionPane.showConfirmDialog(null,
                        "Bạn có chắc muốn thoát ván đấu? Bot sẽ đánh thay bạn!", "Xác nhận thoát", JOptionPane.YES_NO_OPTION);
                if (confirm == JOptionPane.YES_OPTION) {
                    sendData(new Message(MessageType.LEAVE_ROOM_REQUEST));
                    gameView.setVisible(false);
                    lobbyFrm.setVisible(true);
                }
            }

            @Override
            public void onGameFinished() {
                gameView.setVisible(false);
                roomWaitingFrm.setVisible(true);
            }
        });

        // --- 6. Sự kiện trên SpectatorView ---
        spectatorView.addLeaveRoomListener(new LeaveRoomListener());
        spectatorView.addSendChatListener(new SendChatListener());
    }

    // =========================================================================
    // CÁC INNER CLASSES XỬ LÝ SỰ KIỆN TỪ VIEW ĐẨY VỀ (ĐÚNG SLIDE 46 MÔN LTM)
    // =========================================================================

    class LoginListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            User user = loginFrm.getUser();
            String host = loginFrm.getHost();
            int port = loginFrm.getPort();

            if (user.getUsername().isEmpty() || user.getPassword().isEmpty()) {
                loginFrm.showMessage("Vui lòng nhập đầy đủ tài khoản và mật khẩu!");
                return;
            }

            new Thread(() -> {
                try {
                    if (socket == null || socket.isClosed()) {
                        boolean connected = openConnection(host, port);
                        if (!connected) {
                            loginFrm.showMessage("Không thể kết nối đến máy chủ: " + host + ":" + port);
                            return;
                        }
                    }
                    sendData(new Message(MessageType.LOGIN_REQUEST, user));
                } catch (Exception ex) {
                    loginFrm.showMessage("Lỗi: " + ex.getMessage());
                }
            }).start();
        }
    }

    class RegisterListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            User user = registerFrm.getUser();
            String host = loginFrm.getHost();
            int port = loginFrm.getPort();
            
            if (user == null) {
                return;
            }

            new Thread(() -> {
                try {
                    if (socket == null || socket.isClosed()) {
                        boolean connected = openConnection(host, port);
                        if (!connected) {
                            registerFrm.showMessage("Không thể kết nối đến máy chủ!");
                            return;
                        }
                    }
                    sendData(new Message(MessageType.REGISTER_REQUEST, user));
                } catch (Exception ex) {
                    registerFrm.showMessage("Lỗi: " + ex.getMessage());
                }
            }).start();
        }
    }

    class CreateRoomListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            String roomName = lobbyFrm.promptCreateRoomName();
            if (roomName != null && !roomName.trim().isEmpty()) {
                Room r = new Room(0, roomName.trim(), currentUser.getId(), currentUser.getUsername(), 4);
                sendData(new Message(MessageType.CREATE_ROOM_REQUEST, r));
            }
        }
    }

    class JoinRoomListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            Integer roomId = lobbyFrm.getSelectedRoomId();
            if (roomId == null) {
                lobbyFrm.showMessage("Vui lòng chọn 1 phòng trong danh sách!");
                return;
            }
            sendData(new Message(MessageType.JOIN_ROOM_REQUEST, roomId));
        }
    }

    class StartGameListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            sendData(new Message(MessageType.START_GAME_REQUEST));
        }
    }

    class LeaveRoomListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            sendData(new Message(MessageType.LEAVE_ROOM_REQUEST));
            roomWaitingFrm.setVisible(false);
            if (spectatorView != null) spectatorView.setVisible(false);
            lobbyFrm.setVisible(true);
            sendData(new Message(MessageType.GET_ROOMS_REQUEST));
        }
    }

    class SendChatListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            String content = roomWaitingFrm.getChatMessage();
            if (content.isEmpty() && spectatorView != null && spectatorView.isVisible()) {
                content = spectatorView.getChatMessage();
            }
            if (!content.isEmpty() && currentRoom != null) {
                ChatMessage chat = new ChatMessage(currentRoom.getId(), currentUser.getUsername(), content);
                sendData(new Message(MessageType.CHAT_MESSAGE, chat));
            }
        }
    }
    
    class SpectateRoomListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            Integer roomId = lobbyFrm.getSelectedRoomId();
            if (roomId == null) {
                lobbyFrm.showMessage("Vui lòng chọn 1 phòng trong danh sách!");
                return;
            }
            sendData(new Message(MessageType.SPECTATE_ROOM_REQUEST, roomId));
        }
    }

    // =========================================================================
    // XỬ LÝ GÓI TIN TỪ SERVER GỬI VỀ (ĐƯỢC GỌI TỪ CLIENT RECEIVER THREAD)
    // =========================================================================

    @SuppressWarnings("unchecked")
    public void handleServerMessage(Message msg) {
        SwingUtilities.invokeLater(() -> {
            try {
                switch (msg.getType()) {
                    case LOGIN_RESPONSE:
                        if (msg.isSuccess()) {
                            this.currentUser = (User) msg.getPayload();
                            loginFrm.setVisible(false);
                            lobbyFrm.setUserInfo(currentUser);
                            lobbyFrm.setVisible(true);
                            sendData(new Message(MessageType.GET_ROOMS_REQUEST));
                            sendData(new Message(MessageType.GET_ONLINE_USERS_REQUEST));
                        } else {
                            loginFrm.showMessage(msg.getMessage());
                        }
                        break;

                    case REGISTER_RESPONSE:
                        registerFrm.showMessage(msg.getMessage());
                        if (msg.isSuccess()) {
                            registerFrm.setVisible(false);
                            loginFrm.setVisible(true);
                        }
                        break;

                    case ROOM_LIST_RESPONSE:
                        List<Room> rooms = (List<Room>) msg.getPayload();
                        lobbyFrm.renderRooms(rooms);
                        break;

                    case ONLINE_USERS_RESPONSE:
                        List<User> users = (List<User>) msg.getPayload();
                        lobbyFrm.renderOnlineUsers(users);
                        break;

                    case CREATE_ROOM_RESPONSE:
                    case JOIN_ROOM_RESPONSE:
                        if (msg.isSuccess()) {
                            this.currentRoom = (Room) msg.getPayload();
                            lobbyFrm.setVisible(false);
                            roomWaitingFrm.renderRoom(currentRoom, currentUser.getId());
                            roomWaitingFrm.setVisible(true);
                        } else {
                            lobbyFrm.showMessage(msg.getMessage());
                        }
                        break;

                    case ROOM_UPDATE_BROADCAST:
                        this.currentRoom = (Room) msg.getPayload();
                        if (roomWaitingFrm.isVisible()) {
                            roomWaitingFrm.renderRoom(currentRoom, currentUser.getId());
                        }
                        break;

                    case GAME_STATE_BROADCAST:
                        GameState state = (GameState) msg.getPayload();
                        if (!gameView.isVisible()) {
                            roomWaitingFrm.setVisible(false);
                            if (currentUser != null) {
                                gameView.setPlayerUsername(currentUser.getUsername());
                            }
                            gameView.setVisible(true);
                        }
                        gameView.updateGameState(state, currentUser.getId());
                        break;

                    case PLAYER_HAND_UPDATE:
                        List<Card> newHand = (List<Card>) msg.getPayload();
                        gameView.renderHand(newHand);
                        break;

                    case CHAT_MESSAGE:
                        ChatMessage chat = (ChatMessage) msg.getPayload();
                        roomWaitingFrm.appendChatMessage(chat);
                        gameView.appendChatMessage(chat);
                        if (spectatorView != null) {
                            spectatorView.addChatMessage(chat.getSender(), chat.getContent(), chat.getSender().equals(currentUser.getUsername()));
                        }
                        break;

                    case SPECTATE_ROOM_RESPONSE:
                        if (msg.isSuccess()) {
                            this.currentRoom = (Room) msg.getPayload();
                            lobbyFrm.setVisible(false);
                            if (currentRoom.getPlayers() != null && currentRoom.getPlayers().size() >= 2) {
                                spectatorView.setPlayersInfo(
                                    currentRoom.getPlayers().get(0).getUsername(),
                                    currentRoom.getPlayers().get(1).getUsername()
                                );
                            }
                            spectatorView.setVisible(true);
                        } else {
                            lobbyFrm.showMessage(msg.getMessage());
                        }
                        break;

                    case SPECTATOR_STATE_BROADCAST:
                        SpectatorState specState = (SpectatorState) msg.getPayload();
                        spectatorView.updateGameState(specState.getGameState(), specState.getHostHand(), specState.getGuestHand());
                        spectatorView.updateCountdown(specState.getGameState().getRemainingSeconds());
                        break;

                    case LEADERBOARD_RESPONSE:
                        List<User> topUsers = (List<User>) msg.getPayload();
                        leaderboardFrm.renderLeaderboard(topUsers);
                        break;

                    case MATCH_HISTORY_RESPONSE:
                        List<Map<String, Object>> hist = (List<Map<String, Object>>) msg.getPayload();
                        matchHistoryFrm.renderHistory(hist);
                        break;

                    case ERROR_NOTIFICATION:
                        JOptionPane.showMessageDialog(null, msg.getMessage(), "Thông báo từ Server", JOptionPane.WARNING_MESSAGE);
                        break;

                    default:
                        break;
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        });
    }

    // =========================================================================
    // HẠ TẦNG KẾT NỐI MẠNG (SOCKET & OBJECT STREAM)
    // =========================================================================

    public boolean openConnection(String host, int port) {
        try {
            socket = new Socket(host, port);
            oos = new ObjectOutputStream(socket.getOutputStream());
            oos.flush();
            ois = new ObjectInputStream(socket.getInputStream());

            // Kích hoạt luồng chạy ngầm đọc Socket
            receiverThread = new ClientReceiverThread(ois, this);
            receiverThread.start();
            return true;
        } catch (IOException e) {
            System.err.println("[ClientControl] Lỗi kết nối Socket: " + e.getMessage());
            return false;
        }
    }

    public synchronized void sendData(Message msg) {
        try {
            if (oos != null && socket != null && !socket.isClosed()) {
                oos.reset();
                oos.writeObject(msg);
                oos.flush();
            }
        } catch (IOException e) {
            System.err.println("[ClientControl] Lỗi gửi dữ liệu: " + e.getMessage());
        }
    }

    public void closeConnection() {
        if (receiverThread != null) receiverThread.stopThread();
        try {
            if (socket != null && !socket.isClosed()) socket.close();
        } catch (IOException ignored) {}
    }

    public void handleDisconnect(String reason) {
        SwingUtilities.invokeLater(() -> {
            JOptionPane.showMessageDialog(null, reason, "Mất kết nối", JOptionPane.ERROR_MESSAGE);
            gameView.setVisible(false);
            roomWaitingFrm.setVisible(false);
            lobbyFrm.setVisible(false);
            loginFrm.setVisible(true);
        });
    }
}
