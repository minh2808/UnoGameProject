package com.ptit.uno.server.dao;

import com.ptit.uno.model.Player;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * MatchDAO lưu thông tin ván đấu và người tham gia (Quan hệ N-N chuẩn CSDL).
 * Bám sát Slide b03.
 */
public class MatchDAO extends DAO {

    public MatchDAO() {
        super();
    }

    /**
     * Lưu kết quả ván đấu vào `match` và `match_player`.
     */
    public int saveMatchResult(String roomName, int winnerUserId, int totalTurns,
                               List<Player> participants, Map<Integer, Integer> scoreChanges) {
        Connection connection = getConnection();
        if (connection == null) {
            System.out.println("[MatchDAO - Offline Mock] Lưu ván đấu ảo vào CSDL.");
            return 1;
        }

        String sqlMatch = "INSERT INTO `match`(room_name, winner_id, total_turns, start_time, end_time) VALUES(?, ?, ?, NOW(), NOW())";
        String sqlPart = "INSERT INTO `match_player`(match_id, user_id, score_change, cards_left, cards_detail, is_winner) VALUES(?, ?, ?, ?, ?, ?)";

        try {
            int matchId = -1;
            try (PreparedStatement psMatch = connection.prepareStatement(sqlMatch, Statement.RETURN_GENERATED_KEYS)) {
                psMatch.setString(1, roomName);
                if (winnerUserId > 0) {
                    psMatch.setInt(2, winnerUserId);
                } else {
                    psMatch.setNull(2, java.sql.Types.INTEGER);
                }
                psMatch.setInt(3, totalTurns);
                psMatch.executeUpdate();

                try (ResultSet rs = psMatch.getGeneratedKeys()) {
                    if (rs.next()) {
                        matchId = rs.getInt(1);
                    }
                }
            }

            if (matchId > 0 && participants != null) {
                try (PreparedStatement psPart = connection.prepareStatement(sqlPart)) {
                    for (Player p : participants) {
                        psPart.setInt(1, matchId);
                        psPart.setInt(2, p.getUserId());
                        int delta = scoreChanges != null && scoreChanges.containsKey(p.getUserId())
                                ? scoreChanges.get(p.getUserId()) : 0;
                        psPart.setInt(3, delta);
                        psPart.setInt(4, p.getCardCount());
                        psPart.setString(5, p.getHand() != null ? p.getHand().toString() : "");
                        psPart.setInt(6, p.getUserId() == winnerUserId ? 1 : 0);
                        psPart.addBatch();
                    }
                    psPart.executeBatch();
                }
            }
            return matchId;
        } catch (Exception e) {
            System.err.println("[MatchDAO] Lỗi saveMatchResult: " + e.getMessage());
        }
        return -1;
    }

    /**
     * Lấy lịch sử ván đấu của 1 người chơi.
     */
    public List<Map<String, Object>> getMatchHistory(int userId) {
        List<Map<String, Object>> list = new ArrayList<Map<String, Object>>();
        Connection connection = getConnection();
        if (connection == null) {
            Map<String, Object> mock = new HashMap<String, Object>();
            mock.put("matchId", 1);
            mock.put("roomName", "Phòng UNO VIP");
            mock.put("winnerName", "player1");
            mock.put("scoreChange", "+50");
            mock.put("endTime", "2026-09-21 10:00:00");
            list.add(mock);
            return list;
        }

        String sql = "SELECT m.id AS match_id, m.room_name, u.username AS winner_name, " +
                "mp.score_change, m.end_time " +
                "FROM `match` m " +
                "JOIN `match_player` mp ON m.id = mp.match_id " +
                "LEFT JOIN `user` u ON m.winner_id = u.id " +
                "WHERE mp.user_id = ? " +
                "ORDER BY m.id DESC LIMIT 20";

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> map = new HashMap<String, Object>();
                    map.put("matchId", rs.getInt("match_id"));
                    map.put("roomName", rs.getString("room_name"));
                    map.put("winnerName", rs.getString("winner_name") != null ? rs.getString("winner_name") : "Không rõ");
                    map.put("scoreChange", (rs.getInt("score_change") >= 0 ? "+" : "") + rs.getInt("score_change"));
                    map.put("endTime", rs.getString("end_time"));
                    list.add(map);
                }
            }
        } catch (Exception e) {
            System.err.println("[MatchDAO] Lỗi getMatchHistory: " + e.getMessage());
        }
        return list;
    }
}
