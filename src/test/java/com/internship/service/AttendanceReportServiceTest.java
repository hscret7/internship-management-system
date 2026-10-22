package com.internship.service;

import com.internship.dao.*;
import com.internship.model.*;
import java.lang.reflect.*;
import java.sql.*;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class AttendanceReportServiceTest {
    final LocalDate day = LocalDate.of(2026, 10, 10);
    AttendanceReportService.Filter filter() { return new AttendanceReportService.Filter(7, 3, day, day); }
    @Test void filterRejectsMissingReversedTooLongAndNonpositiveBounds() {
        assertThrows(IllegalArgumentException.class, () -> new AttendanceReportService.Filter(null, null, day, null));
        assertThrows(IllegalArgumentException.class, () -> new AttendanceReportService.Filter(null, null, day, day.minusDays(1)));
        assertThrows(IllegalArgumentException.class, () -> new AttendanceReportService.Filter(null, null, day, day.plusDays(366)));
        assertThrows(IllegalArgumentException.class, () -> new AttendanceReportService.Filter(0, null, day, day));
        assertThrows(IllegalArgumentException.class, () -> new AttendanceReportService.Filter(null, -1, day, day));
        assertDoesNotThrow(() -> new AttendanceReportService.Filter(null, null, day, day.plusDays(365)));
    }
    @Test void sameConnectionAndFiltersReuseBe2StatusesWithoutRecalculatingThem() throws Exception {
        Fixture db = new Fixture(); var scheduleCalls = new ArrayList<Object>(); var attendanceCalls = new ArrayList<Object>();
        WorkScheduleDAO schedules = new WorkScheduleDAO() {
            @Override public List<WorkSchedule> list(Long owner, Integer enrollment, Integer program, LocalDate from, LocalDate to, Connection c) throws SQLException {
                assertNull(owner); scheduleCalls.addAll(List.of(enrollment, program, from, to)); assertFalse(c.isClosed());
                return List.of(shift(3, false), shift(4, false), shift(5, true), shift(6, false),
                        new WorkSchedule(9, 9, day, LocalTime.NOON, LocalTime.of(17, 0), null, "CANCELLED", "X", "P", false));
            }
        };
        AttendanceDAO attendance = new AttendanceDAO() {
            @Override public List<Attendance> getAttendanceReport(int program, int enrollment, LocalDate from, LocalDate to, Connection c) throws SQLException {
                attendanceCalls.addAll(List.of(program, enrollment, from, to)); assertFalse(c.isClosed());
                return List.of(punch(3, true), punch(4, false));
            }
        };
        var snapshot = new AttendanceReportService(() -> db.connection, attendance, schedules).snapshot(3_000_000_010L, filter());
        assertEquals(List.of(3, 7, day, day), scheduleCalls); assertEquals(List.of(7, 3, day, day), attendanceCalls);
        assertEquals(List.of("PRESENT", "MISSING_CHECKOUT", "LEAVE", "ABSENT"), snapshot.rows().stream().map(AttendanceReportService.Row::status).toList());
        assertEquals(9, snapshot.rows().getFirst().workedHours()); assertEquals(0, snapshot.rows().get(1).workedHours());
        assertTrue(db.committed); assertFalse(db.rolledBack); assertEquals(1, db.closes);
        assertEquals(Connection.TRANSACTION_REPEATABLE_READ, db.isolation); assertTrue(db.readOnly);
        assertThrows(UnsupportedOperationException.class, () -> snapshot.rows().clear());
    }
    @Test void staleOrInactiveActorCannotReadAnyAttendance() {
        Fixture db = new Fixture(); db.authorized = false;
        assertThrows(SecurityException.class, () -> new AttendanceReportService(() -> db.connection, new AttendanceDAO(), new WorkScheduleDAO()).snapshot(10, filter()));
        assertEquals(0, db.countQueries); assertFalse(db.committed); assertEquals(1, db.closes);
    }
    @Test void oversizedExportFailsBeforeBe2LoadAndNeverReturnsTruncatedData() {
        Fixture db = new Fixture(); db.count = AttendanceReportService.MAX_ROWS + 1;
        assertThrows(IllegalStateException.class, () -> new AttendanceReportService(() -> db.connection, new AttendanceDAO(), new WorkScheduleDAO()).snapshot(10, filter()));
        assertTrue(db.rolledBack); assertFalse(db.committed); assertEquals(1, db.closes);
    }
    @Test void be2SqlFailureIsPropagatedWithRollbackNotEmptySuccess() {
        Fixture db = new Fixture();
        WorkScheduleDAO schedules = new WorkScheduleDAO() { @Override public List<WorkSchedule> list(Long owner, Integer enrollment, Integer program, LocalDate from, LocalDate to, Connection c) { return List.of(); } };
        AttendanceDAO dao = new AttendanceDAO() { @Override public List<Attendance> getAttendanceReport(int program, int enrollment, LocalDate from, LocalDate to, Connection c) throws SQLException { throw new SQLException("source schema mismatch", "42S22", 1054); } };
        var failure = assertThrows(SQLException.class, () -> new AttendanceReportService(() -> db.connection, dao, schedules).snapshot(10, filter()));
        assertEquals(1054, failure.getErrorCode()); assertTrue(db.rolledBack); assertFalse(db.committed); assertEquals(1, db.closes);
    }
    @Test void borrowedConnectionSuppressesOnlyCloseAndUnwrapsSqlExceptions() throws Exception {
        Fixture db = new Fixture(); Connection borrowed = AttendanceReportService.borrowed(db.connection);
        borrowed.close(); assertEquals(0, db.closes); assertFalse(borrowed.isClosed()); borrowed.commit(); assertTrue(db.committed);
        Connection broken = proxy(Connection.class, (p, m, a) -> { throw new SQLException("Failure", "08001"); });
        assertEquals("08001", assertThrows(SQLException.class, () -> AttendanceReportService.borrowed(broken).commit()).getSQLState());
    }
    @Test void successfulGenerationEvidenceUsesWorkPeriodNotProgramCohort() throws Exception {
        Fixture db = new Fixture();
        var snapshot = new AttendanceReportService.Snapshot(filter(), List.of(), Instant.now());
        new AttendanceReportService(() -> db.connection, new AttendanceDAO(), new WorkScheduleDAO()).recordExport(3_000_000_010L, snapshot, "xlsx");
        assertTrue(db.eventSql.contains("report_from,report_to")); assertFalse(db.eventSql.contains("cohort_from"));
        assertEquals(3_000_000_010L, db.eventParameters.get(1)); assertEquals(7, db.eventParameters.get(2));
        assertEquals(3, db.eventParameters.get(3)); assertEquals(day, db.eventParameters.get(5)); assertEquals(0, db.eventParameters.get(7));
        db.authorized = false;
        assertThrows(SecurityException.class, () -> new AttendanceReportService(() -> db.connection, new AttendanceDAO(), new WorkScheduleDAO()).recordExport(10, snapshot, "pdf"));
    }
    WorkSchedule shift(int id, boolean leave) { return new WorkSchedule(id, id, day, LocalTime.of(8, 0), LocalTime.of(17, 0), null, "ACTIVE", "Nguyễn Văn An", "Java", leave); }
    Attendance punch(int id, boolean checkout) { var row = new Attendance(); row.setEnrollmentId(id); row.setWorkDate(day); row.setCheckInTime(day.atTime(8, 0)); if (checkout) row.setCheckOutTime(day.atTime(17, 0)); return row; }

    static class Fixture {
        boolean authorized = true, committed, rolledBack, readOnly; int closes, countQueries, isolation; long count;
        String eventSql = ""; Map<Integer, Object> eventParameters = new HashMap<>();
        Connection connection = proxy(Connection.class, (p, m, a) -> switch (m.getName()) {
            case "prepareStatement" -> statement((String)a[0]);
            case "isClosed" -> closes > 0;
            case "setReadOnly" -> { readOnly = (boolean)a[0]; yield null; }
            case "setTransactionIsolation" -> { isolation = (int)a[0]; yield null; }
            case "commit" -> { committed = true; yield null; }
            case "rollback" -> { rolledBack = true; yield null; }
            case "close" -> { closes++; yield null; }
            default -> null;
        });
        PreparedStatement statement(String sql) {
            boolean auth = sql.contains("FROM users"); boolean event = sql.startsWith("INSERT");
            if (!auth && !event) countQueries++; if (event) eventSql = sql;
            return proxy(PreparedStatement.class, (p, m, a) -> {
                if (m.getName().startsWith("set") && event) { eventParameters.put((int)a[0], a[1]); return null; }
                if (m.getName().equals("executeUpdate")) return 1;
                if (m.getName().equals("executeQuery")) {
                    boolean[] visited = {false};
                    return proxy(ResultSet.class, (rp, rm, ra) -> switch(rm.getName()) {
                        case "next" -> { boolean result = !visited[0] && (!auth || authorized); visited[0] = true; yield result; }
                        case "getLong" -> count;
                        default -> null;
                    });
                }
                return null;
            });
        }
    }
    @SuppressWarnings("unchecked") static <T> T proxy(Class<T> type, InvocationHandler handler) { return (T)Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type}, handler); }
}
