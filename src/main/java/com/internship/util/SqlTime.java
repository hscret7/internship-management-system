package com.internship.util;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.*;
import java.time.*;

/** TIMESTAMP epoch projections preserve instants even when JDBC and DB session zones differ. */
public final class SqlTime {
    private SqlTime() { }
    public static Instant instant(ResultSet rows, String column) throws SQLException {
        BigDecimal epoch = rows.getBigDecimal(column + "_epoch");
        if (epoch == null) return null;
        long seconds = epoch.setScale(0, RoundingMode.FLOOR).longValueExact();
        long nanos = epoch.subtract(BigDecimal.valueOf(seconds)).movePointRight(9).longValueExact();
        return Instant.ofEpochSecond(seconds, nanos);
    }
    /** Compatibility models store UTC components; never JVM-local components. */
    public static LocalDateTime utcLocal(ResultSet rows, String column) throws SQLException {
        Instant value = instant(rows, column);
        return value == null ? null : LocalDateTime.ofInstant(value, ZoneOffset.UTC);
    }
}
