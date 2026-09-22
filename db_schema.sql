-- ====================================================================
-- DATABASE SCHEMA: GAME BÀI UNO MULTIPLAYER ONLINE (PTIT LTM)
-- ====================================================================

CREATE DATABASE IF NOT EXISTS unodb CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE unodb;

-- 1. Bảng người dùng (tbl_user)
DROP TABLE IF EXISTS tbl_match_participant;
DROP TABLE IF EXISTS tbl_match;
DROP TABLE IF EXISTS tbl_friend;
DROP TABLE IF EXISTS tbl_user;

CREATE TABLE tbl_user (
    id INT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    score INT DEFAULT 1000,
    win_matches INT DEFAULT 0,
    total_matches INT DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 2. Bảng thông tin ván đấu (tbl_match)
CREATE TABLE tbl_match (
    id INT AUTO_INCREMENT PRIMARY KEY,
    room_name VARCHAR(50),
    winner_id INT,
    total_turns INT DEFAULT 0,
    start_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    end_time TIMESTAMP NULL,
    FOREIGN KEY (winner_id) REFERENCES tbl_user(id) ON DELETE SET NULL
);

-- 3. Bảng chi tiết người tham gia ván đấu (tbl_match_participant - Quan hệ N-N)
CREATE TABLE tbl_match_participant (
    id INT AUTO_INCREMENT PRIMARY KEY,
    match_id INT NOT NULL,
    user_id INT NOT NULL,
    seat_number INT NOT NULL,
    score_change INT DEFAULT 0,
    is_bot_played BOOLEAN DEFAULT FALSE,
    FOREIGN KEY (match_id) REFERENCES tbl_match(id) ON DELETE CASCADE,
    FOREIGN KEY (user_id) REFERENCES tbl_user(id) ON DELETE CASCADE
);

-- 4. Bảng bạn bè (tbl_friend)
CREATE TABLE tbl_friend (
    id INT AUTO_INCREMENT PRIMARY KEY,
    user_id INT NOT NULL,
    friend_id INT NOT NULL,
    status VARCHAR(20) DEFAULT 'ACCEPTED',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES tbl_user(id) ON DELETE CASCADE,
    FOREIGN KEY (friend_id) REFERENCES tbl_user(id) ON DELETE CASCADE
);

-- ====================================================================
-- DỮ LIỆU MẪU ĐỂ TEST HỆ THỐNG
-- ====================================================================
INSERT INTO tbl_user (username, password, score, win_matches, total_matches) VALUES
('player1', '123456', 1250, 5, 8),
('player2', '123456', 1100, 3, 6),
('player3', '123456', 950, 1, 4),
('player4', '123456', 1050, 2, 5),
('admin', 'admin123', 1500, 10, 12);
