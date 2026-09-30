package com.ptit.uno.client.view;

import com.ptit.uno.model.Room;
import com.ptit.uno.model.User;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionListener;
import java.util.List;

/**
 * Giao diện Sảnh chờ (LobbyFrm): Xem danh sách phòng, người chơi online, tạo phòng, xem BXH.
 * Passive View chuẩn MVC Cải tiến.
 */
public class LobbyFrm extends JFrame {
    private JLabel lblUserInfo;
    private JTable tblRooms;
    private DefaultTableModel roomModel;
    private JTable tblOnline;
    private DefaultTableModel onlineModel;

    private JButton btnCreateRoom;
    private JButton btnJoinRoom;
    private JButton btnRefresh;
    private JButton btnLeaderboard;
    private JButton btnHistory;
    private JButton btnLogout;

    public LobbyFrm() {
        super("UNO Multiplayer - Sảnh chính (Lobby)");
        initComponents();
    }

    private void initComponents() {
        this.setSize(900, 600);
        this.setLocationRelativeTo(null);
        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Header: Thông tin người dùng
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(200, 200, 200)),
                BorderFactory.createEmptyBorder(8, 12, 8, 12)
        ));
        lblUserInfo = new JLabel("Người chơi: Đang tải... | Điểm: 1000");
        lblUserInfo.setFont(new Font("Arial", Font.BOLD, 14));
        lblUserInfo.setForeground(new Color(20, 100, 20));

        JPanel headerButtons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 0));
        btnLeaderboard = new JButton("🏆 Bảng xếp hạng");
        btnHistory = new JButton("📜 Lịch sử đấu");
        btnLogout = new JButton("Đăng xuất");
        btnLogout.setForeground(Color.RED);

        headerButtons.add(btnLeaderboard);
        headerButtons.add(btnHistory);
        headerButtons.add(btnLogout);

        headerPanel.add(lblUserInfo, BorderLayout.WEST);
        headerPanel.add(headerButtons, BorderLayout.EAST);
        mainPanel.add(headerPanel, BorderLayout.NORTH);

        // Center: Chia 2 cột (Danh sách phòng ở giữa, danh sách Online bên phải)
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        splitPane.setResizeWeight(0.7);

        // Panel phòng chơi
        JPanel roomPanel = new JPanel(new BorderLayout(5, 5));
        roomPanel.setBorder(BorderFactory.createTitledBorder("Danh sách phòng chơi"));

        roomModel = new DefaultTableModel(new String[]{"ID", "Tên phòng", "Chủ phòng", "Số người", "Trạng thái"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        tblRooms = new JTable(roomModel);
        tblRooms.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tblRooms.setRowHeight(24);
        roomPanel.add(new JScrollPane(tblRooms), BorderLayout.CENTER);

        JPanel roomButtons = new JPanel(new FlowLayout(FlowLayout.LEFT));
        btnCreateRoom = new JButton("➕ Tạo phòng mới");
        btnCreateRoom.setBackground(new Color(40, 140, 40));
        btnCreateRoom.setForeground(Color.WHITE);
        btnCreateRoom.setFont(new Font("Arial", Font.BOLD, 12));

        btnJoinRoom = new JButton("Vào phòng đã chọn");
        btnJoinRoom.setFont(new Font("Arial", Font.BOLD, 12));

        btnRefresh = new JButton("🔄 Làm mới");
        roomButtons.add(btnCreateRoom);
        roomButtons.add(btnJoinRoom);
        roomButtons.add(btnRefresh);
        roomPanel.add(roomButtons, BorderLayout.SOUTH);

        splitPane.setLeftComponent(roomPanel);

        // Panel danh sách người chơi online
        JPanel onlinePanel = new JPanel(new BorderLayout(5, 5));
        onlinePanel.setBorder(BorderFactory.createTitledBorder("Người chơi trực tuyến"));
        onlineModel = new DefaultTableModel(new String[]{"ID", "Tên người chơi", "Điểm số"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        tblOnline = new JTable(onlineModel);
        tblOnline.setRowHeight(22);
        onlinePanel.add(new JScrollPane(tblOnline), BorderLayout.CENTER);

        splitPane.setRightComponent(onlinePanel);
        mainPanel.add(splitPane, BorderLayout.CENTER);

        this.setContentPane(mainPanel);
    }

    public void setUserInfo(User user) {
        lblUserInfo.setText("Xin chào: " + user.getUsername() + " | Điểm Elo: " + user.getScore() +
                " | Thắng: " + user.getWinMatches() + "/" + user.getTotalMatches());
    }

    public void renderRooms(List<Room> rooms) {
        roomModel.setRowCount(0);
        if (rooms != null) {
            for (Room r : rooms) {
                roomModel.addRow(new Object[]{
                        r.getId(),
                        r.getName(),
                        r.getHostName(),
                        r.getCurrentPlayerCount() + "/" + r.getMaxPlayers(),
                        r.getStatus()
                });
            }
        }
    }

    public void renderOnlineUsers(List<User> users) {
        onlineModel.setRowCount(0);
        if (users != null) {
            for (User u : users) {
                onlineModel.addRow(new Object[]{u.getId(), u.getUsername(), u.getScore()});
            }
        }
    }

    public Integer getSelectedRoomId() {
        int row = tblRooms.getSelectedRow();
        if (row != -1) {
            return (Integer) roomModel.getValueAt(row, 0);
        }
        return null;
    }

    public void showMessage(String msg) {
        JOptionPane.showMessageDialog(this, msg);
    }

    public String promptCreateRoomName() {
        return JOptionPane.showInputDialog(this, "Nhập tên phòng mới:", "Tạo phòng UNO", JOptionPane.PLAIN_MESSAGE);
    }

    // --- BỘ ĐĂNG KÝ LISTENER ĐỂ CONTROLLER CẮM VÀO ---
    public void addCreateRoomListener(ActionListener log) {
        btnCreateRoom.addActionListener(log);
    }

    public void addJoinRoomListener(ActionListener log) {
        btnJoinRoom.addActionListener(log);
    }

    public void addRefreshListener(ActionListener log) {
        btnRefresh.addActionListener(log);
    }

    public void addLeaderboardListener(ActionListener log) {
        btnLeaderboard.addActionListener(log);
    }

    public void addHistoryListener(ActionListener log) {
        btnHistory.addActionListener(log);
    }

    public void addLogoutListener(ActionListener log) {
        btnLogout.addActionListener(log);
    }
}
