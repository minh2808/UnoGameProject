package com.ptit.uno.client.view.fx.room;

import com.ptit.uno.client.view.RoomWaitingFrm;
import com.ptit.uno.model.ChatMessage;
import com.ptit.uno.model.Room;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * RoomWaitingFXView: Cầu nối View chuẩn MVC giữa ClientControl và giao diện Phòng chờ JavaFX FXML.
 * Kế thừa RoomWaitingFrm để tương thích hoàn toàn theo mô hình MVC Cải tiến.
 */
public class RoomWaitingFXView extends RoomWaitingFrm {
    private Stage stage;
    private RoomWaitingUIBridge controller;

    private final List<ActionListener> startGameListeners = new ArrayList<>();
    private final List<ActionListener> leaveRoomListeners = new ArrayList<>();
    private final List<ActionListener> sendChatListeners = new ArrayList<>();

    public RoomWaitingFXView() {
        super.setVisible(false);

        try {
            Platform.startup(() -> {});
        } catch (IllegalStateException ignored) {}

        Platform.runLater(this::initFX);
    }

    private void initFX() {
        try {
            URL fxmlUrl = getClass().getResource("/resource/fxml/room_waiting_view.fxml");
            if (fxmlUrl == null) {
                fxmlUrl = getClass().getResource("/fxml/room_waiting_view.fxml");
            }
            if (fxmlUrl == null) {
                File f = new File("src/resource/fxml/room_waiting_view.fxml");
                if (f.exists()) {
                    fxmlUrl = f.toURI().toURL();
                }
            }

            if (fxmlUrl == null) {
                System.err.println("[RoomWaitingFXView] Không tìm thấy file room_waiting_view.fxml!");
                return;
            }

            FXMLLoader loader = new FXMLLoader(fxmlUrl);
            Parent root = loader.load();
            this.controller = loader.getController();

            stage = new Stage();
            stage.setTitle("UNO Multiplayer - Phòng chờ (Đấu trường 1-1)");
            Scene scene = new Scene(root, 1100, 680);
            scene.setFill(Color.TRANSPARENT);
            stage.setScene(scene);
            stage.setMinWidth(1000);
            stage.setMinHeight(620);
            stage.centerOnScreen();

            if (controller != null) {
                controller.setOnStartGameAction(() -> fireEvent(startGameListeners, "START_GAME"));
                controller.setOnLeaveRoomAction(() -> fireEvent(leaveRoomListeners, "LEAVE_ROOM"));
                controller.setOnSendChatAction(() -> fireEvent(sendChatListeners, "SEND_CHAT"));
            }

            stage.setOnCloseRequest(e -> {
                e.consume();
                fireEvent(leaveRoomListeners, "LEAVE_ROOM");
            });

        } catch (Exception e) {
            System.err.println("[RoomWaitingFXView] Lỗi khởi tạo JavaFX RoomWaiting View: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void fireEvent(List<ActionListener> listeners, String command) {
        ActionEvent event = new ActionEvent(this, ActionEvent.ACTION_PERFORMED, command);
        for (ActionListener l : listeners) {
            l.actionPerformed(event);
        }
    }

    @Override
    public void renderRoom(Room room, int currentUserId) {
        if (controller != null) {
            controller.renderRoom(room, currentUserId);
        }
    }

    @Override
    public void appendChatMessage(ChatMessage msg) {
        if (controller != null) {
            controller.appendChatMessage(msg);
        }
    }

    @Override
    public String getChatMessage() {
        return controller != null ? controller.getChatMessage() : "";
    }

    @Override
    public void showMessage(String msg) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Thông báo");
            alert.setHeaderText(null);
            alert.setContentText(msg);
            alert.show();
        });
    }

    @Override
    public void addStartGameListener(ActionListener log) {
        if (log != null && !startGameListeners.contains(log)) startGameListeners.add(log);
    }

    @Override
    public void addLeaveRoomListener(ActionListener log) {
        if (log != null && !leaveRoomListeners.contains(log)) leaveRoomListeners.add(log);
    }

    @Override
    public void addSendChatListener(ActionListener log) {
        if (log != null && !sendChatListeners.contains(log)) sendChatListeners.add(log);
    }

    @Override
    public void setVisible(boolean visible) {
        super.setVisible(false);
        Platform.runLater(() -> {
            if (stage != null) {
                if (visible) {
                    stage.show();
                    stage.toFront();
                } else {
                    stage.hide();
                }
            }
        });
    }

    public Stage getStage() {
        return stage;
    }

    @Override
    public boolean isVisible() {
        return stage != null && stage.isShowing();
    }
}
