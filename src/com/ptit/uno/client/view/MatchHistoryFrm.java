package com.ptit.uno.client.view;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;
import java.util.Map;

/**
 * Giao diện Lịch sử ván đấu của người chơi (MatchHistoryFrm).
 * Passive View chuẩn MVC Cải tiến.
 */
public class MatchHistoryFrm extends JFrame {
    private JTable tblHistory;
    private DefaultTableModel tableModel;

    public MatchHistoryFrm() {
        super("LỊCH SỬ CÁC VÁN ĐẤU");
        initComponents();
    }

    private void initComponents() {
        this.setSize(650, 450);
        this.setLocationRelativeTo(null);
        this.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 15, 10, 15));

        JLabel lblTitle = new JLabel("📜 LỊCH SỬ THI ĐẤU GẦN ĐÂY", JLabel.CENTER);
        lblTitle.setFont(new Font("Arial", Font.BOLD, 18));
        lblTitle.setForeground(new Color(20, 90, 160));
        mainPanel.add(lblTitle, BorderLayout.NORTH);

        tableModel = new DefaultTableModel(new String[]{"Mã ván", "Tên phòng", "Người thắng", "Biến động điểm", "Thời gian"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        tblHistory = new JTable(tableModel);
        tblHistory.setRowHeight(24);
        mainPanel.add(new JScrollPane(tblHistory), BorderLayout.CENTER);

        JButton btnClose = new JButton("Đóng");
        btnClose.addActionListener(e -> this.dispose());
        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        bottomPanel.add(btnClose);
        mainPanel.add(bottomPanel, BorderLayout.SOUTH);

        this.setContentPane(mainPanel);
    }

    public void renderHistory(List<Map<String, Object>> list) {
        tableModel.setRowCount(0);
        if (list != null) {
            for (Map<String, Object> map : list) {
                tableModel.addRow(new Object[]{
                        map.get("matchId"),
                        map.get("roomName"),
                        map.get("winnerName"),
                        map.get("scoreChange"),
                        map.get("endTime")
                });
            }
        }
    }
}
