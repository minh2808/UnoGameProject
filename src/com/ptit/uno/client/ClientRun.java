package com.ptit.uno.client;

import com.ptit.uno.client.control.ClientControl;
import com.ptit.uno.client.view.LoginFrm;
import com.ptit.uno.client.view.fx.auth.LoginFXView;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;

/**
 * Lớp khởi động phía Client (ClientRun).
 * Bám sát Slide b05 (TS. Nguyễn Mạnh Hùng):
 * public class ClientRun {
 *     public static void main(String[] args) {
 *         ClientView view = new ClientView();
 *         ClientControl control = new ClientControl(view);
 *         view.setVisible(true);
 *     }
 * }
 */
public class ClientRun {
    public static void main(String[] args) {
        // Áp dụng giao diện hệ thống cho đẹp mắt
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {}

        SwingUtilities.invokeLater(() -> {
            // Sử dụng LoginFXView (Giao diện Dark Cyber cao cấp chuẩn mockup UNO Nhóm 6)
            LoginFrm loginFrm = new LoginFXView();
            new ClientControl(loginFrm);
            loginFrm.setVisible(true);
        });
    }
}
