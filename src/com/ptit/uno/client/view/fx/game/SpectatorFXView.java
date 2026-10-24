package com.ptit.uno.client.view.fx.game;

import com.ptit.uno.model.Card;
import com.ptit.uno.model.GameState;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import javax.swing.*;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;

public class SpectatorFXView extends JFrame {
    private Stage stage;
    private SpectatorUIBridge controller;
    private final List<ActionListener> leaveRoomListeners = new ArrayList<>();
    private final List<ActionListener> sendChatListeners = new ArrayList<>();
    
    // Store chat text from bridge temporarily so ClientControl can read it
    private String chatMessageToSend = "";

    public SpectatorFXView() {
        super.setVisible(false); // Hide Swing JFrame
        try {
            Platform.startup(() -> {});
        } catch (IllegalStateException ignored) {}
        Platform.runLater(this::initFX);
    }

    private void initFX() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/resource/fxml/spectator_view.fxml"));
            Parent root = loader.load();
            controller = loader.getController();

            stage = new Stage();
            stage.setTitle("UNO Multiplayer - Chế độ Xem trận (Khán giả)");
            Scene scene = new Scene(root, 1150, 680);
            FXSoundHelper.attachButtonSound(scene);
            stage.setScene(scene);
            stage.setMinWidth(1000);
            stage.setMinHeight(620);
            stage.centerOnScreen();
            
            // Override close behavior to trigger leave instead
            stage.setOnCloseRequest(e -> {
                e.consume();
                fireEvent(leaveRoomListeners, "LEAVE_SPECTATE");
            });

            if (controller != null) {
                controller.setOnLeaveAction(() -> fireEvent(leaveRoomListeners, "LEAVE_SPECTATE"));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void resetBoard() {
        if (controller != null) {
            controller.resetBoard();
        }
    }

    public void setPlayersInfo(String hostName, String guestName) {
        if (controller != null) {
            controller.setPlayersInfo(hostName, guestName);
        }
    }

    public void updateGameState(GameState state, List<Card> hostHand, List<Card> guestHand) {
        if (controller != null) {
            controller.updateGameState(state, hostHand, guestHand);
        }
    }

    public void updateCountdown(int timeLeft) {
        if (controller != null) {
            controller.updateCountdown(timeLeft);
        }
    }

    public void addChatMessage(String sender, String message, boolean isSelf) {
        if (controller != null) {
            controller.addChatMessage(sender, message, isSelf);
        }
    }

    public void addLeaveRoomListener(ActionListener log) {
        if (log != null && !leaveRoomListeners.contains(log)) leaveRoomListeners.add(log);
    }

    public void addSendChatListener(ActionListener log) {
        if (log != null && !sendChatListeners.contains(log)) sendChatListeners.add(log);
    }

    private void fireEvent(List<ActionListener> listeners, String command) {
        java.awt.event.ActionEvent event = new java.awt.event.ActionEvent(this, java.awt.event.ActionEvent.ACTION_PERFORMED, command);
        for (ActionListener listener : listeners) {
            listener.actionPerformed(event);
        }
    }

    public String getChatMessage() {
        return chatMessageToSend;
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

    @Override
    public boolean isVisible() {
        return stage != null && stage.isShowing();
    }

    public Stage getStage() {
        return stage;
    }
}
