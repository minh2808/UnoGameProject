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
            stage.setTitle("UNO - Spectator Mode");
            stage.setScene(new Scene(root, 1100, 700));
            stage.setResizable(false);
            
            // Override close behavior to trigger leave instead
            stage.setOnCloseRequest(e -> {
                e.consume();
                fireEvent(leaveRoomListeners, "LEAVE_SPECTATE");
            });

            if (controller != null) {
                controller.setOnLeaveAction(() -> fireEvent(leaveRoomListeners, "LEAVE_SPECTATE"));
                controller.setOnSendChatAction(msg -> {
                    chatMessageToSend = msg;
                    fireEvent(sendChatListeners, "SEND_CHAT");
                });
            }
        } catch (Exception e) {
            e.printStackTrace();
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
}
