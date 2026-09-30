package com.ptit.uno.client.view.fx.lobby;

import com.ptit.uno.client.view.MatchHistoryFrm;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.io.File;
import java.net.URL;
import java.util.List;
import java.util.Map;

public class MatchHistoryFXView extends MatchHistoryFrm {
    private Stage stage;
    private MatchHistoryUIBridge controller;
    private String currentUsername;

    public MatchHistoryFXView() {
        super.setVisible(false);

        try {
            Platform.startup(() -> {});
        } catch (IllegalStateException ignored) {}

        Platform.runLater(this::initFX);
    }

    private void initFX() {
        try {
            URL fxmlUrl = getClass().getResource("/resource/fxml/match_history_view.fxml");
            if (fxmlUrl == null) {
                fxmlUrl = getClass().getResource("/fxml/match_history_view.fxml");
            }
            if (fxmlUrl == null) {
                File f = new File("src/resource/fxml/match_history_view.fxml");
                if (f.exists()) {
                    fxmlUrl = f.toURI().toURL();
                }
            }

            FXMLLoader loader = new FXMLLoader(fxmlUrl);
            Parent root = loader.load();
            this.controller = loader.getController();

            stage = new Stage();
            stage.initStyle(StageStyle.TRANSPARENT);
            stage.setTitle("LỊCH SỬ CÁC VÁN ĐẤU");
            Scene scene = new Scene(root);
            scene.setFill(Color.TRANSPARENT);
            stage.setScene(scene);
            
            // To make it behave like a dialog
            stage.initModality(Modality.APPLICATION_MODAL);
            
        } catch (Exception e) {
            System.err.println("[MatchHistoryFXView] Lỗi khởi tạo: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    public void setCurrentUsername(String username) {
        this.currentUsername = username;
        if (controller != null) {
            controller.setCurrentUsername(username);
        }
    }

    @Override
    public void renderHistory(List<Map<String, Object>> list) {
        if (controller != null) {
            Platform.runLater(() -> {
                controller.setCurrentUsername(currentUsername);
                controller.renderHistory(list);
            });
        }
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
