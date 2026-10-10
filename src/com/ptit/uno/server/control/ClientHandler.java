package com.ptit.uno.server.control;

import com.ptit.uno.model.*;
import com.ptit.uno.protocol.Message;
import com.ptit.uno.protocol.MessageType;
import com.ptit.uno.server.dao.MatchDAO;
import com.ptit.uno.server.dao.UserDAO;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.List;
import java.util.Map;

/**
 * ClientHandler: Luồng phục vụ riêng cho mỗi kết nối Client từ máy trạm.
 * Bám sát Slide b02-1 (Multi-thread) và Slide b05 (Socket TCP với ObjectStream).
 */
public class ClientHandler extends Thread {
    private final Socket socket;
    private final ServerControl serverControl;
    private ObjectInputStream ois;
    private ObjectOutputStream oos;
    private User currentUser;
    private int currentRoomId = -1;
    private boolean isSpectator = false;
    private boolean isRunning = true;

    private final UserDAO userDAO;
    private final MatchDAO matchDAO;

    public ClientHandler(Socket socket, ServerControl serverControl) {
        this.socket = socket;
        this.serverControl = serverControl;
        this.userDAO = new UserDAO();
        this.matchDAO = new MatchDAO();
    }

    @Override
    public void run() {
        try {
            // Thứ tự mở luồng ObjectStream chuẩn: OOS trước, OIS sau (chống deadlock)
            oos = new ObjectOutputStream(socket.getOutputStream());
            oos.flush();
            ois = new ObjectInputStream(socket.getInputStream());

            serverControl.getView().showMessage("Client đã kết nối từ: " + socket.getRemoteSocketAddress());

            while (isRunning) {
                Object obj = ois.readObject();
                if (obj instanceof Message) {
                    Message req = (Message) obj;
                    processMessage(req);
                }
            }
        } catch (Exception e) {
            serverControl.getView().showMessage("Client ngắt kết nối: " + (currentUser != null ? currentUser.getUsername() : socket.getRemoteSocketAddress()));
        } finally {
            close();
        }
    }

