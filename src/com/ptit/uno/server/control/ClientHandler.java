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
                    if (u == null || u.getUsername() == null) {
                        sendMessage(new Message(MessageType.LOGIN_RESPONSE, null, false, "Thông tin đăng nhập không hợp lệ!"));
                        break;
                    }
                    if (serverControl.getClientByUsername(u.getUsername().trim()) != null) {
                        sendMessage(new Message(MessageType.LOGIN_RESPONSE, null, false, "Tài khoản này đang được đăng nhập ở nơi khác!"));
                        break;
                    }
                    User loggedIn = userDAO.checkLogin(u.getUsername().trim(), u.getPassword());
                    if (loggedIn != null) {
                        this.currentUser = loggedIn;
                        if (!serverControl.addOnlineUser(this)) {
                            this.currentUser = null;
                            sendMessage(new Message(MessageType.LOGIN_RESPONSE, null, false, "Tài khoản này đang được đăng nhập ở nơi khác!"));
                            break;
                        }
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
                    String roomName = (r != null && r.getName() != null) ? r.getName().trim() : "";
                    if (roomName.isEmpty()) {
                        sendMessage(new Message(MessageType.CREATE_ROOM_RESPONSE, null, false, "Tên phòng không được để trống!"));
                        break;
                    }
                    if (this.currentRoomId != -1) {
                        serverControl.getRoomManager().leaveRoom(this.currentRoomId, currentUser.getId());
                    }
                    int maxP = (r.getMaxPlayers() > 0) ? Math.min(r.getMaxPlayers(), 2) : 2;
                    Room created = serverControl.getRoomManager().createRoom(
                            roomName, currentUser.getId(), currentUser.getUsername(), maxP
                    );
                    if (created == null) {
                        sendMessage(new Message(MessageType.CREATE_ROOM_RESPONSE, null, false,
                                "Tên phòng \"" + roomName + "\" đã tồn tại! Vui lòng đặt tên khác."));
                        break;
                    }
                    this.currentRoomId = created.getId();
                    currentUser.setStatus("Đang trong phòng");
                    sendMessage(new Message(MessageType.CREATE_ROOM_RESPONSE, created, true, "Tạo phòng thành công!"));
                    serverControl.broadcastRoomList();
                    serverControl.broadcastOnlineUsers();
                    break;
                }
                case JOIN_ROOM_REQUEST: {
                    int roomId = (Integer) req.getPayload();
                    if (this.currentRoomId != -1 && this.currentRoomId != roomId) {
                        serverControl.getRoomManager().leaveRoom(this.currentRoomId, currentUser.getId());
                    }
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
                    GameManager gm = serverControl.getRoomManager().getGameManager(roomId);
                    GameState state = (gm != null) ? gm.getGameState() : null;
                    if (targetRoom != null
                            && Room.STATUS_PLAYING.equals(targetRoom.getStatus())
                            && state != null
                            && !state.isGameOver()
                            && targetRoom.getPlayers().size() >= 2) {
                        this.currentRoomId = roomId;
                        this.isSpectator = true;
                        currentUser.setStatus("Đang xem");
                        sendMessage(new Message(MessageType.SPECTATE_ROOM_RESPONSE, targetRoom, true, "Đang vào xem..."));
                        serverControl.broadcastOnlineUsers();

                        List<Card> hostHand = new java.util.ArrayList<Card>(targetRoom.getPlayers().get(0).getHand());
                        List<Card> guestHand = new java.util.ArrayList<Card>(targetRoom.getPlayers().get(1).getHand());
                        SpectatorState specState = new SpectatorState(state, hostHand, guestHand);
                        sendMessage(new Message(MessageType.SPECTATOR_STATE_BROADCAST, specState));
                    } else {
                        sendMessage(new Message(MessageType.SPECTATE_ROOM_RESPONSE, null, false,
                                "Không thể xem trận: Trận đấu đã kết thúc hoặc phòng không còn tồn tại!"));
                        sendMessage(new Message(MessageType.ROOM_LIST_RESPONSE,
                                serverControl.getRoomManager().getAllRooms()));
                    }
                    break;
                }
                case LEAVE_ROOM_REQUEST: {
                    if (currentRoomId != -1) {
                        int leavingRoomId = currentRoomId;
                        boolean wasSpectator = isSpectator;
                        Room leavingRoom = serverControl.getRoomManager().getRoom(leavingRoomId);
                        boolean wasPlayingOrFinished = leavingRoom != null
                                && (Room.STATUS_PLAYING.equals(leavingRoom.getStatus())
                                || Room.STATUS_FINISHED.equals(leavingRoom.getStatus()));
                        currentRoomId = -1;
                        isSpectator = false;
                        if (!wasSpectator) {
                            serverControl.getRoomManager().leaveRoom(leavingRoomId, currentUser.getId());
                            Room current = serverControl.getRoomManager().getRoom(leavingRoomId);
                            if (current != null) {
                                serverControl.broadcastToRoom(leavingRoomId, new Message(MessageType.ROOM_UPDATE_BROADCAST, current));
                            } else {
                                for (ClientHandler ch : serverControl.getActiveClients()) {
                                    if (ch != this && ch.getCurrentRoomId() == leavingRoomId) {
                                        ch.setCurrentRoomId(-1);
                                        ch.setSpectator(false);
                                        if (ch.getCurrentUser() != null) {
                                            ch.getCurrentUser().setStatus("Rảnh");
                                        }
                                        // Chỉ đẩy người còn lại về sảnh nếu đang ở phòng chờ (WAITING).
                                        // Nếu đang trong trận (PLAYING) hoặc vừa kết thúc (FINISHED), giữ nguyên màn hình bàn đấu
                                        // để người chơi còn lại xem bảng Kết Quả Chiến Thắng (+20 điểm).
                                        if (!wasPlayingOrFinished) {
                                            ch.sendMessage(new Message(MessageType.ROOM_UPDATE_BROADCAST, null, false,
                                                    "Phòng đấu đã giải tán vì người chơi đã rời đi."));
                                        }
                                    }
                                }
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
                        serverControl.broadcastRoomList();
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
                    if (isSpectator) {
                        break; // Khán giả (góc nhìn thứ 3) không có quyền gửi bình luận / tin nhắn
                    }
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
                        String baseName = "Phòng của " + currentUser.getUsername();
                        String autoName = baseName;
                        int suffix = 2;
                        while (serverControl.getRoomManager().isRoomNameExists(autoName)) {
                            autoName = baseName + " (" + suffix + ")";
                            suffix++;
                        }
                        room = serverControl.getRoomManager().createRoom(
                                autoName,
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
                case REMATCH_REQUEST: {
                    Room r = serverControl.getRoomManager().getRoom(currentRoomId);
                    if (r == null || currentUser == null) {
                        sendMessage(new Message(MessageType.ROOM_UPDATE_BROADCAST, null, false,
                                "Đối thủ đã rời phòng hoặc phòng không còn tồn tại!"));
                        break;
                    }

                    Player opponent = null;
                    for (Player p : r.getPlayers()) {
                        if (p.getUserId() != currentUser.getId()) {
                            opponent = p;
                            break;
                        }
                    }

                    if (opponent == null) {
                        sendMessage(new Message(MessageType.ROOM_UPDATE_BROADCAST, null, false,
                                "Đối thủ đã rời phòng!"));
                        break;
                    }

                    if (opponent.isBot()) {
                        serverControl.broadcastToRoom(currentRoomId, new Message(MessageType.REMATCH_START_COUNTDOWN));
                        java.util.concurrent.Executors.newSingleThreadScheduledExecutor().schedule(() -> {
                            Room rm = serverControl.getRoomManager().getRoom(currentRoomId);
                            if (rm != null && rm.getCurrentPlayerCount() >= 2) {
                                GameManager gm = new GameManager(rm, serverControl);
                                serverControl.getRoomManager().setGameManager(currentRoomId, gm);
                                gm.startGame();
                                serverControl.getView().showMessage("Phòng #" + currentRoomId + " bắt đầu ván đấu lại!");
                                serverControl.updateRoomUsersStatus(currentRoomId, "Đang chơi");
                                serverControl.broadcastRoomList();
                            }
                        }, 3500, java.util.concurrent.TimeUnit.MILLISECONDS);
                    } else {
                        ClientHandler opponentClient = serverControl.getClientById(opponent.getUserId());
                        if (opponentClient != null) {
                            opponentClient.sendMessage(new Message(MessageType.REMATCH_REQUEST, currentUser.getUsername()));
                        } else {
                            sendMessage(new Message(MessageType.ERROR_NOTIFICATION, "Đối thủ đã rời phòng!"));
                        }
                    }
                    break;
                }
                case REMATCH_RESPONSE: {
                    boolean accepted = false;
                    if (req.getPayload() instanceof Boolean) {
                        accepted = (Boolean) req.getPayload();
                    }
                    Room r = serverControl.getRoomManager().getRoom(currentRoomId);
                    if (r == null || currentUser == null) {
                        sendMessage(new Message(MessageType.ROOM_UPDATE_BROADCAST, null, false,
                                "Đối thủ đã rời phòng hoặc phòng không còn tồn tại!"));
                        break;
                    }

                    Player opponent = null;
                    for (Player p : r.getPlayers()) {
                        if (p.getUserId() != currentUser.getId()) {
                            opponent = p;
                            break;
                        }
                    }

                    ClientHandler opponentClient = (opponent != null) ? serverControl.getClientById(opponent.getUserId()) : null;

                    if (!accepted) {
                        serverControl.getRoomManager().removeRoom(currentRoomId);
                        serverControl.broadcastRoomList();
                        if (opponentClient != null) {
                            opponentClient.sendMessage(new Message(MessageType.ERROR_NOTIFICATION, currentUser.getUsername() + " đã từ chối đấu lại!"));
                        }
                    } else {
                        serverControl.broadcastToRoom(currentRoomId, new Message(MessageType.REMATCH_START_COUNTDOWN));
                        java.util.concurrent.Executors.newSingleThreadScheduledExecutor().schedule(() -> {
                            Room rm = serverControl.getRoomManager().getRoom(currentRoomId);
                            if (rm != null && rm.getCurrentPlayerCount() >= 2) {
                                GameManager gm = new GameManager(rm, serverControl);
                                serverControl.getRoomManager().setGameManager(currentRoomId, gm);
                                gm.startGame();
                                serverControl.getView().showMessage("Phòng #" + currentRoomId + " bắt đầu ván đấu lại!");
                                serverControl.updateRoomUsersStatus(currentRoomId, "Đang chơi");
                                serverControl.broadcastRoomList();
                            }
                        }, 3500, java.util.concurrent.TimeUnit.MILLISECONDS);
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
            Room leavingRoom = serverControl.getRoomManager().getRoom(leavingRoomId);
            boolean wasPlayingOrFinished = leavingRoom != null
                    && (Room.STATUS_PLAYING.equals(leavingRoom.getStatus())
                    || Room.STATUS_FINISHED.equals(leavingRoom.getStatus()));
            currentRoomId = -1;
            isSpectator = false;
            if (!wasSpectator) {
                serverControl.getRoomManager().leaveRoom(leavingRoomId, currentUser.getId());
                Room current = serverControl.getRoomManager().getRoom(leavingRoomId);
                if (current != null) {
                    serverControl.broadcastToRoom(leavingRoomId, new Message(MessageType.ROOM_UPDATE_BROADCAST, current));
                } else {
                    for (ClientHandler ch : serverControl.getActiveClients()) {
                        if (ch != this && ch.getCurrentRoomId() == leavingRoomId) {
                            ch.setCurrentRoomId(-1);
                            ch.setSpectator(false);
                            if (ch.getCurrentUser() != null) {
                                ch.getCurrentUser().setStatus("Rảnh");
                            }
                            if (!wasPlayingOrFinished) {
                                ch.sendMessage(new Message(MessageType.ROOM_UPDATE_BROADCAST, null, false,
                                        "Phòng đấu đã giải tán do đối thủ ngắt kết nối."));
                            }
                        }
                    }
                }
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

    public void setCurrentRoomId(int currentRoomId) {
        this.currentRoomId = currentRoomId;
    }

    public boolean isSpectator() {
        return isSpectator;
    }

    public void setSpectator(boolean spectator) {
        isSpectator = spectator;
    }
}
