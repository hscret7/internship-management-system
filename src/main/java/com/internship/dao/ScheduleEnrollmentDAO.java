package com.internship.dao;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/** Parent enrollment locks serialize schedule/leave mutations for the same intern. */
public class ScheduleEnrollmentDAO {
    public record Context(int enrollmentId, long userId, String enrollmentStatus,
                          String programStatus, LocalDate startDate, LocalDate endDate) { }

    /** All matching enrollments are returned; callers must make an explicit choice. */
    public record Choice(int enrollmentId, int programId, String programName,
                         String enrollmentStatus, String programStatus, String internName, String email) {
        public int getEnrollmentId() { return enrollmentId; }
        public String getProgramName() { return programName; }
        public String getEnrollmentStatus() { return enrollmentStatus; }
    }

    public List<Choice> choices(Long owner, String email, Connection connection) throws SQLException {
        if (owner == null && email == null) throw new IllegalArgumentException("Lookup requires owner or email");
        String sql = """
                SELECT e.enrollment_id, e.program_id, p.program_name,
                       e.status AS enrollment_status, p.status AS program_status,
                       ip.full_name, COALESCE(u.email, ip.email) AS email
                FROM program_enrollments e
                JOIN intern_profiles ip ON ip.profile_id=e.intern_profile_id
                JOIN internship_programs p ON p.program_id=e.program_id
                LEFT JOIN users u ON u.user_id=ip.user_id
                WHERE 1=1
                """ + (owner == null ? "" : " AND ip.user_id=?")
                + (email == null ? "" : " AND (LOWER(TRIM(u.email))=LOWER(?) OR LOWER(TRIM(ip.email))=LOWER(?))")
                + " ORDER BY p.program_name, e.enrollment_id";
        try (PreparedStatement s = connection.prepareStatement(sql)) {
            int i = 1;
            if (owner != null) s.setLong(i++, owner);
            if (email != null) { s.setString(i++, email); s.setString(i, email); }
            try (ResultSet r = s.executeQuery()) {
                List<Choice> result = new ArrayList<>();
                while (r.next()) result.add(new Choice(r.getInt("enrollment_id"), r.getInt("program_id"),
                        r.getString("program_name"), r.getString("enrollment_status"), r.getString("program_status"),
                        r.getString("full_name"), r.getString("email")));
                return List.copyOf(result);
            }
        }
    }

    public Context find(int id, boolean lock, Connection connection) throws SQLException {
        if (lock) {
            Integer program = null;
            try (PreparedStatement s = connection.prepareStatement("SELECT program_id FROM program_enrollments WHERE enrollment_id=?")) {
                s.setInt(1, id);
                try (ResultSet r = s.executeQuery()) { if (r.next()) program = r.getInt(1); }
            }
            if (program == null) return null;
            new InternshipProgramDAO().lockProgram(program, connection);
        }
        String sql = """
                SELECT e.enrollment_id, ip.user_id, e.status AS enrollment_status,
                       p.status AS program_status, p.start_date, p.end_date
                FROM program_enrollments e
                JOIN intern_profiles ip ON ip.profile_id=e.intern_profile_id
                JOIN internship_programs p ON p.program_id=e.program_id
                WHERE e.enrollment_id=?
                """ + (lock ? " FOR UPDATE" : "");
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, id);
            try (ResultSet result = statement.executeQuery()) {
                return result.next() ? new Context(result.getInt("enrollment_id"), result.getLong("user_id"),
                        result.getString("enrollment_status"), result.getString("program_status"),
                        result.getDate("start_date").toLocalDate(), result.getDate("end_date").toLocalDate()) : null;
            }
        }
    }
}
