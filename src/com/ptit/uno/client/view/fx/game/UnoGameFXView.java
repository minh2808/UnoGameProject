package com.ptit.uno.client.view.fx.game;

import com.ptit.uno.model.Card;
import com.ptit.uno.model.ChatMessage;
import com.ptit.uno.model.GameState;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

import java.io.File;
import java.net.URL;
import java.util.List;

/**
 * UnoGameFXView: Cầu nối View chuẩn MVC giữa ClientControl và giao diện JavaFX FXML bàn chơi.
 * Đóng gói toàn bộ thao tác luồng Platform.runLater, cho phép Controller gọi các hàm
 * setVisible, renderHand, updateGameState, appendChatMessage tương tự một JFrame truyền thống.
 */
public class UnoGameFXView {
    private Stage stage;
    private GameBoardUIBridge controller;

    public UnoGameFXView() {
        try {
            Platform.startup(() -> {});
        } catch (IllegalStateException ignored) {}

        Platform.runLater(this::initFX);
    }

    private void initFX() {
        try {
            URL fxmlUrl = getClass().getResource("/resource/fxml/game_board.fxml");
            if (fxmlUrl == null) {
                fxmlUrl = getClass().getResource("/fxml/game_board.fxml");
            }
            if (fxmlUrl == null) {
                File f = new File("src/resource/fxml/game_board.fxml");
                if (f.exists()) {
                    fxmlUrl = f.toURI().toURL();
                }
            }

            if (fxmlUrl == null) {
                System.err.println("[UnoGameFXView] Không tìm thấy file FXML game_board.fxml!");
                return;
            }

            FXMLLoader loader = new FXMLLoader(fxmlUrl);
            Parent root = loader.load();
            this.controller = loader.getController();

            stage = new Stage();
            stage.setTitle("UNO Multiplayer Online - PTIT LTM");
            stage.setScene(new Scene(root, 1150, 680));
            stage.setMinWidth(1000);
            stage.setMinHeight(620);
            stage.centerOnScreen();

            stage.setOnCloseRequest(e -> {
                e.consume();
            });

        } catch (Exception e) {
            System.err.println("[UnoGameFXView] Lỗi khởi tạo JavaFX View: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void setActionListener(GameBoardUIBridge.GameActionListener listener) {
        Platform.runLater(() -> {
            if (controller != null) {
                controller.setActionListener(listener);
            }
        });
    }

    public void setPlayerUsername(String username) {
        Platform.runLater(() -> {
            if (controller != null) {
                controller.setPlayerInfo(username);
            }
        });
    }

    public void setVisible(boolean visible) {
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

    public boolean isVisible() {
        return stage != null && stage.isShowing();
    }

    public void renderHand(List<Card> hand) {
        Platform.runLater(() -> {
            if (controller != null) {
                controller.setHandCards(hand);
            }
        });
    }

    public void updateGameState(GameState state, int currentUserId) {
        Platform.runLater(() -> {
            if (controller != null) {
                controller.updateGameState(state, currentUserId);
            }
        });
    }

    public void appendChatMessage(ChatMessage chat) {
        if (chat == null) return;
        Platform.runLater(() -> {
            if (controller != null) {
                controller.addChatMessage(chat.getSender(), chat.getContent(), Color.web("#f1c40f"));
            }
        });
    }

    public void addGameLog(String prefix, String message, Color prefixColor, Color messageColor) {
        Platform.runLater(() -> {
            if (controller != null) {
                controller.addGameLog(prefix, message, prefixColor, messageColor);
            }
        });
    }

    public GameBoardUIBridge getController() {
        return controller;
    }
}
