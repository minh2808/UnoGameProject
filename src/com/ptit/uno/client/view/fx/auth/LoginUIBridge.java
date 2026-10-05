package com.ptit.uno.client.view.fx.auth;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

/**
 * LoginUIBridge: Lớp cầu nối UI Code-Behind cho login_view.fxml.
 * Đóng vai trò là Presentation Helper thuần túy của tầng View,
 * ánh xạ các component FXML và chuyển giao sự kiện cho LoginFXView.
 */
public class LoginUIBridge {

    @FXML
    private TextField txtHost;
    @FXML
    private TextField txtPort;
    @FXML
    private TextField txtUsername;
    @FXML
    private PasswordField txtPassword;
    @FXML
    private Label lblError;
    @FXML
    private Button btnLogin;
    @FXML
    private Button btnToRegister;

    private Runnable onLoginAction;
    private Runnable onToRegisterAction;

    @FXML
    public void initialize() {
        if (btnLogin != null) {
            btnLogin.setOnAction(e -> {
                if (validateInput()) {
                    if (onLoginAction != null) {
                        onLoginAction.run();
                    }
                }
            });
        }

        if (btnToRegister != null) {
            btnToRegister.setOnAction(e -> {
                if (onToRegisterAction != null) {
                    onToRegisterAction.run();
                }
            });
        }
    }

    private boolean validateInput() {
        String u = getUsername();
        String p = getPassword();

        if (u.isEmpty() || p.isEmpty()) {
            setError("Vui lòng nhập đầy đủ tên đăng nhập và mật khẩu!");
            return false;
        }

        try {
            int port = getPort();
            if (port <= 0 || port > 65535) {
                setError("Cổng Port phải là số trong khoảng 1 - 65535!");
                return false;
            }
        } catch (Exception e) {
            setError("Port không hợp lệ!");
            return false;
        }

        clearError();
        return true;
    }

    public String getHost() {
        return txtHost != null ? txtHost.getText().trim() : "localhost";
    }

    public int getPort() {
        try {
            return txtPort != null ? Integer.parseInt(txtPort.getText().trim()) : 8888;
        } catch (NumberFormatException e) {
            return 8888;
        }
    }

    public String getUsername() {
        return txtUsername != null ? txtUsername.getText().trim() : "";
    }

    public String getPassword() {
        return txtPassword != null ? txtPassword.getText() : "";
    }

    public void setError(String msg) {
        if (lblError != null) {
            lblError.setText(msg);
        }
    }

    public void clearError() {
        if (lblError != null) {
            lblError.setText("");
        }
    }

    public void setOnLoginAction(Runnable action) {
        this.onLoginAction = action;
    }

    public void setOnToRegisterAction(Runnable action) {
        this.onToRegisterAction = action;
    }
}
