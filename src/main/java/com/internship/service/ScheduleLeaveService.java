package com.internship.service;

import com.internship.dao.*;
import com.internship.dao.ScheduleEnrollmentDAO.Context;
import com.internship.model.*;
import com.internship.util.ConnectionProvider;
import com.internship.util.DBContext;
import com.internship.util.UserRoleChecks;
import java.sql.*;
import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.*;

/** US23/US24. Writes lock the enrollment first; no attendance history is overwritten. */
public class ScheduleLeaveService {
    public static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private final ScheduleEnrollmentDAO enrollments;
    private final WorkScheduleDAO schedules;
    private final LeaveRequestDAO leaves;
    private final ConnectionProvider connections;
    private final Clock clock;

    public ScheduleLeaveService() {
        this(new ScheduleEnrollmentDAO(), new WorkScheduleDAO(), new LeaveRequestDAO(),
                DBContext::getConnection, Clock.system(BUSINESS_ZONE));
    }

    public ScheduleLeaveService(ScheduleEnrollmentDAO enrollments, WorkScheduleDAO schedules,
                                 LeaveRequestDAO leaves, ConnectionProvider connections, Clock clock) {
        this.enrollments = Objects.requireNonNull(enrollments);
        this.schedules = Objects.requireNonNull(schedules);
        this.leaves = Objects.requireNonNull(leaves);
        this.connections = Objects.requireNonNull(connections);
        this.clock = Objects.requireNonNull(clock).withZone(BUSINESS_ZONE);
    }

    public List<WorkSchedule> schedules(User actor, Integer enrollment, Integer program,
                                        LocalDate from, LocalDate to) throws SQLException {
        reader(actor); filters(enrollment, program, from, to);
        try (Connection c = connections.getConnection()) {
            if (enrollment != null) readable(actor, context(enrollment, false, c));
            return schedules.list(owner(actor), enrollment, program, from, to, c);
        }
    }

    public List<LeaveRequest> leaves(User actor, Integer enrollment, Integer program, String status,
                                     LocalDate from, LocalDate to) throws SQLException {
        reader(actor); filters(enrollment, program, from, to);
        if (status != null && !Set.of("PENDING", "APPROVED", "REJECTED").contains(status))
            throw invalid("Trạng thái nghỉ phép không hợp lệ.");
        try (Connection c = connections.getConnection()) {
            if (enrollment != null) readable(actor, context(enrollment, false, c));
            return leaves.list(owner(actor), enrollment, program, status, from, to, c);
        }
    }

    public int createSchedule(User actor, int enrollment, LocalDate date, LocalTime start,
                               LocalTime end, String note) throws SQLException {
        hr(actor); positive(enrollment); times(start, end);
        String cleaned = text(note, 500, false);
        return write(c -> {
            Context ctx = context(enrollment, true, c); active(ctx); dateRange(ctx, date, date);
            futureShift(date, start);
            noScheduleOverlap(enrollment, date, start, end, 0, c);
            return schedules.create(enrollment, date, start, end, cleaned, actor.getUserId(), c);
        });
    }

    public void updateSchedule(User actor, int scheduleId, LocalDate date, LocalTime start,
                                LocalTime end, String note) throws SQLException {
        hr(actor); positive(scheduleId); times(start, end);
        String cleaned = text(note, 500, false);
        write(c -> {
            WorkSchedule before = schedule(scheduleId, c);
            Context ctx = context(before.enrollmentId(), true, c); active(ctx);
            // Re-read after obtaining the parent lock: another transaction may have edited it.
            before = schedule(scheduleId, c); editable(before);
            dateRange(ctx, date, date); futureShift(date, start);
            noScheduleOverlap(ctx.enrollmentId(), date, start, end, scheduleId, c);
            if (!schedules.update(scheduleId, date, start, end, cleaned, actor.getUserId(), c))
                throw conflict("SCHEDULE_CONFLICT", "Ca đã bị hủy hoặc thay đổi.");
            return null;
        });
    }

    public void cancelSchedule(User actor, int scheduleId) throws SQLException {
        hr(actor); positive(scheduleId);
        write(c -> {
            WorkSchedule before = schedule(scheduleId, c);
            active(context(before.enrollmentId(), true, c));
            editable(schedule(scheduleId, c));
            if (!schedules.cancel(scheduleId, actor.getUserId(), c))
                throw conflict("SCHEDULE_CONFLICT", "Ca đã bị hủy hoặc thay đổi.");
            return null;
        });
    }

