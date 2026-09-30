package com.ptit.uno.server.dao;

import java.sql.Connection;
import java.sql.DriverManager;

/**
 * Lớp DAO cơ sở quản lý kết nối CSDL MySQL bằng JDBC.
 * Bám sát Slide b03 (TS. Nguyễn Mạnh Hùng).
 */
public class DAO {
    protected static Connection con;

    private static final String DB_URL = "jdbc:mysql://localhost:3306/unodb?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC&characterEncoding=UTF-8";
    private static final String DB_USER = "root";
    private static final String DB_PASSWORD = "28082005"; // Mặc định MySQL local

    public DAO() {
        getConnection();
    }

    public static synchronized Connection getConnection() {
        try {
            if (con == null || con.isClosed()) {
                // Hỗ trợ cả driver cũ (com.mysql.jdbc.Driver) và driver mới (com.mysql.cj.jdbc.Driver)
                try {
                    Class.forName("com.mysql.cj.jdbc.Driver");
                } catch (ClassNotFoundException ex) {
                    Class.forName("com.mysql.jdbc.Driver");
                }
                con = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
            }
        } catch (Exception e) {
            System.err.println("[DAO] Cảnh báo kết nối CSDL: " + e.getMessage());
        }
        return con;
    }

    public static void closeConnection() {
        try {
            if (con != null && !con.isClosed()) {
                con.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
