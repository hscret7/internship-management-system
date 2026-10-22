package com.internship.dao;

import com.internship.model.EnrollmentSummary;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/** Lists only enrollments linked to the authenticated mentor/intern user ID. */
public class Be4EnrollmentDAO {
    public List<EnrollmentSummary> findForMentor(Connection connection, long mentorUserId) throws SQLException {
        String sql = """
                SELECT pe.enrollment_id, ip.full_name AS intern_name,
                       p.program_name, pe.status, p.start_date, p.end_date
                FROM mentor_assignments ma
                JOIN mentors m ON m.mentor_id = ma.mentor_id
                JOIN program_enrollments pe ON pe.enrollment_id = ma.enrollment_id
                JOIN internship_programs p ON p.program_id = pe.program_id
                JOIN intern_profiles ip ON ip.profile_id = pe.intern_profile_id
                WHERE m.user_id = ? AND (ma.ended_at IS NULL OR (pe.status IN ('COMPLETED','CANCELLED')
                  AND ma.assignment_id = (SELECT MAX(last_ma.assignment_id) FROM mentor_assignments last_ma WHERE last_ma.enrollment_id=pe.enrollment_id)))
                ORDER BY pe.enrolled_at DESC, pe.enrollment_id DESC
                """;
        return list(connection, sql, mentorUserId);
    }

    public List<EnrollmentSummary> findForIntern(Connection connection, long internUserId) throws SQLException {
        String sql = """
                SELECT pe.enrollment_id, ip.full_name AS intern_name,
                       p.program_name, pe.status, p.start_date, p.end_date
                FROM program_enrollments pe
                JOIN internship_programs p ON p.program_id = pe.program_id
                JOIN intern_profiles ip ON ip.profile_id = pe.intern_profile_id
                WHERE ip.user_id = ?
                ORDER BY pe.enrolled_at DESC, pe.enrollment_id DESC
                """;
        return list(connection, sql, internUserId);
    }

    private List<EnrollmentSummary> list(Connection connection, String sql, long userId) throws SQLException {
        List<EnrollmentSummary> result = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, userId);
            try (ResultSet rows = statement.executeQuery()) {
                while (rows.next()) {
                    result.add(new EnrollmentSummary(
                            rows.getInt("enrollment_id"), rows.getString("intern_name"),
                            rows.getString("program_name"), rows.getString("status"),
                            rows.getDate("start_date").toLocalDate(),
                            rows.getDate("end_date").toLocalDate()));
                }
            }
        }
        return result;
    }
}
