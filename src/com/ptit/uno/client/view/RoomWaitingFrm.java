package com.ptit.uno.client.view;

import com.ptit.uno.model.ChatMessage;
import com.ptit.uno.model.Player;
import com.ptit.uno.model.Room;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;
import java.util.List;

/**
 * Giao diện Phòng chờ trước trận (RoomWaitingFrm): 4 vị trí ghế, Chat phòng, nút Bắt đầu.
 * Passive View chuẩn MVC Cải tiến.
 */
public class RoomWaitingFrm extends JFrame {
    private JLabel lblRoomInfo;
    private JPanel seatsPanel;
    private JLabel[] lblSeats;
    private JTextArea txtChat;
    private JTextField txtInputChat;
    private JButton btnSendChat;
    private JButton btnStartGame;
    private JButton btnLeaveRoom;

    public RoomWaitingFrm() {
        super("UNO Multiplayer - Phòng chờ");
        initComponents();
    }

    private void initComponents() {
        this.setSize(750, 500);
        this.setLocationRelativeTo(null);
        this.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);

        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Tiêu đề phòng
        lblRoomInfo = new JLabel("Phòng chờ: ...", JLabel.CENTER);
        lblRoomInfo.setFont(new Font("Arial", Font.BOLD, 16));
        lblRoomInfo.setForeground(new Color(20, 20, 120));
        mainPanel.add(lblRoomInfo, BorderLayout.NORTH);

        // Center: Chia 2 phần (Ghế ngồi bên trái, Chat bên phải)
        JPanel centerPanel = new JPanel(new GridLayout(1, 2, 10, 10));

        // 4 Ghế ngồi
        seatsPanel = new JPanel(new GridLayout(4, 1, 8, 8));
        seatsPanel.setBorder(BorderFactory.createTitledBorder("Danh sách người chơi trong phòng (2 - 4)"));
        lblSeats = new JLabel[4];
        for (int i = 0; i < 4; i++) {
            lblSeats[i] = new JLabel("Ghế #" + (i + 1) + ": [Trống]", JLabel.LEFT);
            lblSeats[i].setFont(new Font("Arial", Font.PLAIN, 14));
            lblSeats[i].setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(Color.LIGHT_GRAY),
                    BorderFactory.createEmptyBorder(5, 10, 5, 10)
            ));
            seatsPanel.add(lblSeats[i]);
        }
        centerPanel.add(seatsPanel);

        // Chat phòng
        JPanel chatPanel = new JPanel(new BorderLayout(5, 5));
        chatPanel.setBorder(BorderFactory.createTitledBorder("Trò chuyện trong phòng"));
        txtChat = new JTextArea();
        txtChat.setEditable(false);
        txtChat.setLineWrap(true);
        txtChat.setFont(new Font("Arial", Font.PLAIN, 12));
        chatPanel.add(new JScrollPane(txtChat), BorderLayout.CENTER);

        JPanel chatInputPanel = new JPanel(new BorderLayout(5, 0));
        txtInputChat = new JTextField();
        btnSendChat = new JButton("Gửi");
        chatInputPanel.add(txtInputChat, BorderLayout.CENTER);
        chatInputPanel.add(btnSendChat, BorderLayout.EAST);
        chatPanel.add(chatInputPanel, BorderLayout.SOUTH);

        centerPanel.add(chatPanel);
        mainPanel.add(centerPanel, BorderLayout.CENTER);

        // Nút bấm dưới cùng
        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 5));
        btnStartGame = new JButton("▶ BẮT ĐẦU VÁN ĐẤU");
        btnStartGame.setFont(new Font("Arial", Font.BOLD, 13));
        btnStartGame.setBackground(new Color(200, 30, 30));
        btnStartGame.setForeground(Color.WHITE);
        btnStartGame.setEnabled(false);

        btnLeaveRoom = new JButton("Rời phòng");
        bottomPanel.add(btnStartGame);
        bottomPanel.add(btnLeaveRoom);
        mainPanel.add(bottomPanel, BorderLayout.SOUTH);

        this.setContentPane(mainPanel);
    }

    public void renderRoom(Room room, int currentUserId) {
        lblRoomInfo.setText("Phòng #" + room.getId() + " - " + room.getName() + " (Chủ phòng: " + room.getHostName() + ")");

        List<Player> players = room.getPlayers();
        for (int i = 0; i < 4; i++) {
            if (players != null && i < players.size()) {
                Player p = players.get(i);
                boolean isHost = p.getUserId() == room.getHostId();
                lblSeats[i].setText("Ghế #" + (i + 1) + ": " + p.getUsername() + (isHost ? " 👑 (Chủ phòng)" : ""));
                lblSeats[i].setForeground(new Color(0, 100, 0));
            } else {
                lblSeats[i].setText("Ghế #" + (i + 1) + ": [Đang chờ người chơi...]");
                lblSeats[i].setForeground(Color.GRAY);
            }
        }

        // Chỉ chủ phòng mới được bấm bắt đầu và khi có từ 2 người trở lên
        boolean isHost = room.getHostId() == currentUserId;
        btnStartGame.setVisible(isHost);
        if (isHost) {
            if (room.getCurrentPlayerCount() >= 2) {
                btnStartGame.setEnabled(true);
                btnStartGame.setText("▶ BẮT ĐẦU VÁN ĐẤU (" + room.getCurrentPlayerCount() + " người)");
                btnStartGame.setBackground(new Color(34, 139, 34)); // Xanh lá cây nổi bật khi đã sẵn sàng
            } else {
                btnStartGame.setEnabled(false);
                btnStartGame.setText("▶ BẮT ĐẦU (Chờ người thứ 2 vào...)");
                btnStartGame.setBackground(new Color(150, 150, 150));
            }
        }
    }

    public void appendChatMessage(ChatMessage msg) {
        txtChat.append(msg.toString() + "\n");
        txtChat.setCaretPosition(txtChat.getDocument().getLength());
    }

    public String getChatMessage() {
        String t = txtInputChat.getText().trim();
        txtInputChat.setText("");
        return t;
    }

    public void showMessage(String msg) {
        JOptionPane.showMessageDialog(this, msg);
    }

    // --- BỘ ĐĂNG KÝ LISTENER ---
    public void addStartGameListener(ActionListener log) {
        btnStartGame.addActionListener(log);
    }

    public void addLeaveRoomListener(ActionListener log) {
        btnLeaveRoom.addActionListener(log);
    }

    public void addSendChatListener(ActionListener log) {
        btnSendChat.addActionListener(log);
        txtInputChat.addActionListener(log);
    }
}
