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
     */
    public boolean approveDocument(int documentId, long reviewedBy) throws SQLException {
        String sql = "UPDATE documents SET status = 'APPROVED', reviewed_by = ?, reviewed_at = CURRENT_TIMESTAMP, reject_reason = NULL WHERE document_id = ? AND status = 'PENDING'";
        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, reviewedBy);
            ps.setInt(2, documentId);
            return ps.executeUpdate() > 0;
        }
    }

    /**
     * HR từ chối tài liệu (US05 - BE2): PENDING -> REJECTED (bắt buộc lý do)
     */
    public boolean rejectDocument(int documentId, long reviewedBy, String rejectReason) throws SQLException {
        String sql = "UPDATE documents SET status = 'REJECTED', reviewed_by = ?, reviewed_at = CURRENT_TIMESTAMP, reject_reason = ? WHERE document_id = ? AND status = 'PENDING'";
        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, reviewedBy);
            ps.setString(2, rejectReason);
            ps.setInt(3, documentId);
            return ps.executeUpdate() > 0;
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
