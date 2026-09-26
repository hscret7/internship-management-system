package com.internship.dao;

import com.internship.model.Mentor;
import com.internship.util.DBContext;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * MentorDAO: Xử lý CSDL cho bảng mentors (BE3).
 */
public class MentorDAO {

    public void lockMentor(int mentorId, Connection conn) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT mentor_id FROM mentors WHERE mentor_id = ? FOR UPDATE")) {
            ps.setInt(1, mentorId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) throw new IllegalStateException("Mentor không tồn tại.");
            }
        }
    }

    public int createMentor(Mentor mentor) throws SQLException {
        String sql = """
            INSERT INTO mentors (user_id, department_id, specialization, max_mentees, status)
            SELECT u.user_id, ?, ?, ?, ? FROM users u
            JOIN roles r ON r.role_id = u.role_id
            WHERE u.user_id = ? AND u.status = 'ACTIVE' AND r.role_name = 'MENTOR'
            """;
        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            if (mentor.getDepartmentId() != null) {
                ps.setInt(1, mentor.getDepartmentId());
            } else {
                ps.setNull(1, Types.INTEGER);
            }
            ps.setString(2, mentor.getSpecialization());
            ps.setInt(3, mentor.getMaxMentees());
            ps.setString(4, mentor.getStatus() != null ? mentor.getStatus() : "ACTIVE");
            ps.setLong(5, mentor.getUserId());
            int rows = ps.executeUpdate();
            if (rows > 0) {
                try (ResultSet rs = ps.getGeneratedKeys()) {
                    if (rs.next()) {
                        int id = rs.getInt(1);
                        mentor.setMentorId(id);
                        return id;
                    }
                }
            }
        }
        return -1;
    }

    public Mentor getMentorById(int mentorId) throws SQLException {
        String sql = """
            SELECT m.*, u.full_name, u.email, d.department_name,
                   (SELECT COUNT(*) FROM mentor_assignments ma
                    JOIN program_enrollments pe ON pe.enrollment_id = ma.enrollment_id
                    WHERE ma.mentor_id = m.mentor_id AND ma.ended_at IS NULL
                    AND pe.status = 'ACTIVE') AS current_mentee_count
            FROM mentors m
            JOIN users u ON m.user_id = u.user_id
            JOIN roles r ON u.role_id = r.role_id
            LEFT JOIN departments d ON m.department_id = d.department_id
            WHERE m.mentor_id = ? AND u.status = 'ACTIVE' AND r.role_name = 'MENTOR'
            """;
        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, mentorId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapResultSet(rs);
                }
            }
        }
        return null;
    }

    /**
     * Overload với connection cho transaction.
     */
    public Mentor getMentorById(int mentorId, Connection conn) throws SQLException {
        String sql = """
            SELECT m.*, u.full_name, u.email, d.department_name,
                   (SELECT COUNT(*) FROM mentor_assignments ma
                    JOIN program_enrollments pe ON pe.enrollment_id = ma.enrollment_id
                    WHERE ma.mentor_id = m.mentor_id AND ma.ended_at IS NULL
                    AND pe.status = 'ACTIVE') AS current_mentee_count
            FROM mentors m
            JOIN users u ON m.user_id = u.user_id
            JOIN roles r ON u.role_id = r.role_id
            LEFT JOIN departments d ON m.department_id = d.department_id
            WHERE m.mentor_id = ? AND u.status = 'ACTIVE' AND r.role_name = 'MENTOR'
            """;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, mentorId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapResultSet(rs);
                }
            }
        }
        return null;
    }

    public List<Mentor> getActiveMentors() throws SQLException {
        String sql = """
            SELECT m.*, u.full_name, u.email, d.department_name,
                   (SELECT COUNT(*) FROM mentor_assignments ma
                    JOIN program_enrollments pe ON pe.enrollment_id = ma.enrollment_id
                    WHERE ma.mentor_id = m.mentor_id AND ma.ended_at IS NULL
                    AND pe.status = 'ACTIVE') AS current_mentee_count
            FROM mentors m
            JOIN users u ON m.user_id = u.user_id
            JOIN roles r ON u.role_id = r.role_id
            LEFT JOIN departments d ON m.department_id = d.department_id
            WHERE m.status = 'ACTIVE' AND u.status = 'ACTIVE' AND r.role_name = 'MENTOR'
            ORDER BY u.full_name
            """;
        return executeListQuery(sql);
    }

    public List<Mentor> getAllMentors() throws SQLException {
        String sql = """
            SELECT m.*, u.full_name, u.email, d.department_name,
                   (SELECT COUNT(*) FROM mentor_assignments ma
                    JOIN program_enrollments pe ON pe.enrollment_id = ma.enrollment_id
                    WHERE ma.mentor_id = m.mentor_id AND ma.ended_at IS NULL
                    AND pe.status = 'ACTIVE') AS current_mentee_count
            FROM mentors m
            JOIN users u ON m.user_id = u.user_id
            LEFT JOIN departments d ON m.department_id = d.department_id
            ORDER BY u.full_name
            """;
        return executeListQuery(sql);
    }

    public boolean updateMentor(Mentor mentor) throws SQLException {
        String sql = """
            UPDATE mentors SET department_id = ?, specialization = ?, max_mentees = ?, status = ?
            WHERE mentor_id = ?
            """;
        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            if (mentor.getDepartmentId() != null) {
                ps.setInt(1, mentor.getDepartmentId());
            } else {
                ps.setNull(1, Types.INTEGER);
            }
            ps.setString(2, mentor.getSpecialization());
            ps.setInt(3, mentor.getMaxMentees());
            ps.setString(4, mentor.getStatus());
            ps.setInt(5, mentor.getMentorId());
            return ps.executeUpdate() > 0;
        }
    }

    // --- Private helpers ---

    private List<Mentor> executeListQuery(String sql) throws SQLException {
        List<Mentor> list = new ArrayList<>();
        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(mapResultSet(rs));
            }
        }
        return list;
    }

    private Mentor mapResultSet(ResultSet rs) throws SQLException {
        Mentor m = new Mentor();
        m.setMentorId(rs.getInt("mentor_id"));
        m.setUserId(rs.getLong("user_id"));
        int deptId = rs.getInt("department_id");
        m.setDepartmentId(rs.wasNull() ? null : deptId);
        m.setSpecialization(rs.getString("specialization"));
        m.setMaxMentees(rs.getInt("max_mentees"));
        m.setStatus(rs.getString("status"));
        m.setCreatedAt(rs.getTimestamp("created_at"));
        m.setUpdatedAt(rs.getTimestamp("updated_at"));
        try {
            m.setFullName(rs.getString("full_name"));
            m.setEmail(rs.getString("email"));
        } catch (SQLException ignored) { }
        try {
            m.setDepartmentName(rs.getString("department_name"));
        } catch (SQLException ignored) { }
        try {
            m.setCurrentMenteeCount(rs.getInt("current_mentee_count"));
        } catch (SQLException ignored) { }
        return m;
    }
}