    private void processMessage(Message req) {
        try {
            switch (req.getType()) {
                case LOGIN_REQUEST: {
                    User u = (User) req.getPayload();
                    User loggedIn = userDAO.checkLogin(u.getUsername(), u.getPassword());
                    if (loggedIn != null) {
                        this.currentUser = loggedIn;
                        serverControl.addOnlineUser(this);
                        sendMessage(new Message(MessageType.LOGIN_RESPONSE, loggedIn, true, "Đăng nhập thành công!"));
                        serverControl.getView().showMessage("User đăng nhập: " + loggedIn.getUsername());
                        serverControl.broadcastOnlineUsers();
                    } else {
                        sendMessage(new Message(MessageType.LOGIN_RESPONSE, null, false, "Sai tên tài khoản hoặc mật khẩu!"));
                    }
                    break;
                }
                case REGISTER_REQUEST: {
                    User regUser = (User) req.getPayload();
                    boolean ok = userDAO.register(regUser);
                    if (ok) {
                        sendMessage(new Message(MessageType.REGISTER_RESPONSE, null, true, "Đăng ký thành công! Hãy đăng nhập."));
                    } else {
                        sendMessage(new Message(MessageType.REGISTER_RESPONSE, null, false, "Tài khoản đã tồn tại!"));
                    }
                    break;
                }
                case GET_ONLINE_USERS_REQUEST: {
                    List<User> list = serverControl.getOnlineUsers();
                    sendMessage(new Message(MessageType.ONLINE_USERS_RESPONSE, list));
                    break;
                }
                case GET_ROOMS_REQUEST: {
                    List<Room> rooms = serverControl.getRoomManager().getAllRooms();
                    sendMessage(new Message(MessageType.ROOM_LIST_RESPONSE, rooms));
                    break;
                }
                case CREATE_ROOM_REQUEST: {
                    Room r = (Room) req.getPayload();
                    Room created = serverControl.getRoomManager().createRoom(
                            r.getName(), currentUser.getId(), currentUser.getUsername(), r.getMaxPlayers()
                    );
                    this.currentRoomId = created.getId();
                    currentUser.setStatus("Đang trong phòng");
                    sendMessage(new Message(MessageType.CREATE_ROOM_RESPONSE, created, true, "Tạo phòng thành công!"));
                    serverControl.broadcastRoomList();
                    serverControl.broadcastOnlineUsers();
                    break;
                }
                case JOIN_ROOM_REQUEST: {
                    int roomId = (Integer) req.getPayload();
                    boolean joined = serverControl.getRoomManager().joinRoom(roomId, currentUser.getId(), currentUser.getUsername());
                    if (joined) {
                        this.currentRoomId = roomId;
                        currentUser.setStatus("Đang trong phòng");
                        Room current = serverControl.getRoomManager().getRoom(roomId);
                        sendMessage(new Message(MessageType.JOIN_ROOM_RESPONSE, current, true, "Vào phòng thành công!"));
                        serverControl.broadcastToRoom(roomId, new Message(MessageType.ROOM_UPDATE_BROADCAST, current));
                        serverControl.broadcastRoomList();
                        serverControl.broadcastOnlineUsers();
                    } else {
                        sendMessage(new Message(MessageType.JOIN_ROOM_RESPONSE, null, false, "Phòng đã đầy hoặc đang trong trận!"));
                    }
                    break;
                }
                case SPECTATE_ROOM_REQUEST: {
                    int roomId = (Integer) req.getPayload();
                    Room targetRoom = serverControl.getRoomManager().getRoom(roomId);
                    if (targetRoom != null && targetRoom.getStatus() == Room.STATUS_PLAYING) {
                        this.currentRoomId = roomId;
                        this.isSpectator = true;
                        currentUser.setStatus("Đang xem");
                        sendMessage(new Message(MessageType.SPECTATE_ROOM_RESPONSE, targetRoom, true, "Đang vào xem..."));
                        serverControl.broadcastOnlineUsers();
                        
                        // Nếu đang chơi thì lấy luôn trạng thái hiện tại gửi cho spectator
                        GameManager gm = serverControl.getRoomManager().getGameManager(roomId);
                        if (gm != null) {
                            GameState state = gm.getGameState();
                            List<Card> hostHand = targetRoom.getPlayers().get(0).getHand();
                            List<Card> guestHand = targetRoom.getPlayers().get(1).getHand();
                            SpectatorState specState = new SpectatorState(state, hostHand, guestHand);
                            sendMessage(new Message(MessageType.SPECTATOR_STATE_BROADCAST, specState));
                        }
                    } else {
                        sendMessage(new Message(MessageType.SPECTATE_ROOM_RESPONSE, null, false, "Phòng chưa bắt đầu hoặc không tồn tại!"));
                    }
                    break;
                }
                case LEAVE_ROOM_REQUEST: {
                    if (currentRoomId != -1) {
                        int leavingRoomId = currentRoomId;
                        boolean wasSpectator = isSpectator;
                        currentRoomId = -1;
                        isSpectator = false;
                        if (!wasSpectator) {
                            serverControl.getRoomManager().leaveRoom(leavingRoomId, currentUser.getId());
                            Room current = serverControl.getRoomManager().getRoom(leavingRoomId);
                            if (current != null) {
                                serverControl.broadcastToRoom(leavingRoomId, new Message(MessageType.ROOM_UPDATE_BROADCAST, current));
                            }
                        }
                        currentUser.setStatus("Rảnh");
                        serverControl.broadcastRoomList();
                        serverControl.broadcastOnlineUsers();
                    }
                    break;
                }
                case START_GAME_REQUEST: {
                    Room r = serverControl.getRoomManager().getRoom(currentRoomId);
                    if (r != null && r.getHostId() == currentUser.getId() && r.getCurrentPlayerCount() >= 2) {
                        GameManager gm = new GameManager(r, serverControl);
                        serverControl.getRoomManager().setGameManager(currentRoomId, gm);
                        gm.startGame();
                        serverControl.getView().showMessage("Phòng #" + currentRoomId + " đã bắt đầu ván bài UNO!");
                        serverControl.updateRoomUsersStatus(currentRoomId, "Đang chơi");
                    } else {
                        sendMessage(new Message(MessageType.ERROR_NOTIFICATION, "Cần tối thiểu 2 người chơi để bắt đầu!"));
                    }
                    break;
                }
                case PLAY_CARD_REQUEST: {
                    Object[] params = (Object[]) req.getPayload();
                    Card card = (Card) params[0];
                    CardColor chosenColor = (CardColor) params[1];
                    GameManager gm = serverControl.getRoomManager().getGameManager(currentRoomId);
                    if (gm != null) {
                        boolean ok = gm.handlePlayCard(currentUser.getId(), card, chosenColor);
                        if (!ok) {
                            sendMessage(new Message(MessageType.ERROR_NOTIFICATION, "Lá bài không hợp lệ hoặc chưa đến lượt!"));
                        }
                    }
                    break;
                }
                case DRAW_CARD_REQUEST: {
                    GameManager gm = serverControl.getRoomManager().getGameManager(currentRoomId);
                    if (gm != null) {
                        gm.handleDrawCard(currentUser.getId());
                    }
                    break;
                }
                case CALL_UNO_REQUEST: {
                    GameManager gm = serverControl.getRoomManager().getGameManager(currentRoomId);
                    if (gm != null) {
                        gm.handleCallUno(currentUser.getId());
                    }
                    break;
                }
                case CHAT_MESSAGE: {
                    ChatMessage chat = (ChatMessage) req.getPayload();
                    chat.setSender(currentUser != null ? currentUser.getUsername() : "Ẩn danh");
                    if (currentRoomId != -1) {
                        serverControl.broadcastToRoom(currentRoomId, new Message(MessageType.CHAT_MESSAGE, chat));
                    }
                    break;
                }
                case GET_LEADERBOARD_REQUEST: {
                    List<User> top = userDAO.getLeaderboard();
                    sendMessage(new Message(MessageType.LEADERBOARD_RESPONSE, top));
                    break;
                }
                case GET_MATCH_HISTORY_REQUEST: {
                    List<Map<String, Object>> hist = matchDAO.getMatchHistory(currentUser.getId());
                    sendMessage(new Message(MessageType.MATCH_HISTORY_RESPONSE, hist));
                    break;
                }
                case INVITE_PLAYER_REQUEST: {
                    String targetUsername = (String) req.getPayload();
                    if (targetUsername == null || targetUsername.trim().isEmpty()) {
                        sendMessage(new Message(MessageType.INVITE_FEEDBACK, null, false, "Tên người chơi không hợp lệ!"));
                        break;
                    }

                    ClientHandler targetClient = serverControl.getClientByUsername(targetUsername);
                    if (targetClient == null) {
                        sendMessage(new Message(MessageType.INVITE_FEEDBACK, null, false, "Người chơi '" + targetUsername + "' hiện không trực tuyến!"));
                        break;
                    }

                    if (targetClient == this) {
                        sendMessage(new Message(MessageType.INVITE_FEEDBACK, null, false, "Bạn không thể tự mời chính mình!"));
                        break;
                    }

                    User targetUser = targetClient.getCurrentUser();
                    if (targetUser != null && "Đang chơi".equalsIgnoreCase(targetUser.getStatus())) {
                        sendMessage(new Message(MessageType.INVITE_FEEDBACK, null, false, "Người chơi '" + targetUsername + "' hiện đang trong ván đấu!"));
                        break;
                    }

                    // Tìm hoặc tạo phòng chơi để mời vào
                    Room room = null;
                    if (this.currentRoomId > 0) {
                        room = serverControl.getRoomManager().getRoom(this.currentRoomId);
                    }

                    if (room == null || room.isFull()) {
                        room = serverControl.getRoomManager().createRoom(
                                "Phòng của " + currentUser.getUsername(),
                                currentUser.getId(),
                                currentUser.getUsername(),
                                2
                        );
                        this.currentRoomId = room.getId();
                        currentUser.setStatus("Đang trong phòng");
                        sendMessage(new Message(MessageType.CREATE_ROOM_RESPONSE, room, true, "Đã tạo phòng mới để mời bạn!"));
                        serverControl.broadcastRoomList();
                        serverControl.broadcastOnlineUsers();
                    }

                    // Gửi thông báo mời thách đấu tới người chơi B
                    targetClient.sendMessage(new Message(
                            MessageType.INVITE_PLAYER_NOTIFICATION,
                            new Object[]{currentUser.getUsername(), room.getId(), room.getName()},
                            true,
                            currentUser.getUsername() + " đã gửi lời mời tham gia phòng!"
                    ));

                    sendMessage(new Message(MessageType.INVITE_FEEDBACK, null, true, "Đã gửi lời mời tới " + targetUsername + "! Đang chờ phản hồi..."));
                    break;
                }
                case INVITE_FEEDBACK: {
                    String inviterName = (String) req.getPayload();
                    if (inviterName != null && !inviterName.isEmpty()) {
                        ClientHandler inviterClient = serverControl.getClientByUsername(inviterName);
                        if (inviterClient != null) {
                            inviterClient.sendMessage(new Message(MessageType.INVITE_FEEDBACK, null, false, req.getMessage()));
                        }
                    }
                    break;
                }
                default:
                    break;
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public synchronized void sendMessage(Message msg) {
        try {
            if (oos != null && !socket.isClosed()) {
                oos.reset(); // Xóa bộ nhớ đệm ObjectOutputStream để cập nhật chính xác dữ liệu mới
                oos.writeObject(msg);
                oos.flush();
            }
        } catch (IOException e) {
            System.err.println("[ClientHandler] Lỗi gửi gói tin: " + e.getMessage());
        }
    }

    public void close() {
        isRunning = false;
        if (currentRoomId != -1 && currentUser != null) {
            int leavingRoomId = currentRoomId;
            boolean wasSpectator = isSpectator;
            currentRoomId = -1;
            isSpectator = false;
            if (!wasSpectator) {
                serverControl.getRoomManager().leaveRoom(leavingRoomId, currentUser.getId());
            }
            serverControl.broadcastRoomList();
        }
        serverControl.removeOnlineUser(this);
        serverControl.broadcastOnlineUsers();
        try {
            if (socket != null && !socket.isClosed()) socket.close();
        } catch (IOException ignored) {}
    }

    public User getCurrentUser() {
        return currentUser;
    }

    public int getCurrentRoomId() {
        return currentRoomId;
    }

    public boolean isSpectator() {
        return isSpectator;
    }

    public void setSpectator(boolean spectator) {
        isSpectator = spectator;
    }
}
