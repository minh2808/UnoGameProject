package com.ptit.uno.client.view;

import com.ptit.uno.model.User;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;

/**
 * Giao diện Bảng xếp hạng người chơi (LeaderboardFrm).
 * Passive View chuẩn MVC Cải tiến.
 */
public class LeaderboardFrm extends JFrame {
    private JTable tblLeaderboard;
    private DefaultTableModel tableModel;

    public LeaderboardFrm() {
        super("BẢNG XẾP HẠNG CAO THỦ UNO");
        initComponents();
    }

    private void initComponents() {
        this.setSize(550, 450);
        this.setLocationRelativeTo(null);
        this.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 15, 10, 15));

        JLabel lblTitle = new JLabel("🏆 TOP CAO THỦ UNO ONLINE", JLabel.CENTER);
        lblTitle.setFont(new Font("Arial", Font.BOLD, 18));
        lblTitle.setForeground(new Color(180, 120, 0));
        mainPanel.add(lblTitle, BorderLayout.NORTH);

        tableModel = new DefaultTableModel(new String[]{"Hạng", "Tên người chơi", "Điểm Elo", "Trận thắng", "Tổng trận"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        tblLeaderboard = new JTable(tableModel);
        tblLeaderboard.setRowHeight(24);
        mainPanel.add(new JScrollPane(tblLeaderboard), BorderLayout.CENTER);

        JButton btnClose = new JButton("Đóng");
        btnClose.addActionListener(e -> this.dispose());
        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        bottomPanel.add(btnClose);
        mainPanel.add(bottomPanel, BorderLayout.SOUTH);

        this.setContentPane(mainPanel);
    }

    public void renderLeaderboard(List<User> list) {
        tableModel.setRowCount(0);
        if (list != null) {
            int rank = 1;
            for (User u : list) {
                tableModel.addRow(new Object[]{
                        rank++,
                        u.getUsername(),
                        u.getScore(),
                        u.getWinMatches(),
                        u.getTotalMatches()
                });
            }
        }
    }
}