    /** Each enrollment gets its own committed transaction and an explicit success/failure result. */
    public record BulkResult(int enrollmentId, Integer scheduleId, boolean success, String errorCode) { }
    public List<BulkResult> createSchedules(User actor, List<Integer> ids, LocalDate date,
                                             LocalTime start, LocalTime end, String note) throws SQLException {
        hr(actor); times(start, end); text(note, 500, false);
        if (ids == null || ids.isEmpty() || ids.size() > 100 || new HashSet<>(ids).size() != ids.size())
            throw invalid("Chọn 1–100 ghi danh khác nhau.");
        for (Integer id : ids) { if (id == null) throw invalid("Mã ghi danh không hợp lệ."); positive(id); }
        List<BulkResult> results = new ArrayList<>();
        for (int id : ids) {
            try { results.add(new BulkResult(id, createSchedule(actor, id, date, start, end, note), true, null)); }
            catch (ScheduleLeaveException e) { results.add(new BulkResult(id, null, false, e.code())); }
            catch (SQLException e) { results.add(new BulkResult(id, null, false, "DATABASE_ERROR")); }
        }
        return List.copyOf(results);
    }

    public int requestLeave(User actor, int enrollment, LocalDate from, LocalDate to,
                             String reason) throws SQLException {
        intern(actor); positive(enrollment);
        String cleaned = text(reason, 1000, true);
        return write(c -> {
            Context ctx = context(enrollment, true, c); readable(actor, ctx); active(ctx);
            dateRange(ctx, from, to);
            if (from.isBefore(LocalDate.now(clock))) throw invalid("Không đăng ký nghỉ cho ngày đã qua.");
            if (leaves.overlaps(enrollment, from, to, 0, c))
                throw conflict("LEAVE_OVERLAP", "Khoảng nghỉ trùng đơn đang chờ hoặc đã duyệt.");
            return leaves.create(enrollment, from, to, cleaned, c);
        });
    }

    public void decideLeave(User actor, int id, boolean approve, String rejectReason) throws SQLException {
        hr(actor); positive(id);
        String cleaned = approve ? null : text(rejectReason, 1000, true);
        write(c -> {
            LeaveRequest before = leave(id, c);
            Context ctx = context(before.enrollmentId(), true, c); active(ctx);
            before = leave(id, c);
            if (!"PENDING".equals(before.status()))
                throw conflict("LEAVE_ALREADY_REVIEWED", "Đơn nghỉ đã được xử lý.");
            if (approve) {
                dateRange(ctx, before.fromDate(), before.toDate());
                if (leaves.overlaps(ctx.enrollmentId(), before.fromDate(), before.toDate(), id, c))
                    throw conflict("LEAVE_OVERLAP", "Khoảng nghỉ trùng đơn khác.");
            }
            if (!leaves.decide(id, approve, actor.getUserId(), cleaned, c))
                throw conflict("LEAVE_ALREADY_REVIEWED", "Đơn nghỉ đã được xử lý.");
            return null;
        });
    }

