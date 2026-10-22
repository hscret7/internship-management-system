package com.internship.dao;

import com.internship.model.WorkSchedule;
import java.sql.*;
import java.time.*;
import java.util.*;

public class WorkScheduleDAO {
    private static final String SELECT = """
            SELECT s.*, ip.full_name AS intern_name, p.program_name,
                   EXISTS(SELECT 1 FROM leave_requests l WHERE l.enrollment_id=s.enrollment_id
                       AND l.status='APPROVED' AND s.work_date BETWEEN l.from_date AND l.to_date) AS approved_leave
            FROM work_schedules s
            JOIN program_enrollments e ON e.enrollment_id=s.enrollment_id
            JOIN intern_profiles ip ON ip.profile_id=e.intern_profile_id
            JOIN internship_programs p ON p.program_id=e.program_id
            """;

    public WorkSchedule find(int id, Connection c) throws SQLException {
        try (PreparedStatement s = c.prepareStatement(SELECT + " WHERE s.schedule_id=?")) {
            s.setInt(1, id);
            try (ResultSet r = s.executeQuery()) { return r.next() ? map(r) : null; }
        }
    }

    public List<WorkSchedule> list(Long owner, Integer enrollment, Integer program,
                                   LocalDate from, LocalDate to, Connection c) throws SQLException {
        String sql = SELECT + " WHERE s.work_date BETWEEN ? AND ?"
                + (owner == null ? "" : " AND ip.user_id=?")
                + (enrollment == null ? "" : " AND e.enrollment_id=?")
                + (program == null ? "" : " AND e.program_id=?")
                + " ORDER BY s.work_date,s.start_time,s.schedule_id";
        try (PreparedStatement s = c.prepareStatement(sql)) {
            int i = 1;
            s.setDate(i++, java.sql.Date.valueOf(from)); s.setDate(i++, java.sql.Date.valueOf(to));
            if (owner != null) s.setLong(i++, owner);
            if (enrollment != null) s.setInt(i++, enrollment);
            if (program != null) s.setInt(i, program);
            try (ResultSet r = s.executeQuery()) {
                List<WorkSchedule> values = new ArrayList<>();
                while (r.next()) values.add(map(r));
                return List.copyOf(values);
            }
        }
    }

    public boolean overlaps(int enrollment, LocalDate date, LocalTime start, LocalTime end,
                            int exceptId, Connection c) throws SQLException {
        try (PreparedStatement s = c.prepareStatement("""
                SELECT 1 FROM work_schedules WHERE enrollment_id=? AND work_date=?
                AND status='ACTIVE' AND start_time<? AND end_time>? AND schedule_id<>? LIMIT 1
                """)) {
            s.setInt(1, enrollment); s.setDate(2, java.sql.Date.valueOf(date));
            s.setTime(3, Time.valueOf(end)); s.setTime(4, Time.valueOf(start)); s.setInt(5, exceptId);
            try (ResultSet r = s.executeQuery()) { return r.next(); }
        }
    }

    public int create(int enrollment, LocalDate date, LocalTime start, LocalTime end,
                      String note, long actor, Connection c) throws SQLException {
        try (PreparedStatement s = c.prepareStatement("""
                INSERT INTO work_schedules(enrollment_id,work_date,start_time,end_time,note,created_by,updated_by)
                VALUES(?,?,?,?,?,?,?)
                """, Statement.RETURN_GENERATED_KEYS)) {
            s.setInt(1, enrollment); s.setDate(2, java.sql.Date.valueOf(date));
            s.setTime(3, Time.valueOf(start)); s.setTime(4, Time.valueOf(end)); s.setString(5, note);
            s.setLong(6, actor); s.setLong(7, actor);
            if (s.executeUpdate() != 1) throw new SQLException("Schedule insert failed");
            try (ResultSet r = s.getGeneratedKeys()) {
                if (r.next()) return r.getInt(1);
                throw new SQLException("Missing schedule ID");
            }
        }
    }

    public boolean update(int id, LocalDate date, LocalTime start, LocalTime end,
                          String note, long actor, Connection c) throws SQLException {
        try (PreparedStatement s = c.prepareStatement("""
                UPDATE work_schedules SET work_date=?,start_time=?,end_time=?,note=?,updated_by=?
                WHERE schedule_id=? AND status='ACTIVE'
                """)) {
            s.setDate(1, java.sql.Date.valueOf(date)); s.setTime(2, Time.valueOf(start));
            s.setTime(3, Time.valueOf(end)); s.setString(4, note); s.setLong(5, actor); s.setInt(6, id);
            return s.executeUpdate() == 1;
        }
    }

    public boolean cancel(int id, long actor, Connection c) throws SQLException {
        try (PreparedStatement s = c.prepareStatement(
                "UPDATE work_schedules SET status='CANCELLED',updated_by=? WHERE schedule_id=? AND status='ACTIVE'")) {
            s.setLong(1, actor); s.setInt(2, id); return s.executeUpdate() == 1;
        }
    }

    private WorkSchedule map(ResultSet r) throws SQLException {
        return new WorkSchedule(r.getInt("schedule_id"), r.getInt("enrollment_id"),
                r.getDate("work_date").toLocalDate(), r.getTime("start_time").toLocalTime(),
                r.getTime("end_time").toLocalTime(), r.getString("note"), r.getString("status"),
                r.getString("intern_name"), r.getString("program_name"), r.getBoolean("approved_leave"));
    }
}
