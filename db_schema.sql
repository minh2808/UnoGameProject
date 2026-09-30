-- ====================================================================
-- DATABASE SCHEMA: GAME BÀI UNO MULTIPLAYER ONLINE (PTIT LTM)
-- Cập nhật cấu trúc theo Báo cáo thiết kế
-- ====================================================================

CREATE DATABASE IF NOT EXISTS unodb CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE unodb;

-- 1. Bảng người dùng (user)
DROP TABLE IF EXISTS match_action;
DROP TABLE IF EXISTS match_player;
DROP TABLE IF EXISTS `match`;
DROP TABLE IF EXISTS `user`;

CREATE TABLE `user` (
    id INT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    score INT DEFAULT 0,
    win_count INT DEFAULT 0,
    total_matches INT DEFAULT 0,
    status VARCHAR(20) DEFAULT 'OFFLINE'
);

-- 2. Bảng thông tin tổng quan ván đấu (match)
CREATE TABLE `match` (
    id INT AUTO_INCREMENT PRIMARY KEY,
    room_name VARCHAR(50) NOT NULL,
    winner_id INT,
    start_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    end_time DATETIME NULL,
    total_turns INT DEFAULT 0,
    FOREIGN KEY (winner_id) REFERENCES `user`(id) ON DELETE SET NULL
);

-- 3. Bảng chi tiết kết quả người tham gia (match_player)
CREATE TABLE match_player (
    id INT AUTO_INCREMENT PRIMARY KEY,
    match_id INT NOT NULL,
    user_id INT NOT NULL,
    score_change INT DEFAULT 0,
    cards_left INT DEFAULT 0,
    cards_detail TEXT NULL,
    is_winner TINYINT(1) DEFAULT 0,
    FOREIGN KEY (match_id) REFERENCES `match`(id) ON DELETE CASCADE,
    FOREIGN KEY (user_id) REFERENCES `user`(id) ON DELETE CASCADE
);

-- 4. Bảng lưu vết hành động ván đấu & Minh bạch điểm số (match_action)
CREATE TABLE match_action (
    id INT AUTO_INCREMENT PRIMARY KEY,
    match_id INT NOT NULL,
    user_id INT NOT NULL,
    turn_number INT NOT NULL,
    action_type VARCHAR(30) NOT NULL,
    card_played VARCHAR(30) NULL,
    cards_count_after INT NOT NULL,
    score_impact INT DEFAULT 0,
    description TEXT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (match_id) REFERENCES `match`(id) ON DELETE CASCADE,
    FOREIGN KEY (user_id) REFERENCES `user`(id) ON DELETE CASCADE
);

-- ====================================================================
-- DỮ LIỆU MẪU ĐỂ TEST HỆ THỐNG
-- ====================================================================
INSERT INTO `user` (username, password, score, win_count, total_matches, status) VALUES
('player1', '123456', 1250, 5, 8, 'OFFLINE'),
('player2', '123456', 1100, 3, 6, 'OFFLINE'),
('player3', '123456', 950, 1, 4, 'OFFLINE'),
('player4', '123456', 1050, 2, 5, 'OFFLINE'),
('admin', 'admin123', 1500, 10, 12, 'OFFLINE');
