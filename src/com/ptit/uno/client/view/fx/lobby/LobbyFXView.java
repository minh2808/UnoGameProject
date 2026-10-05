package com.ptit.uno.client.view.fx.lobby;

import com.ptit.uno.client.view.LobbyFrm;
import com.ptit.uno.model.Room;
import com.ptit.uno.model.User;
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
 * LobbyFXView: Cầu nối View chuẩn MVC giữa ClientControl và giao diện Sảnh
 * chính JavaFX FXML.
 * Kế thừa LobbyFrm để tương thích hoàn toàn theo mô hình MVC Cải tiến (Slide
 * b02-2 & b05).
 */
public class LobbyFXView extends LobbyFrm {
    private Stage stage;
    private LobbyUIBridge controller;

    private final List<ActionListener> createRoomListeners = new ArrayList<>();
    private final List<ActionListener> joinRoomListeners = new ArrayList<>();
    private final List<ActionListener> refreshListeners = new ArrayList<>();
    private final List<ActionListener> leaderboardListeners = new ArrayList<>();
    private final List<ActionListener> historyListeners = new ArrayList<>();
    private final List<ActionListener> logoutListeners = new ArrayList<>();
    private final List<ActionListener> spectateListeners = new ArrayList<>();

    public LobbyFXView() {
        super.setVisible(false);

        try {
            Platform.startup(() -> {
            });
        } catch (IllegalStateException ignored) {
        }

        Platform.runLater(this::initFX);
    }

    private void initFX() {
        try {
            URL fxmlUrl = getClass().getResource("/resource/fxml/lobby_view.fxml");
            if (fxmlUrl == null) {
                fxmlUrl = getClass().getResource("/fxml/lobby_view.fxml");
            }
            if (fxmlUrl == null) {
                File f = new File("src/resource/fxml/lobby_view.fxml");
                if (f.exists()) {
                    fxmlUrl = f.toURI().toURL();
                }
            }

            if (fxmlUrl == null) {
                System.err.println("[LobbyFXView] Không tìm thấy file lobby_view.fxml!");
                return;
            }

            FXMLLoader loader = new FXMLLoader(fxmlUrl);
            Parent root = loader.load();
            this.controller = loader.getController();

            stage = new Stage();
            stage.setTitle("UNO Multiplayer - Sảnh chính (Lobby)");
            Scene scene = new Scene(root, 1100, 680);
            scene.setFill(Color.TRANSPARENT);
            stage.setScene(scene);
            stage.setMinWidth(1000);
            stage.setMinHeight(620);
            stage.centerOnScreen();

            // Đăng ký kích hoạt listeners
            if (controller != null) {
                controller.setOnCreateRoomAction(() -> fireEvent(createRoomListeners, "CREATE_ROOM"));
                controller.setOnJoinRoomAction(() -> fireEvent(joinRoomListeners, "JOIN_ROOM"));
                controller.setOnRefreshAction(() -> fireEvent(refreshListeners, "REFRESH"));
                controller.setOnLeaderboardAction(() -> fireEvent(leaderboardListeners, "LEADERBOARD"));
                controller.setOnHistoryAction(() -> fireEvent(historyListeners, "HISTORY"));
                controller.setOnLogoutAction(() -> fireEvent(logoutListeners, "LOGOUT"));
                controller.setOnSpectateAction(() -> fireEvent(spectateListeners, "SPECTATE_ROOM"));
            }

            stage.setOnCloseRequest(e -> {
                System.exit(0);
            });

        } catch (Exception e) {
            System.err.println("[LobbyFXView] Lỗi khởi tạo JavaFX Lobby View: " + e.getMessage());
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
    public void setUserInfo(User user) {
        if (controller != null) {
            controller.updateUserInfo(user);
        }
    }

    @Override
    public void renderRooms(List<Room> rooms) {
        if (controller != null) {
            controller.updateRoomList(rooms);
        }
    }

    @Override
    public void renderOnlineUsers(List<User> users) {
        if (controller != null) {
            controller.updateOnlineUsers(users);
        }
    }

    @Override
    public Integer getSelectedRoomId() {
        if (controller != null) {
            int id = controller.getSelectedRoomId();
            return id != -1 ? id : null;
        }
        return null;
    }

    @Override
    public String promptCreateRoomName() {
        if (controller != null) {
            final String[] result = new String[1];
            // Đồng bộ gọi hiển thị dialog trên JavaFX Application Thread
            if (Platform.isFxApplicationThread()) {
                return controller.promptCreateRoomName();
            } else {
                java.util.concurrent.CountDownLatch latch = new java.util.concurrent.CountDownLatch(1);
                Platform.runLater(() -> {
                    try {
                        result[0] = controller.promptCreateRoomName();
                    } finally {
                        latch.countDown();
                    }
                });
                try {
                    latch.await();
                } catch (InterruptedException ignored) {
                }
                return result[0];
            }
        }
        return JOptionPane.showInputDialog(null, "Nhập tên phòng mới:", "Tạo phòng UNO", JOptionPane.PLAIN_MESSAGE);
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
    public void addCreateRoomListener(ActionListener log) {
        if (log != null && !createRoomListeners.contains(log))
            createRoomListeners.add(log);
    }

    @Override
    public void addJoinRoomListener(ActionListener log) {
        if (log != null && !joinRoomListeners.contains(log))
            joinRoomListeners.add(log);
    }

    @Override
    public void addRefreshListener(ActionListener log) {
        if (log != null && !refreshListeners.contains(log))
            refreshListeners.add(log);
    }

    @Override
    public void addLeaderboardListener(ActionListener log) {
        if (log != null && !leaderboardListeners.contains(log))
            leaderboardListeners.add(log);
    }

    @Override
    public void addHistoryListener(ActionListener log) {
        if (log != null && !historyListeners.contains(log))
            historyListeners.add(log);
    }

    @Override
    public void addLogoutListener(ActionListener log) {
        if (log != null && !logoutListeners.contains(log))
            logoutListeners.add(log);
    }

    public void addSpectateRoomListener(ActionListener log) {
        if (log != null && !spectateListeners.contains(log))
            spectateListeners.add(log);
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
