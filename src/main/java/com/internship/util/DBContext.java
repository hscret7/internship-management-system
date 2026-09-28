package com.internship.util;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * DBContext: Cấu hình kết nối MySQL JDBC.
 * Phù hợp với thông số baseline: MySQL 8.0+, Driver MySQL Connector/J 9.7.0.
 */
public class DBContext {
    private static final String DRIVER = "com.mysql.cj.jdbc.Driver";
    
    // Mặc định hỗ trợ cả port 3306 và port 3307 (máy Docker của Huệ)
    private static final String DB_HOST = System.getenv("DB_HOST") != null ? System.getenv("DB_HOST") : "localhost";
    private static final String DB_PORT = System.getenv("DB_PORT") != null ? System.getenv("DB_PORT") : "3306";
    private static final String DB_NAME = System.getenv("DB_NAME") != null ? System.getenv("DB_NAME") : "internship_management";
    private static final String DB_USER = System.getenv("DB_USER") != null ? System.getenv("DB_USER") : "root";
    private static final String DB_PASS = System.getenv("DB_PASS") != null ? System.getenv("DB_PASS") : "root";

    private static final String URL = "jdbc:mysql://" + DB_HOST + ":" + DB_PORT + "/" + DB_NAME 
            + "?useSSL=false&serverTimezone=Asia/Ho_Chi_Minh&allowPublicKeyRetrieval=true&characterEncoding=UTF-8";

    static {
        try {
            Class.forName(DRIVER);
        } catch (ClassNotFoundException e) {
            System.err.println("Không tìm thấy MySQL Driver: " + e.getMessage());
        }
    }

    public static Connection getConnection() throws SQLException {
        try {
            return DBConnection.getConnection();
        } catch (Throwable t) {
            return DriverManager.getConnection(URL, DB_USER, DB_PASS);
        }
    }

    public static void close(AutoCloseable... closeables) {
        for (AutoCloseable c : closeables) {
            if (c != null) {
                try {
                    c.close();
                } catch (Exception ignored) {
                }
            }
        }
    }
}
