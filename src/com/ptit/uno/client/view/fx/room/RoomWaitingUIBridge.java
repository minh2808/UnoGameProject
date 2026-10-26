package com.ptit.uno.client.view.fx.room;

import com.ptit.uno.model.ChatMessage;
import com.ptit.uno.model.Player;
import com.ptit.uno.model.Room;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;

import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

/**
 * RoomWaitingUIBridge: Lớp cầu nối UI Code-Behind cho room_waiting_view.fxml.
 * Điều khiển sàn đấu 1-1 đối kháng, hiển thị avatar/ghế ngồi, chatbox và nút bắt đầu ván đấu.
 */
public class RoomWaitingUIBridge implements Initializable {

    @FXML private Label lblRoomInfo;
    @FXML private Label lblSeatHeader;

    // Ghế 1: Chủ phòng
    @FXML private Label lblHostElo;
    @FXML private Label lblHostName;

    // Ghế 2: Khách
    @FXML private Label lblGuestElo;
    @FXML private Label lblGuestName;
    @FXML private Label lblGuestAvatar;
    @FXML private VBox guestFrame;
    @FXML private Circle guestRing;

    // Chat
    @FXML private TextArea txtChatLog;
    @FXML private TextField txtChatInput;
    @FXML private Button btnSendChat;

    // Thanh điều khiển dưới
    @FXML private Label lblWaitNote;
    @FXML private Button btnStartGame;
    @FXML private Button btnLeaveRoom;

    private Runnable onStartGameAction;
    private Runnable onLeaveRoomAction;
    private Runnable onSendChatAction;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        if (btnStartGame != null) {
            btnStartGame.setOnAction(e -> {
                if (onStartGameAction != null) onStartGameAction.run();
            });
        }

        if (btnLeaveRoom != null) {
            btnLeaveRoom.setOnAction(e -> {
                if (onLeaveRoomAction != null) onLeaveRoomAction.run();
            });
        }

        if (btnSendChat != null) {
            btnSendChat.setOnAction(e -> handleSendMessage());
        }

        if (txtChatInput != null) {
            txtChatInput.setOnAction(e -> handleSendMessage());
        }
    }

    private void handleSendMessage() {
        if (txtChatInput != null && !txtChatInput.getText().trim().isEmpty()) {
            if (onSendChatAction != null) {
                onSendChatAction.run();
            }
        }
    }

    public void renderRoom(Room room, int currentUserId) {
        if (room == null) return;

        Platform.runLater(() -> {
            lblRoomInfo.setText(String.format("Phòng #%d - %s (Chủ phòng: %s)",
                    room.getId(), room.getName(), room.getHostName()));

            List<Player> players = room.getPlayers();
            int count = players != null ? players.size() : 0;
            lblSeatHeader.setText(String.format("ĐẤU TRƯỜNG ĐỐI KHÁNG 1-1 (%d/2)", count));

            // Ghế 1: Chủ phòng
            if (players != null && !players.isEmpty()) {
                Player host = players.get(0);
                lblHostName.setText(host.getUsername());
                lblHostElo.setText(host.isReady() ? "Chủ phòng (Sẵn sàng)" : "Chủ phòng (Đang chờ)");
            } else {
                lblHostName.setText(room.getHostName());
                lblHostElo.setText("Chủ phòng");
            }

            // Ghế 2: Khách
            if (players != null && players.size() >= 2) {
                Player guest = players.get(1);
                lblGuestName.setText(guest.getUsername());
                lblGuestElo.setText(guest.isReady() ? "Đối thủ (Sẵn sàng)" : "Đối thủ (Đã vào phòng)");
                org.kordamp.ikonli.javafx.FontIcon guestIcon = new org.kordamp.ikonli.javafx.FontIcon("fas-user");
                guestIcon.setIconSize(54);
                guestIcon.setIconColor(javafx.scene.paint.Color.web("#4ff0a0"));
                lblGuestAvatar.setText("");
                lblGuestAvatar.setGraphic(guestIcon);
                guestFrame.getStyleClass().remove("seat-frame-empty");
                if (!guestFrame.getStyleClass().contains("seat-frame")) {
                    guestFrame.getStyleClass().add("seat-frame");
                }
                guestRing.setVisible(true);
            } else {
                lblGuestName.setText("Ghế trống");
                lblGuestElo.setText("Đang chờ...");
                org.kordamp.ikonli.javafx.FontIcon waitIcon = new org.kordamp.ikonli.javafx.FontIcon("fas-clock");
                waitIcon.setIconSize(54);
                waitIcon.setIconColor(javafx.scene.paint.Color.web("#95a5a6"));
                lblGuestAvatar.setText("");
                lblGuestAvatar.setGraphic(waitIcon);
                guestFrame.getStyleClass().remove("seat-frame");
                if (!guestFrame.getStyleClass().contains("seat-frame-empty")) {
                    guestFrame.getStyleClass().add("seat-frame-empty");
                }
                guestRing.setVisible(false);
            }

            // Phân quyền bắt đầu ván
            boolean isHost = (room.getHostId() == currentUserId);
            btnStartGame.setVisible(isHost);

            if (isHost) {
                if (count >= 2) {
                    btnStartGame.setDisable(false);
                    btnStartGame.setText("BẮT ĐẦU VÁN ĐẤU (2 người)");
                } else {
                    btnStartGame.setDisable(true);
                    btnStartGame.setText("Chờ người thứ 2 vào...");
                }
                lblWaitNote.setText("");
            } else {
                btnStartGame.setVisible(false);
                lblWaitNote.setText("Chỉ chủ phòng bấm được nút Bắt đầu.");
            }
        });
    }

    public void appendChatMessage(ChatMessage msg) {
        if (msg == null) return;
        Platform.runLater(() -> {
            if (txtChatLog != null) {
                txtChatLog.appendText(msg.toString() + "\n");
            }
        });
    }

    public void clearChat() {
        Platform.runLater(() -> {
            if (txtChatLog != null) {
                txtChatLog.clear();
            }
            if (txtChatInput != null) {
                txtChatInput.clear();
            }
        });
    }

    public String getChatMessage() {
        if (txtChatInput == null) return "";
        String msg = txtChatInput.getText().trim();
        txtChatInput.clear();
        return msg;
    }

    public void setOnStartGameAction(Runnable r) { this.onStartGameAction = r; }
    public void setOnLeaveRoomAction(Runnable r) { this.onLeaveRoomAction = r; }
    public void setOnSendChatAction(Runnable r) { this.onSendChatAction = r; }
}
