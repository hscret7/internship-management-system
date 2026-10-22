package com.internship.dao;

import java.sql.*;
import java.util.*;

/** All mutations use the caller's transaction, with program -> enrollment lock order. */
public class ProgramLifecycleDAO {
    public record Enrollment(int id, int programId, String status) { }
    public String lockProgram(Connection c, int id) throws SQLException {
        try (var s = c.prepareStatement("SELECT status FROM internship_programs WHERE program_id=? FOR UPDATE")) {
            s.setInt(1, id);
            try (var r = s.executeQuery()) { return r.next() ? r.getString(1) : null; }
        }
    }
    public Enrollment enrollment(Connection c, int id, boolean lock) throws SQLException {
        try (var s = c.prepareStatement("SELECT enrollment_id,program_id,status FROM program_enrollments WHERE enrollment_id=?" + (lock ? " FOR UPDATE" : ""))) {
            s.setInt(1, id);
            try (var r = s.executeQuery()) { return r.next() ? new Enrollment(r.getInt(1), r.getInt(2), r.getString(3)) : null; }
        }
    }
    public List<Enrollment> lockActiveEnrollments(Connection c, int programId) throws SQLException {
        try (var s = c.prepareStatement("SELECT enrollment_id,program_id,status FROM program_enrollments WHERE program_id=? AND status='ACTIVE' ORDER BY enrollment_id FOR UPDATE")) {
            s.setInt(1, programId);
            try (var r = s.executeQuery()) {
                List<Enrollment> rows = new ArrayList<>();
                while (r.next()) rows.add(new Enrollment(r.getInt(1), r.getInt(2), r.getString(3)));
                return rows;
            }
        }
    }
    public void closeEnrollment(Connection c, Enrollment e, String target, long actor, String reason) throws SQLException {
        try (var s = c.prepareStatement("UPDATE program_enrollments SET status=?,completed_at=CASE WHEN ?='COMPLETED' THEN CURRENT_TIMESTAMP ELSE completed_at END WHERE enrollment_id=? AND status='ACTIVE'")) {
            s.setString(1, target); s.setString(2, target); s.setInt(3, e.id());
            if (s.executeUpdate() != 1) throw new IllegalStateException("Ghi danh đã được kết thúc bởi thao tác khác.");
        }
        try (var s = c.prepareStatement("UPDATE mentor_assignments SET ended_at=CURRENT_TIMESTAMP,end_reason=? WHERE enrollment_id=? AND ended_at IS NULL")) {
            s.setString(1, "Enrollment " + target); s.setInt(2, e.id()); s.executeUpdate();
        }
        event(c, "enrollment_lifecycle_events", "enrollment_id", e.id(), e.status(), target, actor, reason);
    }
    public void closeProgram(Connection c, int id, String from, String target, long actor, String reason) throws SQLException {
        try (var s = c.prepareStatement("UPDATE internship_programs SET status=? WHERE program_id=? AND status=?")) {
            s.setString(1, target); s.setInt(2, id); s.setString(3, from);
            if (s.executeUpdate() != 1) throw new IllegalStateException("Chương trình đã đổi trạng thái.");
        }
        event(c, "program_lifecycle_events", "program_id", id, from, target, actor, reason);
    }
    private void event(Connection c, String table, String key, int id, String from, String to, long actor, String reason) throws SQLException {
        // Table/key are private compile-time constants, never client input.
        try (var s = c.prepareStatement("INSERT INTO " + table + "(" + key + ",from_status,to_status,actor_user_id,reason) VALUES(?,?,?,?,?)")) {
            s.setInt(1, id); s.setString(2, from); s.setString(3, to); s.setLong(4, actor); s.setString(5, reason); s.executeUpdate();
        }
    }
}
