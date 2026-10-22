package com.internship.dao;

import com.internship.service.AuditLogService.Query;
import org.junit.jupiter.api.Test;
import java.lang.reflect.Proxy;
import java.sql.*;
import java.time.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class AuditLogDAOTimeZoneTest {
    private static final Instant EXPECTED = Instant.parse("2026-10-07T11:24:53.246524Z");
    @Test void utcTimestampAndFilterRoundTripUnderDifferentJvmZones() throws Exception {
        TimeZone original = TimeZone.getDefault();
        try {
            for (String zone : List.of("UTC", "Asia/Ho_Chi_Minh", "America/New_York")) {
                TimeZone.setDefault(TimeZone.getTimeZone(zone));
                FakeJdbc jdbc = new FakeJdbc();
                var page = new AuditLogDAO(() -> jdbc.connection).search(
                        new Query(null, "LOGIN", null, null, EXPECTED.minusSeconds(1), EXPECTED.plusSeconds(1), 1, 20));
                assertEquals(EXPECTED, page.items().getFirst().occurredAt(), zone);
                assertEquals(List.of(EXPECTED.minusSeconds(1), EXPECTED.plusSeconds(1),
                        EXPECTED.minusSeconds(1), EXPECTED.plusSeconds(1)), jdbc.bounds);
                assertTrue(jdbc.initialized);
                assertTrue(jdbc.closed);
            }
        } finally { TimeZone.setDefault(original); }
    }
    @Test void sessionSetupFailureClosesConnectionAndDoesNotQuery() {
        FakeJdbc jdbc = new FakeJdbc(); jdbc.setupFailure = true;
        assertThrows(SQLException.class, () -> new AuditLogDAO(() -> jdbc.connection).search(
                new Query(null,null,null,null,null,null,1,20)));
        assertTrue(jdbc.closed); assertFalse(jdbc.prepared);
    }
    @Test void insertsAlsoUseDedicatedUtcSession() throws Exception {
        FakeJdbc jdbc = new FakeJdbc();
        new AuditLogDAO(() -> jdbc.connection).insert(5L,"LOGIN","USER",5L,"SUCCESS");
        assertTrue(jdbc.initialized); assertTrue(jdbc.closed);
    }
    @Test void actorEmailFilterIsBoundInCountAndPageWithoutDroppingSystemRows() throws Exception {
        FakeJdbc jdbc = new FakeJdbc();
        String email = "o'neil@example.com";
        new AuditLogDAO(() -> jdbc.connection).search(new Query(null,null,null,null,null,null,1,20,email));
        assertEquals(2, jdbc.sql.size());
        assertTrue(jdbc.sql.stream().allMatch(sql -> sql.contains("LEFT JOIN users") && sql.contains("LOWER(TRIM(u.email)) = LOWER(?)")));
        assertTrue(jdbc.sql.stream().noneMatch(sql -> sql.contains(email)));
        assertEquals(List.of(email,email), jdbc.values);
    }
    private static class FakeJdbc {
        List<String> sql = new ArrayList<>();
        List<Object> values = new ArrayList<>();
        boolean initialized, closed, setupFailure, prepared;
        List<Instant> bounds = new ArrayList<>();
        Connection connection = proxy(Connection.class, (name,args) -> switch(name) {
            case "createStatement" -> proxy(Statement.class, (method,values) -> {
                if(method.equals("execute")) {
                    assertEquals("SET SESSION time_zone = '+00:00'",values[0]);
                    if(setupFailure) throw new SQLException("setup failed");
                    initialized=true; return false;
                }
                return null;
            });
            case "prepareStatement" -> {
                assertTrue(initialized); prepared=true;
                sql.add((String)args[0]);
                boolean count=((String)args[0]).contains("COUNT(*)");
                yield proxy(PreparedStatement.class, (method,values) -> switch(method) {
                    case "setObject" -> {this.values.add(values[1]); yield null;}
                    case "setTimestamp" -> {
                        assertEquals(3,values.length,"Date filters must provide a Calendar");
                        assertEquals("UTC",((Calendar)values[2]).getTimeZone().getID());
                        bounds.add(((Timestamp)values[1]).toInstant()); yield null;
                    }
                    case "executeUpdate" -> 1;
                    case "executeQuery" -> result(count);
                    default -> null;
                });
            }
            case "close" -> {closed=true; yield null;}
            default -> null;
        });
        ResultSet result(boolean count) {
            int[] row={0};
            return proxy(ResultSet.class,(method,args)->switch(method) {
                case "next" -> ++row[0]==1;
                case "getLong" -> count ? 1L : 5L;
                case "wasNull" -> false;
                case "getString" -> args[0].equals("action")?"LOGIN":args[0].equals("object_type")?"USER":"SUCCESS";
                case "getTimestamp" -> {
                    assertEquals(2,args.length,"Timestamp read must provide a Calendar");
                    Calendar calendar=(Calendar)args[1];
                    assertEquals("UTC",calendar.getTimeZone().getID());
                    // Model JDBC's interpretation of MySQL's timezone-less wire value.
                    yield Timestamp.from(LocalDateTime.parse("2026-10-07T11:24:53.246524")
                            .atZone(calendar.getTimeZone().toZoneId()).toInstant());
                }
                default -> null;
            });
        }
    }
    interface Call { Object invoke(String method,Object[] args) throws Throwable; }
    @SuppressWarnings("unchecked") static <T> T proxy(Class<T> type,Call call) {
        return (T)Proxy.newProxyInstance(type.getClassLoader(),new Class<?>[]{type},
                (p,m,a)->call.invoke(m.getName(),a));
    }
}
