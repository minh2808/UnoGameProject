package com.ptit.uno.client.view;

import com.ptit.uno.model.User;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;

/**
 * Giao diện đăng nhập (LoginFrm) thiết kế theo mô hình MVC Cải tiến.
 * View hoàn toàn thụ động (Passive View): Chỉ hiển thị, cung cấp hàm getter và đăng ký listener.
 * Bám sát Slide b02-2 & b05.
 */
public class LoginFrm extends JFrame {
    private JTextField txtHost;
    private JTextField txtPort;
    private JTextField txtUsername;
    private JPasswordField txtPassword;
    private JButton btnLogin;
    private JButton btnToRegister;

    public LoginFrm() {
        super("UNO Multiplayer - Đăng nhập (PTIT LTM)");
        initComponents();
    }

    private void initComponents() {
        this.setSize(400, 320);
        this.setLocationRelativeTo(null);
        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        this.setResizable(false);

        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(15, 20, 15, 20));

        // Tiêu đề
        JLabel lblTitle = new JLabel("GAME BÀI UNO ONLINE", JLabel.CENTER);
        lblTitle.setFont(new Font("Arial", Font.BOLD, 20));
        lblTitle.setForeground(new Color(200, 30, 30));
        mainPanel.add(lblTitle, BorderLayout.NORTH);

        // Form nhập liệu
        JPanel formPanel = new JPanel(new GridLayout(4, 2, 10, 10));

        formPanel.add(new JLabel("Server Host:"));
        txtHost = new JTextField("localhost");
        formPanel.add(txtHost);

        formPanel.add(new JLabel("Server Port:"));
        txtPort = new JTextField("8888");
        formPanel.add(txtPort);

        formPanel.add(new JLabel("Tên đăng nhập:"));
        txtUsername = new JTextField("player1");
        formPanel.add(txtUsername);

        formPanel.add(new JLabel("Mật khẩu:"));
        txtPassword = new JPasswordField("123456");
        formPanel.add(txtPassword);

        mainPanel.add(formPanel, BorderLayout.CENTER);

        // Các nút bấm
        JPanel buttonPanel = new JPanel(new GridLayout(1, 2, 10, 0));
        btnLogin = new JButton("Đăng nhập");
        btnLogin.setFont(new Font("Arial", Font.BOLD, 13));
        btnLogin.setBackground(new Color(40, 140, 40));
        btnLogin.setForeground(Color.WHITE);

        btnToRegister = new JButton("Chưa có tài khoản?");
        buttonPanel.add(btnLogin);
        buttonPanel.add(btnToRegister);

        mainPanel.add(buttonPanel, BorderLayout.SOUTH);
        this.setContentPane(mainPanel);
    }

    // --- CÁC HÀM GETTER DỮ LIỆU ĐỂ CONTROLLER LẤY RA (Chuẩn MVC Cải tiến) ---
    public String getHost() {
        return txtHost.getText().trim();
    }

    public int getPort() {
        try {
            return Integer.parseInt(txtPort.getText().trim());
        } catch (NumberFormatException e) {
            return 8888;
        }
    }

    public User getUser() {
        return new User(txtUsername.getText().trim(), new String(txtPassword.getPassword()));
    }

    public void showMessage(String msg) {
        JOptionPane.showMessageDialog(this, msg);
    }

    // --- CÁC HÀM ĐĂNG KÝ LISTENER ĐỂ CONTROLLER CẮM VÀO (Chuẩn MVC Cải tiến) ---
    public void addLoginListener(ActionListener log) {
        btnLogin.addActionListener(log);
    }

    public void addToRegisterListener(ActionListener log) {
        btnToRegister.addActionListener(log);
    }
}
