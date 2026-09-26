package com.internship.dao;

import com.internship.model.Application;
import com.internship.util.DBContext;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * ApplicationDAO: Xử lý CSDL cho bảng applications (BE3).
 * Bảng applications thuộc schema BE2 – DAO này chỉ đọc/cập nhật,
 * không tạo lại bảng.
 */
public class ApplicationDAO {

    /**
     * Tạo đơn ứng tuyển mới (trạng thái PENDING).
     */
    public int createApplication(int internId, Connection conn) throws SQLException {
        String sql = "INSERT INTO applications (intern_id, status) VALUES (?, 'PENDING')";
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, internId);
            int rows = ps.executeUpdate();
            if (rows > 0) {
                try (ResultSet rs = ps.getGeneratedKeys()) {
                    if (rs.next()) {
                        return rs.getInt(1);
                    }
                }
            }
        }
        return -1;
    }

    /**
     * Overload: tạo đơn với connection tự quản lý.
     */
    public int createApplication(int internId) throws SQLException {
        try (Connection conn = DBContext.getConnection()) {
            return createApplication(internId, conn);
        }
    }

    /** Serializes submissions for the same profile before checking PENDING applications. */
    public Integer lockInternProfileId(long userId, Connection conn) throws SQLException {
        String sql = "SELECT profile_id FROM intern_profiles WHERE user_id = ? FOR UPDATE";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : null;
            }
        }
    }

    public boolean hasPendingApplication(int internId, Connection conn) throws SQLException {
        String sql = "SELECT application_id FROM applications WHERE intern_id = ? AND status = 'PENDING' LIMIT 1";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, internId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    /**
     * Lấy Application theo ID.
     */
    public Application getApplicationById(int applicationId) throws SQLException {
        String sql = """
            SELECT a.*, ip.full_name AS intern_name, ip.email AS intern_email,
                   u.full_name AS reviewer_name
            FROM applications a
            JOIN intern_profiles ip ON a.intern_id = ip.profile_id
            LEFT JOIN users u ON a.reviewed_by = u.user_id
            WHERE a.application_id = ?
            """;
        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, applicationId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapResultSet(rs);
                }
            }
        }
        return null;
    }

    /**
     * Lấy Application theo ID sử dụng connection cho sẵn (trong transaction).
     */
    public Application getApplicationById(int applicationId, Connection conn) throws SQLException {
        String sql = """
            SELECT a.*, ip.full_name AS intern_name, ip.email AS intern_email,
                   u.full_name AS reviewer_name
            FROM applications a
            JOIN intern_profiles ip ON a.intern_id = ip.profile_id
            LEFT JOIN users u ON a.reviewed_by = u.user_id
            WHERE a.application_id = ?
            """;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, applicationId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapResultSet(rs);
                }
            }
        }
        return null;
    }

    public Application getApplicationByIdForUpdate(int applicationId, Connection conn)
            throws SQLException {
        String sql = "SELECT * FROM applications WHERE application_id = ? FOR UPDATE";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, applicationId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapResultSet(rs) : null;
            }
        }
    }

    /**
     * Kiểm tra intern đã có application PENDING chưa.
     */
    public boolean hasPendingApplication(int internId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM applications WHERE intern_id = ? AND status = 'PENDING'";
        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, internId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }
        }
        return false;
    }

    /**
     * Lấy tất cả đơn PENDING (cho HR xem danh sách chờ duyệt).
     */
    public List<Application> getPendingApplications() throws SQLException {
        String sql = """
            SELECT a.*, ip.full_name AS intern_name, ip.email AS intern_email,
                   u.full_name AS reviewer_name
            FROM applications a
            JOIN intern_profiles ip ON a.intern_id = ip.profile_id
            LEFT JOIN users u ON a.reviewed_by = u.user_id
            WHERE a.status = 'PENDING'
            ORDER BY a.submitted_at ASC
            """;
        return executeListQuery(sql);
    }

    /**
     * Lấy tất cả đơn (cho HR xem toàn bộ, có phân loại).
     */
    public List<Application> getAllApplications() throws SQLException {
        String sql = """
            SELECT a.*, ip.full_name AS intern_name, ip.email AS intern_email,
                   u.full_name AS reviewer_name
            FROM applications a
            JOIN intern_profiles ip ON a.intern_id = ip.profile_id
            LEFT JOIN users u ON a.reviewed_by = u.user_id
            ORDER BY a.submitted_at DESC
            """;
        return executeListQuery(sql);
    }

    /**
     * Lấy đơn của intern (TTS xem đơn của mình).
     */
    public List<Application> getApplicationsByInternId(int internId) throws SQLException {
        String sql = """
            SELECT a.*, ip.full_name AS intern_name, ip.email AS intern_email,
                   u.full_name AS reviewer_name
            FROM applications a
            JOIN intern_profiles ip ON a.intern_id = ip.profile_id
            LEFT JOIN users u ON a.reviewed_by = u.user_id
            WHERE a.intern_id = ?
            ORDER BY a.submitted_at DESC
            """;
        List<Application> list = new ArrayList<>();
        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, internId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapResultSet(rs));
                }
            }
        }
        return list;
    }

    /**
     * Duyệt đơn: chỉ cập nhật nếu đơn đang PENDING.
     * @return true nếu cập nhật thành công (đơn đang PENDING).
     */
    public boolean approveApplication(int applicationId, long reviewedBy, Connection conn)
            throws SQLException {
        String sql = """
            UPDATE applications
            SET status = 'APPROVED', reviewed_by = ?, reviewed_at = CURRENT_TIMESTAMP
            WHERE application_id = ? AND status = 'PENDING'
            """;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, reviewedBy);
            ps.setInt(2, applicationId);
            return ps.executeUpdate() > 0;
        }
    }

    /**
     * Từ chối đơn: chỉ cập nhật nếu đơn đang PENDING.
     * @return true nếu cập nhật thành công (đơn đang PENDING).
     */
    public boolean rejectApplication(int applicationId, long reviewedBy,
                                     String rejectReason, Connection conn) throws SQLException {
        String sql = """
            UPDATE applications
            SET status = 'REJECTED', reviewed_by = ?, reviewed_at = CURRENT_TIMESTAMP,
                reject_reason = ?
            WHERE application_id = ? AND status = 'PENDING'
            """;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, reviewedBy);
            ps.setString(2, rejectReason);
            ps.setInt(3, applicationId);
            return ps.executeUpdate() > 0;
        }
    }

    // --- Private helpers ---

    private List<Application> executeListQuery(String sql) throws SQLException {
        List<Application> list = new ArrayList<>();
        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(mapResultSet(rs));
            }
        }
        return list;
    }

    private Application mapResultSet(ResultSet rs) throws SQLException {
        Application app = new Application();
        app.setApplicationId(rs.getInt("application_id"));
        app.setInternId(rs.getInt("intern_id"));
        app.setStatus(rs.getString("status"));
        app.setSubmittedAt(rs.getTimestamp("submitted_at"));

        long reviewedBy = rs.getLong("reviewed_by");
        app.setReviewedBy(rs.wasNull() ? null : reviewedBy);

        app.setReviewedAt(rs.getTimestamp("reviewed_at"));
        app.setRejectReason(rs.getString("reject_reason"));

        // Transient display fields (may not exist in all queries)
        try {
            app.setInternName(rs.getString("intern_name"));
        } catch (SQLException ignored) { }
        try {
            app.setInternEmail(rs.getString("intern_email"));
        } catch (SQLException ignored) { }
        try {
            app.setReviewerName(rs.getString("reviewer_name"));
        } catch (SQLException ignored) { }

        return app;
    }
}
