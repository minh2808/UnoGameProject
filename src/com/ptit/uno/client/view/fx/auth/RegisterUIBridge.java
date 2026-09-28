package com.ptit.uno.client.view.fx.auth;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

/**
 * RegisterUIBridge: Lớp cầu nối UI Code-Behind cho register_view.fxml.
 * Xử lý kiểm tra mật khẩu xác nhận realtime và chuyển giao sự kiện cho RegisterFXView.
 */
public class RegisterUIBridge {

    @FXML private TextField txtUsername;
    @FXML private PasswordField txtPassword;
    @FXML private PasswordField txtConfirmPassword;
    @FXML private Label lblError;
    @FXML private Button btnRegister;
    @FXML private Button btnBackToLogin;

    private Runnable onRegisterAction;
    private Runnable onBackAction;

    @FXML
    public void initialize() {
        if (txtConfirmPassword != null && txtPassword != null) {
            txtConfirmPassword.textProperty().addListener((obs, oldVal, newVal) -> checkPasswordMatch());
            txtPassword.textProperty().addListener((obs, oldVal, newVal) -> checkPasswordMatch());
        }

        if (btnRegister != null) {
            btnRegister.setOnAction(e -> {
                if (validateForm()) {
                    if (onRegisterAction != null) {
                        onRegisterAction.run();
                    }
                }
            });
        }

        if (btnBackToLogin != null) {
            btnBackToLogin.setOnAction(e -> {
                if (onBackAction != null) {
                    onBackAction.run();
                }
            });
        }
    }

    private boolean checkPasswordMatch() {
        String p1 = getPassword();
        String p2 = getConfirmPassword();

        if (p2.isEmpty()) {
            clearError();
            txtConfirmPassword.getStyleClass().remove("input-error");
            return true;
        }

        if (!p1.equals(p2)) {
            setError("Mật khẩu xác nhận không khớp!");
            if (!txtConfirmPassword.getStyleClass().contains("input-error")) {
                txtConfirmPassword.getStyleClass().add("input-error");
            }
            return false;
        } else {
            clearError();
            txtConfirmPassword.getStyleClass().remove("input-error");
            return true;
        }
    }

    private boolean validateForm() {
        String u = getUsername();
        String p1 = getPassword();
        String p2 = getConfirmPassword();

        if (u.isEmpty()) {
            setError("Vui lòng nhập tên đăng nhập!");
            return false;
        }

        if (p1.isEmpty()) {
            setError("Vui lòng nhập mật khẩu!");
            return false;
        }

        if (!p1.equals(p2)) {
            setError("Mật khẩu xác nhận không khớp!");
            return false;
        }

        clearError();
        return true;
    }

    public String getUsername() {
        return txtUsername != null ? txtUsername.getText().trim() : "";
    }

    public String getPassword() {
        return txtPassword != null ? txtPassword.getText() : "";
    }

    public String getConfirmPassword() {
        return txtConfirmPassword != null ? txtConfirmPassword.getText() : "";
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

    public void resetForm() {
        if (txtUsername != null) txtUsername.clear();
        if (txtPassword != null) txtPassword.clear();
        if (txtConfirmPassword != null) {
            txtConfirmPassword.clear();
            txtConfirmPassword.getStyleClass().remove("input-error");
        }
        clearError();
    }

    public void setOnRegisterAction(Runnable action) {
        this.onRegisterAction = action;
    }

    public void setOnBackAction(Runnable action) {
        this.onBackAction = action;
    }
}
