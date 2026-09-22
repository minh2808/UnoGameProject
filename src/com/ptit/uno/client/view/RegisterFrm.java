package com.ptit.uno.client.view;

import com.ptit.uno.model.User;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;

/**
 * Giao diện đăng ký tài khoản mới (RegisterFrm).
 * Passive View chuẩn MVC Cải tiến.
 */
public class RegisterFrm extends JFrame {
    private JTextField txtUsername;
    private JPasswordField txtPassword;
    private JPasswordField txtConfirmPassword;
    private JButton btnRegister;
    private JButton btnBackToLogin;

    public RegisterFrm() {
        super("UNO Multiplayer - Đăng ký tài khoản");
        initComponents();
    }

    private void initComponents() {
        this.setSize(400, 300);
        this.setLocationRelativeTo(null);
        this.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        this.setResizable(false);

        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(15, 20, 15, 20));

        JLabel lblTitle = new JLabel("ĐĂNG KÝ TÀI KHOẢN MỚI", JLabel.CENTER);
        lblTitle.setFont(new Font("Arial", Font.BOLD, 18));
        lblTitle.setForeground(new Color(30, 90, 200));
        mainPanel.add(lblTitle, BorderLayout.NORTH);

        JPanel formPanel = new JPanel(new GridLayout(3, 2, 10, 10));
        formPanel.add(new JLabel("Tên đăng nhập:"));
        txtUsername = new JTextField();
        formPanel.add(txtUsername);

        formPanel.add(new JLabel("Mật khẩu:"));
        txtPassword = new JPasswordField();
        formPanel.add(txtPassword);

        formPanel.add(new JLabel("Xác nhận mật khẩu:"));
        txtConfirmPassword = new JPasswordField();
        formPanel.add(txtConfirmPassword);

        mainPanel.add(formPanel, BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel(new GridLayout(1, 2, 10, 0));
        btnRegister = new JButton("Đăng ký");
        btnRegister.setFont(new Font("Arial", Font.BOLD, 13));
        btnRegister.setBackground(new Color(30, 90, 200));
        btnRegister.setForeground(Color.WHITE);

        btnBackToLogin = new JButton("Quay lại");
        buttonPanel.add(btnRegister);
        buttonPanel.add(btnBackToLogin);

        mainPanel.add(buttonPanel, BorderLayout.SOUTH);
        this.setContentPane(mainPanel);
    }

    public User getUser() {
        String pass = new String(txtPassword.getPassword());
        String confirm = new String(txtConfirmPassword.getPassword());
        if (!pass.equals(confirm)) {
            showMessage("Mật khẩu xác nhận không khớp!");
            return null;
        }
        return new User(txtUsername.getText().trim(), pass);
    }

    public void showMessage(String msg) {
        JOptionPane.showMessageDialog(this, msg);
    }

    public void addRegisterListener(ActionListener log) {
        btnRegister.addActionListener(log);
    }

    public void addBackListener(ActionListener log) {
        btnBackToLogin.addActionListener(log);
    }
}
