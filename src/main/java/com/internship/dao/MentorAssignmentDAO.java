package com.internship.dao;

import com.internship.model.MentorAssignment;
import com.internship.util.DBContext;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * MentorAssignmentDAO: Xử lý CSDL cho bảng mentor_assignments (BE3).
 */
public class MentorAssignmentDAO {

    /**
     * Tạo mentor assignment mới (trong transaction).
     */
    public int createAssignment(MentorAssignment assignment, Connection conn) throws SQLException {
        String sql = """
            INSERT INTO mentor_assignments (enrollment_id, mentor_id, assigned_by)
            VALUES (?, ?, ?)
            """;
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, assignment.getEnrollmentId());
            ps.setInt(2, assignment.getMentorId());
            ps.setLong(3, assignment.getAssignedBy());
            int rows = ps.executeUpdate();
            if (rows > 0) {
                try (ResultSet rs = ps.getGeneratedKeys()) {
                    if (rs.next()) {
                        int id = rs.getInt(1);
                        assignment.setAssignmentId(id);
                        return id;
                    }
                }
            }
        }
        return -1;
    }

    /**
     * Kết thúc assignment hiện tại (đổi mentor).
     */
    public boolean endActiveAssignment(int enrollmentId, String endReason, Connection conn)
            throws SQLException {
        String sql = """
            UPDATE mentor_assignments
            SET ended_at = CURRENT_TIMESTAMP, end_reason = ?
            WHERE enrollment_id = ? AND ended_at IS NULL
            """;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, endReason);
            ps.setInt(2, enrollmentId);
            return ps.executeUpdate() == 1;
        }
    }

    /**
     * Lấy mentor assignment hiện hành (ended_at IS NULL) cho enrollment.
     */
    public MentorAssignment getActiveAssignment(int enrollmentId) throws SQLException {
        String sql = """
            SELECT ma.*, u_mentor.full_name AS mentor_name, u_by.full_name AS assigned_by_name
            FROM mentor_assignments ma
            JOIN mentors m ON ma.mentor_id = m.mentor_id
            JOIN users u_mentor ON m.user_id = u_mentor.user_id
            JOIN users u_by ON ma.assigned_by = u_by.user_id
            WHERE ma.enrollment_id = ? AND ma.ended_at IS NULL
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

    public MentorAssignment getActiveAssignment(int enrollmentId, Connection conn) throws SQLException {
        String sql = "SELECT * FROM mentor_assignments WHERE enrollment_id = ? AND ended_at IS NULL";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, enrollmentId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapResultSet(rs) : null;
            }
        }
    }

    /**
     * Kiểm tra enrollment có assignment đang hiệu lực hay không (trong transaction).
     */
    public boolean hasActiveAssignment(int enrollmentId, Connection conn) throws SQLException {
        String sql = "SELECT COUNT(*) FROM mentor_assignments WHERE enrollment_id = ? AND ended_at IS NULL";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, enrollmentId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt(1) > 0;
            }
        }
        return false;
    }

    /**
     * Lấy lịch sử assignment cho enrollment.
     */
    public List<MentorAssignment> getAssignmentHistory(int enrollmentId) throws SQLException {
        String sql = """
            SELECT ma.*, u_mentor.full_name AS mentor_name, u_by.full_name AS assigned_by_name
            FROM mentor_assignments ma
            JOIN mentors m ON ma.mentor_id = m.mentor_id
            JOIN users u_mentor ON m.user_id = u_mentor.user_id
            JOIN users u_by ON ma.assigned_by = u_by.user_id
            WHERE ma.enrollment_id = ?
            ORDER BY ma.assigned_at DESC
            """;
        List<MentorAssignment> list = new ArrayList<>();
        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, enrollmentId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapResultSet(rs));
                }
            }
        }
        return list;
    }

    // --- Private helpers ---

    private MentorAssignment mapResultSet(ResultSet rs) throws SQLException {
        MentorAssignment ma = new MentorAssignment();
        ma.setAssignmentId(rs.getInt("assignment_id"));
        ma.setEnrollmentId(rs.getInt("enrollment_id"));
        ma.setMentorId(rs.getInt("mentor_id"));
        ma.setAssignedAt(rs.getTimestamp("assigned_at"));
        ma.setAssignedBy(rs.getLong("assigned_by"));
        ma.setEndedAt(rs.getTimestamp("ended_at"));
        ma.setEndReason(rs.getString("end_reason"));
        ma.setCreatedAt(rs.getTimestamp("created_at"));
        try {
            ma.setMentorName(rs.getString("mentor_name"));
        } catch (SQLException ignored) { }
        try {
            ma.setAssignedByName(rs.getString("assigned_by_name"));
        } catch (SQLException ignored) { }
        return ma;
    }
}
