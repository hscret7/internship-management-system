package com.internship.service;

import com.internship.dao.AttendanceDAO;
import com.internship.dao.TaskAccessDAO;
import com.internship.dao.WorkScheduleDAO;
import com.internship.model.Role;
import com.internship.model.User;
import com.internship.util.DBConnection;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Objects;

/** US34 export adapter. BE2 remains authoritative for attendance classification/hours. */
public class AttendanceReportService {
    public static final int MAX_ROWS = 10_000;

    public record Filter(Integer programId, Integer enrollmentId, LocalDate from, LocalDate to) {
        public Filter {
            if (programId != null && programId <= 0 || enrollmentId != null && enrollmentId <= 0)
                throw new IllegalArgumentException("Mã chương trình/ghi danh phải lớn hơn 0.");
            if (from == null || to == null) throw new IllegalArgumentException("Cần cả from và to (YYYY-MM-DD).");
            if (from.isAfter(to)) throw new IllegalArgumentException("Ngày bắt đầu phải trước hoặc bằng ngày kết thúc.");
            if (ChronoUnit.DAYS.between(from, to) >= 366)
                throw new IllegalArgumentException("Chỉ xuất tối đa 366 ngày; hãy thu hẹp khoảng ngày.");
        }
    }

    public record Row(int enrollmentId, String internName, String programName, LocalDate workDate,
                      LocalTime startTime, LocalTime endTime, LocalDateTime checkInTime,
                      LocalDateTime checkOutTime, boolean approvedLeave, String status, double workedHours) {
        static Row copy(AttendanceService.AttendanceReportRow source) {
            return new Row(source.enrollmentId, source.internName, source.programName, source.workDate,
                    source.startTime, source.endTime, source.checkInTime, source.checkOutTime,
                    source.approvedLeave, source.status, source.workedHours);
        }
    }
    public record Snapshot(Filter filter, List<Row> rows, Instant generatedAt) {
        public Snapshot { rows = List.copyOf(rows); }
    }

    private final Be4ConnectionFactory connections;
    private final AttendanceDAO attendance;
    private final WorkScheduleDAO schedules;

    public AttendanceReportService() { this(DBConnection::getConnection, new AttendanceDAO(), new WorkScheduleDAO()); }
    public AttendanceReportService(Be4ConnectionFactory connections, AttendanceDAO attendance, WorkScheduleDAO schedules) {
        this.connections = Objects.requireNonNull(connections);
        this.attendance = Objects.requireNonNull(attendance);
        this.schedules = Objects.requireNonNull(schedules);
    }

    private void authorize(Connection connection, long actor) throws SQLException {
        if (actor <= 0 || !new TaskAccessDAO().isActiveHrOrAdmin(connection, actor))
            throw new SecurityException("Chỉ HR/Admin đang hoạt động được xuất chuyên cần.");
    }

    public Snapshot snapshot(long actor, Filter filter) throws SQLException {
        Objects.requireNonNull(filter);
        try (Connection connection = connections.open()) {
            // Authorize before the data snapshot; never trust a stale session role or supplied actor field.
            authorize(connection, actor);
            connection.setReadOnly(true);
            connection.setTransactionIsolation(Connection.TRANSACTION_REPEATABLE_READ);
            connection.setAutoCommit(false);
            try {
                preflight(connection, filter);
                User authorizedActor = new User();
                authorizedActor.setUserId(actor);
                Role role = new Role();
                role.setRoleName("HR"); // scope already authorized against the database above
                authorizedActor.setRole(role);
                // BE2 closes the connection it borrows. Keep our snapshot open until its owner commits.
                var service = new AttendanceService(attendance, schedules, () -> borrowed(connection));
                List<Row> rows = service.getReport(authorizedActor, filter.enrollmentId(), filter.programId(),
                        filter.from(), filter.to()).stream().map(Row::copy).toList();
                if (rows.size() > MAX_ROWS) throw tooLarge();
                connection.commit();
                return new Snapshot(filter, rows, Instant.now());
            } catch (SQLException | RuntimeException failure) {
                connection.rollback();
                throw failure;
            }
        }
    }

    private void preflight(Connection connection, Filter filter) throws SQLException {
        String sql = "SELECT COUNT(*) FROM work_schedules s JOIN program_enrollments e "
                + "ON e.enrollment_id=s.enrollment_id WHERE s.status='ACTIVE' AND s.work_date BETWEEN ? AND ?"
                + (filter.programId() == null ? "" : " AND e.program_id=?")
                + (filter.enrollmentId() == null ? "" : " AND e.enrollment_id=?");
        try (var statement = connection.prepareStatement(sql)) {
            statement.setObject(1, filter.from());
            statement.setObject(2, filter.to());
            int index = 3;
            if (filter.programId() != null) statement.setInt(index++, filter.programId());
            if (filter.enrollmentId() != null) statement.setInt(index, filter.enrollmentId());
            try (var result = statement.executeQuery()) {
                result.next();
                if (result.getLong(1) > MAX_ROWS) throw tooLarge();
            }
        }
    }

    private static IllegalStateException tooLarge() {
        return new IllegalStateException("Quá 10000 ca làm việc; hãy thu hẹp bộ lọc. Không xuất một phần dữ liệu.");
    }

    static Connection borrowed(Connection owner) {
        return (Connection) Proxy.newProxyInstance(Connection.class.getClassLoader(), new Class<?>[]{Connection.class},
                (proxy, method, args) -> {
                    if (method.getName().equals("close")) return null;
                    try { return method.invoke(owner, args); }
                    catch (InvocationTargetException failure) { throw failure.getCause(); }
                });
    }

    public void recordExport(long actor, Snapshot snapshot, String format) throws SQLException {
        if (!"xlsx".equals(format) && !"pdf".equals(format)) throw new IllegalArgumentException("Định dạng không hợp lệ.");
        try (Connection connection = connections.open()) {
            authorize(connection, actor);
            try (var statement = connection.prepareStatement("INSERT INTO report_export_events"
                    + "(actor_user_id,program_id,enrollment_id,format,report_kind,report_from,report_to,row_count) "
                    + "VALUES(?,?,?,?,'ATTENDANCE',?,?,?)")) {
                statement.setLong(1, actor);
                statement.setObject(2, snapshot.filter().programId());
                statement.setObject(3, snapshot.filter().enrollmentId());
                statement.setString(4, format);
                statement.setObject(5, snapshot.filter().from());
                statement.setObject(6, snapshot.filter().to());
                statement.setInt(7, snapshot.rows().size());
                statement.executeUpdate();
            }
        }
    }
}
