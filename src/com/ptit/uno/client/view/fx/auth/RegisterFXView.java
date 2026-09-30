package com.ptit.uno.client.view.fx.auth;

import com.ptit.uno.client.view.RegisterFrm;
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
 * RegisterFXView: Cầu nối View chuẩn MVC giữa ClientControl và giao diện Đăng ký JavaFX.
 * Kế thừa RegisterFrm để tương thích hoàn toàn với ClientControl.
 */
public class RegisterFXView extends RegisterFrm {
    private Stage stage;
    private RegisterUIBridge controller;
    private final List<ActionListener> registerListeners = new ArrayList<>();
    private final List<ActionListener> backListeners = new ArrayList<>();

    public RegisterFXView() {
        super.setVisible(false);

        try {
            Platform.startup(() -> {});
        } catch (IllegalStateException ignored) {}

        Platform.runLater(this::initFX);
    }

    private void initFX() {
        try {
            URL fxmlUrl = getClass().getResource("/resource/fxml/register_view.fxml");
            if (fxmlUrl == null) {
                fxmlUrl = getClass().getResource("/fxml/register_view.fxml");
            }
            if (fxmlUrl == null) {
                File f = new File("src/resource/fxml/register_view.fxml");
                if (f.exists()) {
                    fxmlUrl = f.toURI().toURL();
                }
            }

            if (fxmlUrl == null) {
                System.err.println("[RegisterFXView] Không tìm thấy file register_view.fxml!");
                return;
            }

            FXMLLoader loader = new FXMLLoader(fxmlUrl);
            Parent root = loader.load();
            this.controller = loader.getController();

            stage = new Stage();
            stage.setTitle("UNO Multiplayer - Đăng ký tài khoản (PTIT LTM)");
            Scene scene = new Scene(root, 960, 580);
            scene.setFill(Color.TRANSPARENT);
            stage.setScene(scene);
            stage.setMinWidth(900);
            stage.setMinHeight(550);
            stage.centerOnScreen();

            if (controller != null) {
                controller.setOnRegisterAction(() -> {
                    ActionEvent event = new ActionEvent(this, ActionEvent.ACTION_PERFORMED, "REGISTER");
                    for (ActionListener l : registerListeners) {
                        l.actionPerformed(event);
                    }
                });

                controller.setOnBackAction(() -> {
                    ActionEvent event = new ActionEvent(this, ActionEvent.ACTION_PERFORMED, "BACK");
                    for (ActionListener l : backListeners) {
                        l.actionPerformed(event);
                    }
                });
            }

            stage.setOnCloseRequest(e -> {
                e.consume();
                setVisible(false);
                ActionEvent event = new ActionEvent(this, ActionEvent.ACTION_PERFORMED, "BACK");
                for (ActionListener l : backListeners) {
                    l.actionPerformed(event);
                }
            });

        } catch (Exception e) {
            System.err.println("[RegisterFXView] Lỗi khởi tạo JavaFX Register View: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    public User getUser() {
        if (controller == null) return null;
        String pass = controller.getPassword();
        String confirm = controller.getConfirmPassword();
        if (!pass.equals(confirm)) {
            showMessage("Mật khẩu xác nhận không khớp!");
            return null;
        }
        return new User(controller.getUsername(), pass);
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
    public void addRegisterListener(ActionListener log) {
        if (log != null && !registerListeners.contains(log)) {
            registerListeners.add(log);
        }
    }

    @Override
    public void addBackListener(ActionListener log) {
        if (log != null && !backListeners.contains(log)) {
            backListeners.add(log);
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
