package com.internship.util;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * Compatibility entry point for BE2 DAOs. All modules use BE1's database
 * configuration so one deployment cannot silently write to two databases.
 */
public final class DBContext {
    private DBContext() {
    }

    public static Connection getConnection() throws SQLException {
        return DBConnection.getConnection();
    }

    public static void close(AutoCloseable... closeables) {
        for (AutoCloseable closeable : closeables) {
            if (closeable != null) {
                try {
                    closeable.close();
                } catch (Exception ignored) {
                    // Closing one resource must not skip the remaining resources.
                }
            }
        }
    }
}
