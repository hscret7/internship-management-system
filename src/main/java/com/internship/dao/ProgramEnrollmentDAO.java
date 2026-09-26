package com.internship.dao;

import com.internship.model.ProgramEnrollment;
import com.internship.util.DBContext;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * ProgramEnrollmentDAO: Xử lý CSDL cho bảng program_enrollments (BE3).
 */
public class ProgramEnrollmentDAO {

    public void lockInternProfile(int internProfileId, Connection conn) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT profile_id FROM intern_profiles WHERE profile_id = ? FOR UPDATE")) {
            ps.setInt(1, internProfileId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) throw new IllegalStateException("Hồ sơ thực tập sinh không tồn tại.");
            }
        }
    }

    public ProgramEnrollment getEnrollmentByIdForUpdate(int enrollmentId, Connection conn)
            throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT * FROM program_enrollments WHERE enrollment_id = ? FOR UPDATE")) {
            ps.setInt(1, enrollmentId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapResultSet(rs) : null;
            }
        }
    }

    /**
     * Tạo enrollment mới (trong transaction).
     */
    public int createEnrollment(ProgramEnrollment enrollment, Connection conn) throws SQLException {
        String sql = """
            INSERT INTO program_enrollments (application_id, program_id, intern_profile_id, status)
            VALUES (?, ?, ?, 'ACTIVE')
            """;
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, enrollment.getApplicationId());
            ps.setInt(2, enrollment.getProgramId());
            ps.setInt(3, enrollment.getInternProfileId());
            int rows = ps.executeUpdate();
            if (rows > 0) {
                try (ResultSet rs = ps.getGeneratedKeys()) {
                    if (rs.next()) {
                        int id = rs.getInt(1);
                        enrollment.setEnrollmentId(id);
                        return id;
                    }
                }
            }
        }
        return -1;
    }

    public ProgramEnrollment getEnrollmentById(int enrollmentId) throws SQLException {
        String sql = """
            SELECT e.*, ip.full_name AS intern_name, p.program_name,
                   m.mentor_id AS current_mentor_id, u.full_name AS current_mentor_name
            FROM program_enrollments e
            JOIN intern_profiles ip ON e.intern_profile_id = ip.profile_id
            JOIN internship_programs p ON e.program_id = p.program_id
            LEFT JOIN mentor_assignments ma ON e.enrollment_id = ma.enrollment_id AND ma.ended_at IS NULL
            LEFT JOIN mentors m ON ma.mentor_id = m.mentor_id
            LEFT JOIN users u ON m.user_id = u.user_id
            WHERE e.enrollment_id = ?
            """;
        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, enrollmentId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapResultSet(rs);
                }
            }
        }
        return null;
    }

    public ProgramEnrollment getEnrollmentById(int enrollmentId, Connection conn) throws SQLException {
        String sql = """
            SELECT e.*, ip.full_name AS intern_name, p.program_name,
                   m.mentor_id AS current_mentor_id, u.full_name AS current_mentor_name
            FROM program_enrollments e
            JOIN intern_profiles ip ON e.intern_profile_id = ip.profile_id
            JOIN internship_programs p ON e.program_id = p.program_id
            LEFT JOIN mentor_assignments ma ON e.enrollment_id = ma.enrollment_id AND ma.ended_at IS NULL
            LEFT JOIN mentors m ON ma.mentor_id = m.mentor_id
            LEFT JOIN users u ON m.user_id = u.user_id
            WHERE e.enrollment_id = ?
            """;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, enrollmentId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapResultSet(rs);
                }
            }
        }
        return null;
    }

    /**
     * Kiểm tra intern có enrollment ACTIVE hay không (trong transaction).
     */
    public boolean hasActiveEnrollment(int internProfileId, Connection conn) throws SQLException {
        String sql = "SELECT COUNT(*) FROM program_enrollments WHERE intern_profile_id = ? AND status = 'ACTIVE'";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, internProfileId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt(1) > 0;
            }
        }
        return false;
    }

    /**
     * Kiểm tra application đã được ghi danh chưa (trong transaction).
     */
    public boolean hasEnrollmentForApplication(int applicationId, Connection conn) throws SQLException {
        String sql = "SELECT COUNT(*) FROM program_enrollments WHERE application_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, applicationId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt(1) > 0;
            }
        }
        return false;
    }

    /**
     * Lấy tất cả enrollment (HR xem).
     */
    public List<ProgramEnrollment> getAllEnrollments() throws SQLException {
        String sql = """
            SELECT e.*, ip.full_name AS intern_name, p.program_name,
                   m.mentor_id AS current_mentor_id, u.full_name AS current_mentor_name
            FROM program_enrollments e
            JOIN intern_profiles ip ON e.intern_profile_id = ip.profile_id
            JOIN internship_programs p ON e.program_id = p.program_id
            LEFT JOIN mentor_assignments ma ON e.enrollment_id = ma.enrollment_id AND ma.ended_at IS NULL
            LEFT JOIN mentors m ON ma.mentor_id = m.mentor_id
            LEFT JOIN users u ON m.user_id = u.user_id
            ORDER BY e.enrolled_at DESC
            """;
        return executeListQuery(sql);
    }

    /**
     * Lấy enrollment theo program.
     */
    public List<ProgramEnrollment> getEnrollmentsByProgram(int programId) throws SQLException {
        String sql = """
            SELECT e.*, ip.full_name AS intern_name, p.program_name,
                   m.mentor_id AS current_mentor_id, u.full_name AS current_mentor_name
            FROM program_enrollments e
            JOIN intern_profiles ip ON e.intern_profile_id = ip.profile_id
            JOIN internship_programs p ON e.program_id = p.program_id
            LEFT JOIN mentor_assignments ma ON e.enrollment_id = ma.enrollment_id AND ma.ended_at IS NULL
            LEFT JOIN mentors m ON ma.mentor_id = m.mentor_id
            LEFT JOIN users u ON m.user_id = u.user_id
            WHERE e.program_id = ?
            ORDER BY e.enrolled_at DESC
            """;
        List<ProgramEnrollment> list = new ArrayList<>();
        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, programId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapResultSet(rs));
                }
            }
        }
        return list;
    }

    /**
     * Lấy enrollment ACTIVE của intern (cho TTS xem).
     */
    public ProgramEnrollment getActiveEnrollmentByIntern(int internProfileId) throws SQLException {
        String sql = """
            SELECT e.*, ip.full_name AS intern_name, p.program_name,
                   m.mentor_id AS current_mentor_id, u.full_name AS current_mentor_name
            FROM program_enrollments e
            JOIN intern_profiles ip ON e.intern_profile_id = ip.profile_id
            JOIN internship_programs p ON e.program_id = p.program_id
            LEFT JOIN mentor_assignments ma ON e.enrollment_id = ma.enrollment_id AND ma.ended_at IS NULL
            LEFT JOIN mentors m ON ma.mentor_id = m.mentor_id
            LEFT JOIN users u ON m.user_id = u.user_id
            WHERE e.intern_profile_id = ? AND e.status = 'ACTIVE'
            """;
        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, internProfileId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapResultSet(rs);
                }
            }
        }
        return null;
    }

    /**
     * Cập nhật trạng thái enrollment.
     */
    public boolean updateEnrollmentStatus(int enrollmentId, String status, Connection conn)
            throws SQLException {
        String sql = "UPDATE program_enrollments SET status = ? WHERE enrollment_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, status);
            ps.setInt(2, enrollmentId);
            return ps.executeUpdate() > 0;
        }
    }

    // --- Private helpers ---

    private List<ProgramEnrollment> executeListQuery(String sql) throws SQLException {
        List<ProgramEnrollment> list = new ArrayList<>();
        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(mapResultSet(rs));
            }
        }
        return list;
    }

    private ProgramEnrollment mapResultSet(ResultSet rs) throws SQLException {
        ProgramEnrollment e = new ProgramEnrollment();
        e.setEnrollmentId(rs.getInt("enrollment_id"));
        e.setApplicationId(rs.getInt("application_id"));
        e.setProgramId(rs.getInt("program_id"));
        e.setInternProfileId(rs.getInt("intern_profile_id"));
        e.setStatus(rs.getString("status"));
        e.setEnrolledAt(rs.getTimestamp("enrolled_at"));
        e.setCompletedAt(rs.getTimestamp("completed_at"));
        e.setCreatedAt(rs.getTimestamp("created_at"));
        e.setUpdatedAt(rs.getTimestamp("updated_at"));
        try {
            e.setInternName(rs.getString("intern_name"));
        } catch (SQLException ignored) { }
        try {
            e.setProgramName(rs.getString("program_name"));
        } catch (SQLException ignored) { }
        try {
            int mentorId = rs.getInt("current_mentor_id");
            e.setCurrentMentorId(rs.wasNull() ? null : mentorId);
            e.setCurrentMentorName(rs.getString("current_mentor_name"));
        } catch (SQLException ignored) { }
        return e;
    }
}
