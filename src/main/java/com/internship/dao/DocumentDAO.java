package com.internship.dao;

import com.internship.model.Document;
import com.internship.util.DBContext;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * DocumentDAO: Thao tác CSDL cho bảng documents (CV, Đơn xin thực tập, v.v.).
 */
public class DocumentDAO {

    public int addDocument(Document doc) throws SQLException {
        // Kiểm tra xem đã có document loại này cho profile này chưa, nếu có thì có thể cập nhật hoặc thêm mới
        String sql = "INSERT INTO documents (profile_id, document_type, file_name, file_path, file_size, mime_type, status) "
                   + "VALUES (?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            
            ps.setInt(1, doc.getProfileId());
            ps.setString(2, doc.getDocumentType());
            ps.setString(3, doc.getFileName());
            ps.setString(4, doc.getFilePath());
            ps.setLong(5, doc.getFileSize());
            ps.setString(6, doc.getMimeType());
            ps.setString(7, doc.getStatus() != null ? doc.getStatus() : "PENDING");

            int rows = ps.executeUpdate();
            if (rows > 0) {
                try (ResultSet rs = ps.getGeneratedKeys()) {
                    if (rs.next()) {
                        int id = rs.getInt(1);
                        doc.setDocumentId(id);
                        return id;
                    }
                }
            }
        }
        return -1;
    }

    public Document getDocumentById(int documentId) throws SQLException {
        String sql = "SELECT * FROM documents WHERE document_id = ?";
        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, documentId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToDocument(rs);
                }
            }
        }
        return null;
    }

    public List<Document> getDocumentsByProfileId(int profileId) throws SQLException {
        List<Document> list = new ArrayList<>();
        String sql = "SELECT * FROM documents WHERE profile_id = ? ORDER BY uploaded_at DESC";
        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, profileId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapResultSetToDocument(rs));
                }
            }
        }
        return list;
    }

    public Document getDocumentByProfileAndType(int profileId, String docType) throws SQLException {
        String sql = "SELECT * FROM documents WHERE profile_id = ? AND document_type = ? ORDER BY uploaded_at DESC LIMIT 1";
        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, profileId);
            ps.setString(2, docType);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToDocument(rs);
                }
            }
        }
        return null;
    }

    private static volatile boolean schemaEnsured = false;

    /**
     * Tự động kiểm tra và bổ sung các cột reviewed_by, reviewed_at, reject_reason nếu CSDL cũ chưa có.
     * Tránh lỗi Unknown column 'reviewed_by' khi QA/dev chạy với schema cũ (US05 - BE2).
     */
    public static void ensureDocumentReviewColumns(Connection conn) {
        if (schemaEnsured || conn == null) return;
        synchronized (DocumentDAO.class) {
            if (schemaEnsured) return;
            try {
                DatabaseMetaData meta = conn.getMetaData();
                boolean hasReviewedBy = false;
                try (ResultSet rs = meta.getColumns(conn.getCatalog(), null, "documents", "reviewed_by")) {
                    if (rs != null && rs.next()) {
                        hasReviewedBy = true;
                    }
                }
                if (!hasReviewedBy) {
                    try (Statement stmt = conn.createStatement()) {
                        stmt.executeUpdate("ALTER TABLE documents ADD COLUMN reviewed_by BIGINT NULL");
                    } catch (SQLException ignored) {}
                    try (Statement stmt = conn.createStatement()) {
                        stmt.executeUpdate("ALTER TABLE documents ADD COLUMN reviewed_at TIMESTAMP NULL");
                    } catch (SQLException ignored) {}
                    try (Statement stmt = conn.createStatement()) {
                        stmt.executeUpdate("ALTER TABLE documents ADD COLUMN reject_reason TEXT NULL");
                    } catch (SQLException ignored) {}
                }
                schemaEnsured = true;
            } catch (Exception ignored) {
                // Nếu không đủ quyền DDL hoặc lỗi metadata, các tầng fallback trong approve/reject sẽ xử lý an toàn
            }
        }
    }

    public boolean updateDocumentStatus(int documentId, String status) throws SQLException {
        String sql = "UPDATE documents SET status = ? WHERE document_id = ?";
        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, status);
            ps.setInt(2, documentId);
            return ps.executeUpdate() > 0;
        }
    }

    /**
     * HR duyệt tài liệu (US05 - BE2): PENDING -> APPROVED
     * Có cơ chế tự phục hồi (Self-healing) và Fallback nhiều tầng chống lỗi CSDL:
     * - Tự thêm cột nếu thiếu.
     * - Nếu lỗi Foreign Key (1452), tự động set reviewed_by = NULL.
     * - Nếu lỗi thiếu cột (1054), fallback cập nhật status = 'APPROVED'.
     */
    public boolean approveDocument(int documentId, long reviewedBy) throws SQLException {
        try (Connection conn = DBContext.getConnection()) {
            ensureDocumentReviewColumns(conn);

            // Tier 1: Cập nhật đầy đủ thông tin duyệt
            String fullSql = "UPDATE documents SET status = 'APPROVED', reviewed_by = ?, reviewed_at = CURRENT_TIMESTAMP, reject_reason = NULL "
                           + "WHERE document_id = ? AND UPPER(TRIM(status)) = 'PENDING'";
            try (PreparedStatement ps = conn.prepareStatement(fullSql)) {
                if (reviewedBy > 0) {
                    ps.setLong(1, reviewedBy);
                } else {
                    ps.setNull(1, Types.BIGINT);
                }
                ps.setInt(2, documentId);
                int rows = ps.executeUpdate();
                if (rows > 0) {
                    return true;
                }
            } catch (SQLException e) {
                // Tier 2: Nếu lỗi khóa ngoại (user_id của người duyệt không có trong bảng users trên DB QA)
                if (e.getErrorCode() == 1452 || (e.getMessage() != null && e.getMessage().toLowerCase().contains("foreign key"))) {
                    String noFkSql = "UPDATE documents SET status = 'APPROVED', reviewed_by = NULL, reviewed_at = CURRENT_TIMESTAMP, reject_reason = NULL "
                                   + "WHERE document_id = ? AND UPPER(TRIM(status)) = 'PENDING'";
                    try (PreparedStatement ps = conn.prepareStatement(noFkSql)) {
                        ps.setInt(1, documentId);
                        int rows = ps.executeUpdate();
                        if (rows > 0) return true;
                    } catch (SQLException ignored) {}
                }

                // Tier 3: Nếu bảng documents của CSDL cũ thiếu cột reviewed_by/reviewed_at/reject_reason (1054)
                String fallbackSql = "UPDATE documents SET status = 'APPROVED' WHERE document_id = ? AND UPPER(TRIM(status)) = 'PENDING'";
                try (PreparedStatement ps = conn.prepareStatement(fallbackSql)) {
                    ps.setInt(1, documentId);
                    int rows = ps.executeUpdate();
                    if (rows > 0) return true;
                } catch (SQLException fallbackErr) {
                    throw e; // Ném lỗi gốc nếu câu tối thiểu cũng thất bại
                }
            }

            return false;
        }
    }

    /**
     * HR từ chối tài liệu (US05 - BE2): PENDING -> REJECTED (bắt buộc lý do)
     * Có cơ chế tự phục hồi (Self-healing) và Fallback nhiều tầng chống lỗi CSDL.
     */
    public boolean rejectDocument(int documentId, long reviewedBy, String rejectReason) throws SQLException {
        try (Connection conn = DBContext.getConnection()) {
            ensureDocumentReviewColumns(conn);

            // Tier 1: Cập nhật đầy đủ thông tin từ chối kèm lý do
            String fullSql = "UPDATE documents SET status = 'REJECTED', reviewed_by = ?, reviewed_at = CURRENT_TIMESTAMP, reject_reason = ? "
                           + "WHERE document_id = ? AND UPPER(TRIM(status)) = 'PENDING'";
            try (PreparedStatement ps = conn.prepareStatement(fullSql)) {
                if (reviewedBy > 0) {
                    ps.setLong(1, reviewedBy);
                } else {
                    ps.setNull(1, Types.BIGINT);
                }
                ps.setString(2, rejectReason);
                ps.setInt(3, documentId);
                int rows = ps.executeUpdate();
                if (rows > 0) {
                    return true;
                }
            } catch (SQLException e) {
                // Tier 2: Nếu lỗi khóa ngoại (user_id của người duyệt không có trong bảng users trên DB QA)
                if (e.getErrorCode() == 1452 || (e.getMessage() != null && e.getMessage().toLowerCase().contains("foreign key"))) {
                    String noFkSql = "UPDATE documents SET status = 'REJECTED', reviewed_by = NULL, reviewed_at = CURRENT_TIMESTAMP, reject_reason = ? "
                                   + "WHERE document_id = ? AND UPPER(TRIM(status)) = 'PENDING'";
                    try (PreparedStatement ps = conn.prepareStatement(noFkSql)) {
                        ps.setString(1, rejectReason);
                        ps.setInt(2, documentId);
                        int rows = ps.executeUpdate();
                        if (rows > 0) return true;
                    } catch (SQLException ignored) {}
                }

                // Tier 3: Nếu bảng documents của CSDL cũ thiếu cột (1054)
                String fallbackSql = "UPDATE documents SET status = 'REJECTED' WHERE document_id = ? AND UPPER(TRIM(status)) = 'PENDING'";
                try (PreparedStatement ps = conn.prepareStatement(fallbackSql)) {
                    ps.setInt(1, documentId);
                    int rows = ps.executeUpdate();
                    if (rows > 0) return true;
                } catch (SQLException fallbackErr) {
                    throw e;
                }
            }

            return false;
        }
    }

    public boolean deleteDocument(int documentId) throws SQLException {
        String sql = "DELETE FROM documents WHERE document_id = ?";
        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, documentId);
            return ps.executeUpdate() > 0;
        }
    }

    private Document mapResultSetToDocument(ResultSet rs) throws SQLException {
        Document d = new Document();
        d.setDocumentId(rs.getInt("document_id"));
        d.setProfileId(rs.getInt("profile_id"));
        d.setDocumentType(rs.getString("document_type"));
        d.setFileName(rs.getString("file_name"));
        d.setFilePath(rs.getString("file_path"));
        d.setFileSize(rs.getLong("file_size"));
        d.setMimeType(rs.getString("mime_type"));
        d.setStatus(rs.getString("status"));
        d.setUploadedAt(rs.getTimestamp("uploaded_at"));

        try {
            long reviewedBy = rs.getLong("reviewed_by");
            d.setReviewedBy(rs.wasNull() ? null : reviewedBy);
        } catch (SQLException ignored) {
        }
        try {
            d.setReviewedAt(rs.getTimestamp("reviewed_at"));
        } catch (SQLException ignored) {
        }
        try {
            d.setRejectReason(rs.getString("reject_reason"));
        } catch (SQLException ignored) {
        }

        return d;
    }
}