    private Context context(int id, boolean lock, Connection c) throws SQLException {
        Context ctx = enrollments.find(id, lock, c);
        if (ctx == null) throw missing("Ghi danh không tồn tại.");
        return ctx;
    }
    private WorkSchedule schedule(int id, Connection c) throws SQLException {
        WorkSchedule value = schedules.find(id, c);
        if (value == null) throw missing("Ca làm việc không tồn tại.");
        return value;
    }
    private LeaveRequest leave(int id, Connection c) throws SQLException {
        LeaveRequest value = leaves.find(id, c);
        if (value == null) throw missing("Đơn nghỉ không tồn tại.");
        return value;
    }
    private void editable(WorkSchedule value) {
        if (!"ACTIVE".equals(value.status())) throw conflict("SCHEDULE_CONFLICT", "Ca đã bị hủy.");
        futureShift(value.workDate(), value.startTime());
    }
    private void futureShift(LocalDate date, LocalTime start) {
        if (date == null || !LocalDateTime.of(date, start).isAfter(LocalDateTime.now(clock)))
            throw conflict("SHIFT_ALREADY_STARTED", "Không tạo/sửa/hủy ca đã bắt đầu; lịch sử được giữ nguyên.");
    }
    private void noScheduleOverlap(int enrollment, LocalDate date, LocalTime start, LocalTime end,
                                    int except, Connection c) throws SQLException {
        if (schedules.overlaps(enrollment, date, start, end, except, c))
            throw conflict("SCHEDULE_OVERLAP", "Ca làm việc bị chồng giờ.");
    }
    private static void active(Context ctx) {
        if (!"ACTIVE".equals(ctx.enrollmentStatus()) || !"ACTIVE".equals(ctx.programStatus()))
            throw conflict("ENROLLMENT_INACTIVE", "Chương trình/ghi danh không còn hoạt động.");
    }
    private static void dateRange(Context ctx, LocalDate from, LocalDate to) {
        if (from == null || to == null || from.isAfter(to)) throw invalid("Khoảng ngày không hợp lệ.");
        if (from.isBefore(ctx.startDate()) || to.isAfter(ctx.endDate()))
            throw invalid("Ngày phải nằm trong thời gian chương trình.");
    }
    private static void times(LocalTime start, LocalTime end) {
        if (start == null || end == null || !start.isBefore(end) || start.getNano() != 0 || end.getNano() != 0)
            throw invalid("Giờ bắt đầu phải trước giờ kết thúc; không hỗ trợ ca qua đêm.");
    }
    private static void filters(Integer enrollment, Integer program, LocalDate from, LocalDate to) {
        if (enrollment != null) positive(enrollment);
        if (program != null) positive(program);
        if (from == null || to == null || from.isAfter(to) || ChronoUnit.DAYS.between(from, to) > 366)
            throw invalid("Chọn khoảng ngày hợp lệ, tối đa 367 ngày.");
    }
    private static String text(String value, int max, boolean required) {
        String cleaned = value == null ? "" : value.trim();
        if (required && cleaned.isEmpty() || cleaned.codePointCount(0, cleaned.length()) > max)
            throw invalid("Nội dung bắt buộc hoặc vượt giới hạn " + max + " ký tự.");
        return cleaned;
    }
    private static void positive(int id) { if (id <= 0) throw invalid("Mã dữ liệu phải là số dương."); }
    private static Long owner(User actor) { return UserRoleChecks.isIntern(actor) ? actor.getUserId() : null; }
    private static void reader(User actor) {
        authenticated(actor);
        if (!UserRoleChecks.isIntern(actor) && !UserRoleChecks.canManageHr(actor)) throw forbidden();
    }
    private static void hr(User actor) { authenticated(actor); if (!UserRoleChecks.canManageHr(actor)) throw forbidden(); }
    private static void intern(User actor) { authenticated(actor); if (!UserRoleChecks.isIntern(actor)) throw forbidden(); }
    private static void authenticated(User actor) {
        if (actor == null || actor.getUserId() <= 0 || actor.getStatus() != UserStatus.ACTIVE) throw forbidden();
    }
    private static void readable(User actor, Context ctx) {
        if (UserRoleChecks.isIntern(actor) && ctx.userId() != actor.getUserId()) throw forbidden();
    }
    private static ScheduleLeaveException invalid(String message) { return new ScheduleLeaveException(400, "VALIDATION_ERROR", message); }
    private static ScheduleLeaveException missing(String message) { return new ScheduleLeaveException(404, "NOT_FOUND", message); }
    private static ScheduleLeaveException forbidden() { return new ScheduleLeaveException(403, "FORBIDDEN", "Không có quyền truy cập."); }
    private static ScheduleLeaveException conflict(String code, String message) { return new ScheduleLeaveException(409, code, message); }

    private interface Transaction<T> { T run(Connection c) throws SQLException; }
    private <T> T write(Transaction<T> action) throws SQLException {
        try (Connection c = connections.getConnection()) {
            c.setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);
            c.setAutoCommit(false);
            try { T result = action.run(c); c.commit(); return result; }
            catch (SQLException | RuntimeException e) {
                try { c.rollback(); } catch (SQLException rollback) { e.addSuppressed(rollback); }
                throw e;
            }
        }
    }
}
