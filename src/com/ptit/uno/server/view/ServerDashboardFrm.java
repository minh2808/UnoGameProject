package com.ptit.uno.server.view;

import javax.swing.*;
import java.awt.*;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Giao diện Dashboard giám sát Server UNO (Java Swing).
 * Bám sát Slide b05.
 */
public class ServerDashboardFrm extends JFrame implements ServerView {
    private JTextArea txtLogs;
    private JLabel lblStatus;
    private JLabel lblClients;
    private JLabel lblRooms;
    private JLabel lblPort;

    public ServerDashboardFrm(int port) {
        super("UNO Game Server - Control Dashboard (PTIT LTM)");
        initComponents(port);
    }

    private void initComponents(int port) {
        this.setSize(750, 500);
        this.setLocationRelativeTo(null);
        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        this.setLayout(new BorderLayout(10, 10));

        // Panel thông tin trạng thái trên cùng
        JPanel topPanel = new JPanel(new GridLayout(1, 4, 10, 10));
        topPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder("Trạng thái máy chủ"),
                BorderFactory.createEmptyBorder(5, 5, 5, 5)
        ));

        lblStatus = new JLabel("Status: RUNNING", JLabel.CENTER);
        lblStatus.setForeground(new Color(0, 130, 0));
        lblStatus.setFont(new Font("Arial", Font.BOLD, 13));

        lblPort = new JLabel("Port: " + port, JLabel.CENTER);
        lblPort.setFont(new Font("Arial", Font.PLAIN, 13));

        lblClients = new JLabel("Clients: 0", JLabel.CENTER);
        lblClients.setFont(new Font("Arial", Font.BOLD, 13));

        lblRooms = new JLabel("Phòng chờ: 0", JLabel.CENTER);
        lblRooms.setFont(new Font("Arial", Font.PLAIN, 13));

        topPanel.add(lblStatus);
        topPanel.add(lblPort);
        topPanel.add(lblClients);
        topPanel.add(lblRooms);

        this.add(topPanel, BorderLayout.NORTH);

        // Vùng hiển thị Logs trung tâm
        txtLogs = new JTextArea();
        txtLogs.setEditable(false);
        txtLogs.setBackground(new Color(24, 24, 24));
        txtLogs.setForeground(new Color(200, 255, 200));
        txtLogs.setFont(new Font("Consolas", Font.PLAIN, 12));
        JScrollPane scrollLogs = new JScrollPane(txtLogs);
        scrollLogs.setBorder(BorderFactory.createTitledBorder("Nhật ký hoạt động mạng (Logs)"));

        this.add(scrollLogs, BorderLayout.CENTER);

        // Panel dưới cùng
        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton btnClear = new JButton("Xóa nhật ký");
        btnClear.addActionListener(e -> txtLogs.setText(""));
        bottomPanel.add(btnClear);

        this.add(bottomPanel, BorderLayout.SOUTH);
    }

    @Override
    public void showMessage(String msg) {
        SwingUtilities.invokeLater(() -> {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            String line = "[" + sdf.format(new Date()) + "] " + msg + "\n";
            txtLogs.append(line);
            txtLogs.setCaretPosition(txtLogs.getDocument().getLength());
            System.out.print(line);
        });
    }

    @Override
    public void updateClientCount(int count) {
        SwingUtilities.invokeLater(() -> lblClients.setText("Clients: " + count));
    }

    @Override
    public void updateRoomCount(int count) {
        SwingUtilities.invokeLater(() -> lblRooms.setText("Phòng chờ: " + count));
    }
}
