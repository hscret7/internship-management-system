package com.internship.service;

import com.internship.dao.AttendanceDAO;
import com.internship.dao.WorkScheduleDAO;
import com.internship.model.Attendance;
import com.internship.model.User;
import com.internship.model.WorkSchedule;
import com.internship.util.ConnectionProvider;
import com.internship.util.DBConnection;
import com.internship.util.UserRoleChecks;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class AttendanceService {

    private final AttendanceDAO attendanceDAO;
    private final WorkScheduleDAO workScheduleDAO;
    private final ConnectionProvider connections;
    private final java.time.Clock clock;
    public static final java.time.ZoneId BUSINESS_ZONE = java.time.ZoneId.of("Asia/Ho_Chi_Minh");

    public AttendanceService() {
        this(new AttendanceDAO(), new WorkScheduleDAO(), DBConnection::getConnection);
    }

    public AttendanceService(AttendanceDAO attendanceDAO, WorkScheduleDAO workScheduleDAO, ConnectionProvider connections) {
        this(attendanceDAO, workScheduleDAO, connections, java.time.Clock.system(BUSINESS_ZONE));
    }

    public AttendanceService(AttendanceDAO attendanceDAO, WorkScheduleDAO workScheduleDAO, ConnectionProvider connections, java.time.Clock clock) {
        this.attendanceDAO = Objects.requireNonNull(attendanceDAO);
        this.workScheduleDAO = Objects.requireNonNull(workScheduleDAO);
        this.connections = Objects.requireNonNull(connections);
        this.clock = Objects.requireNonNull(clock).withZone(BUSINESS_ZONE);
    }

    public void checkIn(User actor, int enrollmentId) throws SQLException {
        if (actor == null || !UserRoleChecks.isIntern(actor)) {
            throw new IllegalArgumentException("Chỉ thực tập sinh mới được chấm công.");
        }
        
        LocalDateTime now = LocalDateTime.now(clock);
        LocalDate today = now.toLocalDate();

        try (Connection c = connections.getConnection()) {
            c.setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);
            c.setAutoCommit(false);
            try {
                attendanceDAO.lockOwnedEnrollment(enrollmentId, actor.getUserId(), c);
                // Validate enrollment owner
                // In a real app we might use enrollmentDAO.findById to check owner.
                // For now, WorkScheduleDAO.list with actor's userId can verify if they have a schedule today.
                List<WorkSchedule> schedules = workScheduleDAO.list(actor.getUserId(), enrollmentId, null, today, today, c);
                if (schedules.isEmpty()) {
                    throw new IllegalArgumentException("Không có ca làm việc nào được lên lịch cho hôm nay.");
                }
                WorkSchedule todaySchedule = null;
                for (WorkSchedule s : schedules) {
                    if ("ACTIVE".equals(s.status())) {
                        todaySchedule = s;
                        break;
                    }
                }
                if (todaySchedule == null) {
                    throw new IllegalArgumentException("Không có ca làm việc ACTIVE nào cho hôm nay.");
                }
                if (todaySchedule.approvedLeave()) {
                    throw new IllegalArgumentException("Bạn đã được duyệt nghỉ phép cho hôm nay, không thể chấm công.");
                }

                Attendance existing = attendanceDAO.findByEnrollmentAndDate(enrollmentId, today, c);
                if (existing != null) {
                    throw new IllegalArgumentException("Bạn đã check-in cho ngày hôm nay rồi.");
                }

                attendanceDAO.checkIn(enrollmentId, today, java.sql.Timestamp.valueOf(now), c);
                c.commit();
            } catch (SQLException | IllegalArgumentException e) {
                c.rollback();
                throw e;
            }
        }
    }

    public void checkOut(User actor, int enrollmentId) throws SQLException {
        if (actor == null || !UserRoleChecks.isIntern(actor)) {
            throw new IllegalArgumentException("Chỉ thực tập sinh mới được chấm công.");
        }
        
        LocalDateTime now = LocalDateTime.now(clock);
        LocalDate today = now.toLocalDate();

        try (Connection c = connections.getConnection()) {
            c.setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);
            c.setAutoCommit(false);
            try {
                attendanceDAO.lockOwnedEnrollment(enrollmentId, actor.getUserId(), c);
                List<WorkSchedule> schedules = workScheduleDAO.list(actor.getUserId(), enrollmentId, null, today, today, c);
                if (schedules.isEmpty()) {
                    throw new IllegalArgumentException("Không có ca làm việc nào cho hôm nay.");
                }

                Attendance existing = attendanceDAO.findByEnrollmentAndDate(enrollmentId, today, c);
                if (existing == null) {
                    throw new IllegalArgumentException("Bạn chưa check-in cho ngày hôm nay.");
                }
                if (existing.getCheckOutTime() != null) {
                    throw new IllegalArgumentException("Bạn đã check-out rồi.");
                }

                attendanceDAO.checkOut(existing.getAttendanceId(), java.sql.Timestamp.valueOf(now), c);
                c.commit();
            } catch (SQLException | IllegalArgumentException e) {
                c.rollback();
                throw e;
            }
        }
    }

    // Report functionality
    public static class AttendanceReportRow {
        public int enrollmentId;
        public String internName;
        public String programName;
        public LocalDate workDate;
        public LocalTime startTime;
        public LocalTime endTime;
        public LocalDateTime checkInTime;
        public LocalDateTime checkOutTime;
        public boolean approvedLeave;
        public String status; // 'PRESENT', 'ABSENT', 'LEAVE', 'MISSING_CHECKOUT'
        public double workedHours;

        public AttendanceReportRow() {
        }

        public AttendanceReportRow(int enrollmentId, String internName, String programName,
                                   LocalDate workDate, LocalTime startTime, LocalTime endTime,
                                   LocalDateTime checkInTime, LocalDateTime checkOutTime,
                                   boolean approvedLeave, String status, double workedHours) {
            this.enrollmentId = enrollmentId;
            this.internName = internName;
            this.programName = programName;
            this.workDate = workDate;
            this.startTime = startTime;
            this.endTime = endTime;
            this.checkInTime = checkInTime;
            this.checkOutTime = checkOutTime;
            this.approvedLeave = approvedLeave;
            this.status = status;
            this.workedHours = workedHours;
        }

        public int getEnrollmentId() { return enrollmentId; }
        public String getInternName() { return internName; }
        public String getProgramName() { return programName; }
        public LocalDate getWorkDate() { return workDate; }
        public LocalTime getStartTime() { return startTime; }
        public LocalTime getEndTime() { return endTime; }
        public LocalDateTime getCheckInTime() { return checkInTime; }
        public LocalDateTime getCheckOutTime() { return checkOutTime; }
        public boolean isApprovedLeave() { return approvedLeave; }
        public String getStatus() { return status; }
        public double getWorkedHours() { return workedHours; }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            AttendanceReportRow that = (AttendanceReportRow) o;
            return enrollmentId == that.enrollmentId &&
                    approvedLeave == that.approvedLeave &&
                    Double.compare(that.workedHours, workedHours) == 0 &&
                    Objects.equals(internName, that.internName) &&
                    Objects.equals(programName, that.programName) &&
                    Objects.equals(workDate, that.workDate) &&
                    Objects.equals(startTime, that.startTime) &&
                    Objects.equals(endTime, that.endTime) &&
                    Objects.equals(checkInTime, that.checkInTime) &&
                    Objects.equals(checkOutTime, that.checkOutTime) &&
                    Objects.equals(status, that.status);
        }

        @Override
        public int hashCode() {
            return Objects.hash(enrollmentId, internName, programName, workDate, startTime, endTime, checkInTime, checkOutTime, approvedLeave, status, workedHours);
        }

        @Override
        public String toString() {
            return "AttendanceReportRow{" +
                    "enrollmentId=" + enrollmentId +
                    ", internName='" + internName + '\'' +
                    ", programName='" + programName + '\'' +
                    ", workDate=" + workDate +
                    ", startTime=" + startTime +
                    ", endTime=" + endTime +
                    ", checkInTime=" + checkInTime +
                    ", checkOutTime=" + checkOutTime +
                    ", approvedLeave=" + approvedLeave +
                    ", status='" + status + '\'' +
                    ", workedHours=" + workedHours +
                    '}';
        }
    }

    public record AttendanceSummary(int totalSchedules, int presentCount, int absentCount, int leaveCount, int missingCheckoutCount, double totalWorkedHours) { }

    public AttendanceSummary getSummary(User actor, Integer enrollmentId, Integer programId, LocalDate fromDate, LocalDate toDate) throws SQLException {
        List<AttendanceReportRow> rows = getReport(actor, enrollmentId, programId, fromDate, toDate);
        int total = rows.size();
        int present = 0, absent = 0, leave = 0, missing = 0;
        double hours = 0.0;
        for (AttendanceReportRow r : rows) {
            if ("PRESENT".equalsIgnoreCase(r.status)) present++;
            else if ("ABSENT".equalsIgnoreCase(r.status)) absent++;
            else if ("LEAVE".equalsIgnoreCase(r.status)) leave++;
            else if ("MISSING_CHECKOUT".equalsIgnoreCase(r.status)) missing++;
            hours += r.workedHours;
        }
        hours = Math.round(hours * 100.0) / 100.0;
        return new AttendanceSummary(total, present, absent, leave, missing, hours);
    }

    public List<AttendanceReportRow> getReport(User actor, Integer enrollmentId, Integer programId, LocalDate fromDate, LocalDate toDate) throws SQLException {
        return getReport(actor, enrollmentId, programId, fromDate, toDate, null);
    }

    public List<AttendanceReportRow> getReport(User actor, Integer enrollmentId, Integer programId, LocalDate fromDate, LocalDate toDate, String statusFilter) throws SQLException {
        if (actor == null) {
            throw new IllegalArgumentException("Người dùng (actor) không được để trống.");
        }
        if (!UserRoleChecks.isIntern(actor) && !UserRoleChecks.canManageHr(actor)) {
            throw new IllegalArgumentException("Không có quyền truy cập dữ liệu chấm công.");
        }

        // Validate and normalize dates
        LocalDate effectiveFrom = fromDate;
        LocalDate effectiveTo = toDate;
        if (effectiveFrom == null && effectiveTo == null) {
            effectiveFrom = LocalDate.now(clock).minusDays(30);
            effectiveTo = LocalDate.now(clock);
        } else if (effectiveFrom == null) {
            effectiveFrom = effectiveTo.minusDays(30);
        } else if (effectiveTo == null) {
            effectiveTo = effectiveFrom.plusDays(30);
        }

        if (effectiveFrom.isAfter(effectiveTo)) {
            throw new IllegalArgumentException("Từ ngày (fromDate) không được sau đến ngày (toDate).");
        }
        if (java.time.temporal.ChronoUnit.DAYS.between(effectiveFrom, effectiveTo) > 367) {
            throw new IllegalArgumentException("Khoảng thời gian tra cứu không được vượt quá 367 ngày.");
        }

        Long ownerId = UserRoleChecks.isIntern(actor) ? actor.getUserId() : null;
        Integer validEnrollmentId = (enrollmentId != null && enrollmentId > 0) ? enrollmentId : null;
        Integer validProgramId = (programId != null && programId > 0) ? programId : null;

        try (Connection c = connections.getConnection()) {
            List<WorkSchedule> schedules = workScheduleDAO.list(ownerId, validEnrollmentId, validProgramId, effectiveFrom, effectiveTo, c);
            List<Attendance> attendances = (ownerId != null)
                    ? attendanceDAO.getAttendanceReport(ownerId, validProgramId != null ? validProgramId : 0, validEnrollmentId != null ? validEnrollmentId : 0, effectiveFrom, effectiveTo, c)
                    : attendanceDAO.getAttendanceReport(validProgramId != null ? validProgramId : 0, validEnrollmentId != null ? validEnrollmentId : 0, effectiveFrom, effectiveTo, c);

            List<AttendanceReportRow> report = new ArrayList<>();
            java.util.Set<String> seen = new java.util.HashSet<>();
            for (Attendance attendance : attendances) {
                if (!seen.add(attendance.getEnrollmentId() + ":" + attendance.getWorkDate()))
                    throw new IllegalArgumentException("Dữ liệu chấm công trùng ngày. Cần HR/QA đối chiếu; không thể tính giờ công đáng tin cậy.");
            }
            for (WorkSchedule s : schedules) {
                if (!"ACTIVE".equals(s.status())) continue;

                AttendanceReportRow row = new AttendanceReportRow();
                row.enrollmentId = s.enrollmentId();
                row.internName = s.internName();
                row.programName = s.programName();
                row.workDate = s.workDate();
                row.startTime = s.startTime();
                row.endTime = s.endTime();
                row.approvedLeave = s.approvedLeave();

                // Find matching attendance
                Attendance a = attendances.stream().filter(att -> att.getEnrollmentId() == s.enrollmentId() && att.getWorkDate().equals(s.workDate())).findFirst().orElse(null);
                if (a != null) {
                    row.checkInTime = a.getCheckInTime();
                    row.checkOutTime = a.getCheckOutTime();
                    if (row.checkOutTime == null) {
                        row.status = "MISSING_CHECKOUT";
                        row.workedHours = 0.0;
                    } else {
                        // One attendance interval per day; count only its intersection with each shift.
                        LocalDateTime start = s.workDate().atTime(s.startTime());
                        LocalDateTime end = s.workDate().atTime(s.endTime());
                        if (row.checkInTime.isAfter(start)) start = row.checkInTime;
                        if (row.checkOutTime.isBefore(end)) end = row.checkOutTime;
                        long minutes = Math.max(0, Duration.between(start, end).toMinutes());
                        row.status = minutes > 0 ? "PRESENT" : (row.approvedLeave ? "LEAVE" : "ABSENT");
                        double hours = Math.max(0.0, minutes / 60.0);
                        row.workedHours = Math.round(hours * 100.0) / 100.0;
                    }
                } else {
                    if (row.approvedLeave) {
                        row.status = "LEAVE";
                    } else {
                        row.status = "ABSENT";
                    }
                    row.workedHours = 0.0;
                }

                if (statusFilter == null || statusFilter.isBlank() || statusFilter.equalsIgnoreCase(row.status)) {
                    report.add(row);
                }
            }
            return report;
        }
    }
}
