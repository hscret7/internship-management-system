package com.internship.util;

import java.sql.Connection;
import java.sql.SQLException;

/** Compatibility facade: all modules use one explicitly selected database. */
public final class DBContext {
    private DBContext() { }
    public static Connection getConnection() throws SQLException {
        return DBConnection.getConnection(); // Never retry against another URL/credentials.
    }
    public static void close(AutoCloseable... values) {
        for (AutoCloseable value : values) {
            if (value != null) try { value.close(); } catch (Exception ignored) { }
        }
    }
}
