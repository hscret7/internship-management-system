package com.internship.dao;

import com.internship.model.LeaveRequest;
import java.sql.*;
import java.time.*;
import java.util.*;

public class LeaveRequestDAO {
    private static final String SELECT = """
            SELECT l.*, UNIX_TIMESTAMP(l.reviewed_at) AS reviewed_at_epoch,
                   UNIX_TIMESTAMP(l.created_at) AS created_at_epoch, ip.full_name AS intern_name, p.program_name
            FROM leave_requests l
            JOIN program_enrollments e ON e.enrollment_id=l.enrollment_id
            JOIN intern_profiles ip ON ip.profile_id=e.intern_profile_id
            JOIN internship_programs p ON p.program_id=e.program_id
            """;

    public LeaveRequest find(int id, Connection c) throws SQLException {
        try (PreparedStatement s = c.prepareStatement(SELECT + " WHERE l.leave_id=?")) {
            s.setInt(1, id);
            try (ResultSet r = s.executeQuery()) { return r.next() ? map(r) : null; }
        }
    }

    public List<LeaveRequest> list(Long owner, Integer enrollment, Integer program, String status,
                                   LocalDate from, LocalDate to, Connection c) throws SQLException {
        String sql = SELECT + " WHERE l.from_date<=? AND l.to_date>=?"
                + (owner == null ? "" : " AND ip.user_id=?")
                + (enrollment == null ? "" : " AND e.enrollment_id=?")
                + (program == null ? "" : " AND e.program_id=?")
                + (status == null ? "" : " AND l.status=?")
                + " ORDER BY l.created_at DESC,l.leave_id DESC";
        try (PreparedStatement s = c.prepareStatement(sql)) {
            int i = 1;
            s.setDate(i++, java.sql.Date.valueOf(to)); s.setDate(i++, java.sql.Date.valueOf(from));
            if (owner != null) s.setLong(i++, owner);
            if (enrollment != null) s.setInt(i++, enrollment);
            if (program != null) s.setInt(i++, program);
            if (status != null) s.setString(i, status);
            try (ResultSet r = s.executeQuery()) {
                List<LeaveRequest> values = new ArrayList<>();
                while (r.next()) values.add(map(r));
                return List.copyOf(values);
            }
        }
    }

    public boolean overlaps(int enrollment, LocalDate from, LocalDate to, int exceptId,
                            Connection c) throws SQLException {
        try (PreparedStatement s = c.prepareStatement("""
                SELECT 1 FROM leave_requests WHERE enrollment_id=? AND status IN ('PENDING','APPROVED')
                AND from_date<=? AND to_date>=? AND leave_id<>? LIMIT 1
                """)) {
            s.setInt(1, enrollment); s.setDate(2, java.sql.Date.valueOf(to));
            s.setDate(3, java.sql.Date.valueOf(from)); s.setInt(4, exceptId);
            try (ResultSet r = s.executeQuery()) { return r.next(); }
        }
    }

    public int create(int enrollment, LocalDate from, LocalDate to, String reason,
                      Connection c) throws SQLException {
        try (PreparedStatement s = c.prepareStatement(
                "INSERT INTO leave_requests(enrollment_id,from_date,to_date,reason) VALUES(?,?,?,?)",
                Statement.RETURN_GENERATED_KEYS)) {
            s.setInt(1, enrollment); s.setDate(2, java.sql.Date.valueOf(from));
            s.setDate(3, java.sql.Date.valueOf(to)); s.setString(4, reason);
            if (s.executeUpdate() != 1) throw new SQLException("Leave insert failed");
            try (ResultSet r = s.getGeneratedKeys()) {
                if (r.next()) return r.getInt(1);
                throw new SQLException("Missing leave ID");
            }
        }
    }

    public boolean decide(int id, boolean approve, long reviewer, String rejectReason,
                          Connection c) throws SQLException {
        try (PreparedStatement s = c.prepareStatement("""
                UPDATE leave_requests SET status=?,reviewed_by=?,reviewed_at=CURRENT_TIMESTAMP,reject_reason=?
                WHERE leave_id=? AND status='PENDING'
                """)) {
            s.setString(1, approve ? "APPROVED" : "REJECTED"); s.setLong(2, reviewer);
            s.setString(3, approve ? null : rejectReason); s.setInt(4, id);
            return s.executeUpdate() == 1;
        }
    }

    private LeaveRequest map(ResultSet r) throws SQLException {
        long reviewer = r.getLong("reviewed_by");
        Long reviewerId = r.wasNull() ? null : reviewer;
        Instant reviewed = com.internship.util.SqlTime.instant(r, "reviewed_at"), created = com.internship.util.SqlTime.instant(r, "created_at");
        return new LeaveRequest(r.getInt("leave_id"), r.getInt("enrollment_id"),
                r.getDate("from_date").toLocalDate(), r.getDate("to_date").toLocalDate(),
                r.getString("reason"), r.getString("status"), reviewerId,
                reviewed, r.getString("reject_reason"),
                created, r.getString("intern_name"), r.getString("program_name"));
    }
}
