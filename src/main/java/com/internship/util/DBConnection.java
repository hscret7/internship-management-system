package com.internship.util;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

public final class DBConnection {

    private static final String PROPERTIES_FILE = "db.properties";

    private static String url;
    private static String username;
    private static String password;

    private DBConnection() {
    }

    static {
        loadProperties();
    }

    private static void loadProperties() {
        // Choose one environment configuration before connecting; never fallback on failure.
        if (System.getenv("DB_URL") != null || System.getenv("DB_NAME") != null) {
            url = System.getenv("DB_URL");
            if (url == null) url = "jdbc:mysql://" + System.getenv().getOrDefault("DB_HOST", "localhost")
                    + ":" + System.getenv().getOrDefault("DB_PORT", "3306") + "/" + System.getenv("DB_NAME")
                    + "?characterEncoding=UTF-8&connectionTimeZone=Asia/Ho_Chi_Minh";
            username = System.getenv("DB_USER"); password = System.getenv("DB_PASS");
            if (username == null || password == null) throw new IllegalStateException("DB_USER and DB_PASS are required with DB_URL/DB_NAME");
            try { Class.forName("com.mysql.cj.jdbc.Driver"); } catch (ClassNotFoundException e) { throw new ExceptionInInitializerError(e); }
            return;
        }
        Properties properties = new Properties();

        try (InputStream input = DBConnection.class
                .getClassLoader()
                .getResourceAsStream(PROPERTIES_FILE)) {

            if (input == null) {
                throw new IllegalStateException(
                        "Không tìm thấy file " + PROPERTIES_FILE
                );
            }

            properties.load(
                new InputStreamReader(input, StandardCharsets.UTF_8)
            );

            url = properties.getProperty("db.url");
            username = properties.getProperty("db.username");
            password = properties.getProperty("db.password");

            String driver = properties.getProperty("db.driver");

            if (url == null || username == null ||
                    password == null || driver == null) {
                throw new IllegalStateException(
                        "Thiếu cấu hình database trong " + PROPERTIES_FILE
                );
            }

            Class.forName(driver);

        } catch (IOException | ClassNotFoundException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(
                url,
                username,
                password
        );
    }
}
