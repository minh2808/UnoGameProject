package com.ptit.uno.server.dao;

import com.ptit.uno.model.User;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * Lớp UserDAO xử lý các thao tác CSDL liên quan đến tài khoản người dùng.
 * Bám sát Slide b03 & b05 (sử dụng PreparedStatement chống SQL Injection).
 */
public class UserDAO extends DAO {

    public UserDAO() {
        super();
    }

    /**
     * Xác thực đăng nhập người dùng.
     */
    public User checkLogin(String username, String password) {
        Connection connection = getConnection();
        if (connection == null) {
            // Chế độ dự phòng khi chưa bật MySQL: cho phép đăng nhập test
            System.out.println("[UserDAO - Offline Mock] Đăng nhập giả lập: " + username);
            User mockUser = new User(username, password);
            mockUser.setId((int) (System.currentTimeMillis() % 10000));
            mockUser.setScore(1000);
            return mockUser;
        }

        String sql = "SELECT id, username, password, score, win_count, total_matches FROM `user` WHERE username = ? AND password = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, username);
            ps.setString(2, password);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    User user = new User();
                    user.setId(rs.getInt("id"));
                    user.setUsername(rs.getString("username"));
                    user.setPassword(rs.getString("password"));
                    user.setScore(rs.getInt("score"));
                    user.setWinMatches(rs.getInt("win_count"));
                    user.setTotalMatches(rs.getInt("total_matches"));
                    // Removed created_at since it's not in db_schema.sql
                    return user;
                }
            }
        } catch (Exception e) {
            System.err.println("[UserDAO] Lỗi checkLogin: " + e.getMessage());
        }
        return null;
    }

    /**
     * Đăng ký tài khoản người dùng mới.
     */
    public boolean register(User user) {
        Connection connection = getConnection();
        if (connection == null) {
            return true;
        }

        String checkSql = "SELECT id FROM `user` WHERE username = ?";
        String insertSql = "INSERT INTO `user`(username, password, score, win_count, total_matches) VALUES(?, ?, 1000, 0, 0)";
        try {
            try (PreparedStatement psCheck = connection.prepareStatement(checkSql)) {
                psCheck.setString(1, user.getUsername());
                try (ResultSet rs = psCheck.executeQuery()) {
                    if (rs.next()) {
                        return false; // Trùng tên tài khoản
                    }
                }
            }

            try (PreparedStatement psInsert = connection.prepareStatement(insertSql, Statement.RETURN_GENERATED_KEYS)) {
                psInsert.setString(1, user.getUsername());
                psInsert.setString(2, user.getPassword());
                int rows = psInsert.executeUpdate();
                if (rows > 0) {
                    try (ResultSet rsKey = psInsert.getGeneratedKeys()) {
                        if (rsKey.next()) {
                            user.setId(rsKey.getInt(1));
                        }
                    }
                    return true;
                }
            }
        } catch (Exception e) {
            System.err.println("[UserDAO] Lỗi register: " + e.getMessage());
        }
        return false;
    }

    /**
     * Lấy danh sách bảng xếp hạng theo điểm số giảm dần.
     */
    public List<User> getLeaderboard() {
        List<User> list = new ArrayList<User>();
        Connection connection = getConnection();
        if (connection == null) {
            list.add(new User(1, "player1", "***", 1250, 5, 8));
            list.add(new User(2, "player2", "***", 1100, 3, 6));
            list.add(new User(3, "player4", "***", 1050, 2, 5));
            return list;
        }

        String sql = "SELECT id, username, score, win_count, total_matches FROM `user` ORDER BY score DESC, win_count DESC LIMIT 20";
        try (Statement st = connection.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                User u = new User();
                u.setId(rs.getInt("id"));
                u.setUsername(rs.getString("username"));
                u.setScore(rs.getInt("score"));
                u.setWinMatches(rs.getInt("win_count"));
                u.setTotalMatches(rs.getInt("total_matches"));
                list.add(u);
            }
        } catch (Exception e) {
            System.err.println("[UserDAO] Lỗi getLeaderboard: " + e.getMessage());
        }
        return list;
    }

    /**
     * Cập nhật điểm số và thống kê thắng/thua sau ván đấu.
     */
    public void updateStats(int userId, int scoreChange, boolean isWinner) {
        Connection connection = getConnection();
        if (connection == null) return;

        String sql = "UPDATE `user` SET score = GREATEST(0, score + ?), total_matches = total_matches + 1, win_count = win_count + ? WHERE id = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, scoreChange);
            ps.setInt(2, isWinner ? 1 : 0);
            ps.setInt(3, userId);
            ps.executeUpdate();
        } catch (Exception e) {
            System.err.println("[UserDAO] Lỗi updateStats: " + e.getMessage());
        }
    }
}
