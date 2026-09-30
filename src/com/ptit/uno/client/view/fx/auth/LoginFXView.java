package com.ptit.uno.client.view.fx.auth;

import com.ptit.uno.client.view.LoginFrm;
import com.ptit.uno.model.User;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
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
 * LoginFXView: Cầu nối View chuẩn MVC giữa ClientControl và giao diện JavaFX FXML.
 * Kế thừa LoginFrm để đảm bảo tính đa hình và tương thích 100% với ClientControl theo Slide b02-2 & b05.
 * Toàn bộ logic hiển thị FXML và xử lý đồ họa Dark Cyber được đóng gói bên trong.
 */
public class LoginFXView extends LoginFrm {
    private Stage stage;
    private LoginUIBridge controller;
    private final List<ActionListener> loginListeners = new ArrayList<>();
    private final List<ActionListener> toRegisterListeners = new ArrayList<>();

    public LoginFXView() {
        super.setVisible(false);

        try {
            Platform.startup(() -> {});
        } catch (IllegalStateException ignored) {}

        Platform.runLater(this::initFX);
    }

    private void initFX() {
        try {
            URL fxmlUrl = getClass().getResource("/resource/fxml/login_view.fxml");
            if (fxmlUrl == null) {
                fxmlUrl = getClass().getResource("/fxml/login_view.fxml");
            }
            if (fxmlUrl == null) {
                File f = new File("src/resource/fxml/login_view.fxml");
                if (f.exists()) {
                    fxmlUrl = f.toURI().toURL();
                }
            }

            if (fxmlUrl == null) {
                System.err.println("[LoginFXView] Không tìm thấy file login_view.fxml!");
                return;
            }

            FXMLLoader loader = new FXMLLoader(fxmlUrl);
            Parent root = loader.load();
            this.controller = loader.getController();

            stage = new Stage();
            stage.setTitle("UNO Multiplayer - Đăng nhập (PTIT LTM)");
            Scene scene = new Scene(root, 960, 580);
            scene.setFill(Color.TRANSPARENT);
            stage.setScene(scene);
            stage.setMinWidth(900);
            stage.setMinHeight(550);
            stage.centerOnScreen();

            if (controller != null) {
                controller.setOnLoginAction(() -> {
                    ActionEvent event = new ActionEvent(this, ActionEvent.ACTION_PERFORMED, "LOGIN");
                    for (ActionListener l : loginListeners) {
                        l.actionPerformed(event);
                    }
                });

                controller.setOnToRegisterAction(() -> {
                    ActionEvent event = new ActionEvent(this, ActionEvent.ACTION_PERFORMED, "TO_REGISTER");
                    for (ActionListener l : toRegisterListeners) {
                        l.actionPerformed(event);
                    }
                });
            }

            stage.setOnCloseRequest(e -> {
                System.exit(0);
            });

        } catch (Exception e) {
            System.err.println("[LoginFXView] Lỗi khởi tạo JavaFX Login View: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    public String getHost() {
        return controller != null ? controller.getHost() : "localhost";
    }

    @Override
    public int getPort() {
        return controller != null ? controller.getPort() : 8888;
    }

    @Override
    public User getUser() {
        if (controller == null) return new User("", "");
        return new User(controller.getUsername(), controller.getPassword());
    }

    @Override
    public void showMessage(String msg) {
        Platform.runLater(() -> {
            if (controller != null) {
                controller.setError(msg);
            }
        });
    }

    @Override
    public void addLoginListener(ActionListener log) {
        if (log != null && !loginListeners.contains(log)) {
            loginListeners.add(log);
        }
    }

    @Override
    public void addToRegisterListener(ActionListener reg) {
        if (reg != null && !toRegisterListeners.contains(reg)) {
            toRegisterListeners.add(reg);
        }
    }

    @Override
    public void setVisible(boolean visible) {
        super.setVisible(false);
        Platform.runLater(() -> {
            if (stage != null) {
                if (visible) {
                    if (controller != null) {
                        controller.clearError();
                    }
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
}
