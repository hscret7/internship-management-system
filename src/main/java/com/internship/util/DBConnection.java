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