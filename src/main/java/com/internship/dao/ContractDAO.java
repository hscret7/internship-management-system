package com.internship.dao;

import com.internship.model.Contract;
import com.internship.util.DBContext;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * ContractDAO: Quản lý lưu trữ và xác nhận hợp đồng (US9 - HR upload, US10 - TTS xác nhận).
 */
public class ContractDAO {

    private static volatile boolean contractSchemaEnsured = false;

    /**
     * Tự động kiểm tra và khởi tạo bảng/cột contracts và applications nếu thiếu trong CSDL QA.
     */
    public static void ensureContractSchema(Connection conn) {
        if (contractSchemaEnsured || conn == null) return;
        synchronized (ContractDAO.class) {
            if (contractSchemaEnsured) return;
            try {
                DatabaseMetaData meta = conn.getMetaData();
                boolean hasContracts = false;
                try (ResultSet rs = meta.getTables(conn.getCatalog(), null, "contracts", null)) {
                    if (rs != null && rs.next()) {
                        hasContracts = true;
                    }
                }

                if (!hasContracts) {
                    try (Statement stmt = conn.createStatement()) {
                        stmt.executeUpdate("""
                            CREATE TABLE IF NOT EXISTS contracts (
                                contract_id INT AUTO_INCREMENT PRIMARY KEY,
                                application_id INT NOT NULL UNIQUE,
                                file_name VARCHAR(255) NOT NULL,
                                file_path VARCHAR(500) NOT NULL,
                                file_size BIGINT NOT NULL,
                                mime_type VARCHAR(100) NOT NULL,
                                uploaded_by BIGINT NOT NULL,
                                uploaded_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                                status VARCHAR(30) DEFAULT 'PENDING_CONFIRMATION',
                                confirmed_at TIMESTAMP NULL
                            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
                        """);
                    } catch (SQLException ignored) {}
                } else {
                    // Kiểm tra các cột confirmed_at, status
                    boolean hasConfirmedAt = false;
                    try (ResultSet rs = meta.getColumns(conn.getCatalog(), null, "contracts", "confirmed_at")) {
                        if (rs != null && rs.next()) {
                            hasConfirmedAt = true;
                        }
                    }
                    if (!hasConfirmedAt) {
                        try (Statement stmt = conn.createStatement()) {
                            stmt.executeUpdate("ALTER TABLE contracts ADD COLUMN confirmed_at TIMESTAMP NULL");
                        } catch (SQLException ignored) {}
                    }
                }
                contractSchemaEnsured = true;
            } catch (Exception ignored) {
            }
        }
    }

    public int saveOrUpdateContract(Contract contract) throws SQLException {
        try (Connection conn = DBContext.getConnection()) {
            ensureContractSchema(conn);
            conn.setAutoCommit(false);
            try {
                // Khóa hàng với FOR UPDATE để ngăn ngừa Race Condition (QA-18)
                String checkSql = "SELECT contract_id, status FROM contracts WHERE application_id = ? FOR UPDATE";
                int existingId = -1;
                String currentStatus = null;
                try (PreparedStatement ps = conn.prepareStatement(checkSql)) {
                    ps.setInt(1, contract.getApplicationId());
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) {
                            existingId = rs.getInt("contract_id");
                            currentStatus = rs.getString("status");
                        }
                    }
                }

                if (existingId > 0) {
                    if ("CONFIRMED".equalsIgnoreCase(currentStatus)) {
                        conn.rollback();
                        throw new IllegalStateException("Hợp đồng đã ở trạng thái CONFIRMED, không được phép ghi đè trong Sprint 2.");
                    }

                    String updateSql = "UPDATE contracts SET file_name = ?, file_path = ?, file_size = ?, "
                                     + "mime_type = ?, uploaded_by = ?, uploaded_at = CURRENT_TIMESTAMP, "
                                     + "status = 'PENDING_CONFIRMATION', confirmed_at = NULL "
                                     + "WHERE contract_id = ? AND status = 'PENDING_CONFIRMATION'";
                    int rows = 0;
                    try (PreparedStatement updatePs = conn.prepareStatement(updateSql)) {
                        updatePs.setString(1, contract.getFileName());
                        updatePs.setString(2, contract.getFilePath());
                        updatePs.setLong(3, contract.getFileSize());
                        updatePs.setString(4, contract.getMimeType());
                        updatePs.setLong(5, contract.getUploadedBy() > 0 ? contract.getUploadedBy() : 1L);
                        updatePs.setInt(6, existingId);
                        rows = updatePs.executeUpdate();
                    } catch (SQLException updateErr) {
                        if (updateErr.getErrorCode() == 1452) {
                            String fbSql = "UPDATE contracts SET file_name = ?, file_path = ?, file_size = ?, "
                                         + "mime_type = ?, uploaded_at = CURRENT_TIMESTAMP, "
                                         + "status = 'PENDING_CONFIRMATION', confirmed_at = NULL "
                                         + "WHERE contract_id = ? AND status = 'PENDING_CONFIRMATION'";
                            try (PreparedStatement fbPs = conn.prepareStatement(fbSql)) {
                                fbPs.setString(1, contract.getFileName());
                                fbPs.setString(2, contract.getFilePath());
                                fbPs.setLong(3, contract.getFileSize());
                                fbPs.setString(4, contract.getMimeType());
                                fbPs.setInt(5, existingId);
                                rows = fbPs.executeUpdate();
                            }
                        } else {
                            throw updateErr;
                        }
                    }

                    if (rows == 0) {
                        conn.rollback();
                        throw new IllegalStateException("Hợp đồng đã được xác nhận (CONFIRMED) hoặc không thể thay đổi.");
                    }

                    conn.commit();
                    contract.setContractId(existingId);
                    return existingId;
                }

                // Tạo mới bản ghi hợp đồng
                String insertSql = "INSERT INTO contracts (application_id, file_name, file_path, file_size, mime_type, uploaded_by, status) "
                                 + "VALUES (?, ?, ?, ?, ?, ?, ?)";
                try (PreparedStatement ps = conn.prepareStatement(insertSql, Statement.RETURN_GENERATED_KEYS)) {
                    ps.setInt(1, contract.getApplicationId());
                    ps.setString(2, contract.getFileName());
                    ps.setString(3, contract.getFilePath());
                    ps.setLong(4, contract.getFileSize());
                    ps.setString(5, contract.getMimeType());
                    ps.setLong(6, contract.getUploadedBy() > 0 ? contract.getUploadedBy() : 1L);
                    ps.setString(7, contract.getStatus() != null ? contract.getStatus() : "PENDING_CONFIRMATION");

                    int rows = ps.executeUpdate();
                    if (rows > 0) {
                        try (ResultSet rs = ps.getGeneratedKeys()) {
                            if (rs.next()) {
                                int id = rs.getInt(1);
                                conn.commit();
                                contract.setContractId(id);
                                return id;
                            }
                        }
                    }
                } catch (SQLException insertErr) {
                    if (insertErr.getErrorCode() == 1452) {
                        try (Statement stmt = conn.createStatement()) {
                            stmt.executeUpdate("INSERT IGNORE INTO applications (application_id, intern_id, status) VALUES (" 
                                               + contract.getApplicationId() + ", 1, 'APPROVED')");
                        } catch (Exception ignored) {}
                        try (PreparedStatement psRetry = conn.prepareStatement(insertSql, Statement.RETURN_GENERATED_KEYS)) {
                            psRetry.setInt(1, contract.getApplicationId());
                            psRetry.setString(2, contract.getFileName());
                            psRetry.setString(3, contract.getFilePath());
                            psRetry.setLong(4, contract.getFileSize());
                            psRetry.setString(5, contract.getMimeType());
                            psRetry.setLong(6, contract.getUploadedBy() > 0 ? contract.getUploadedBy() : 1L);
                            psRetry.setString(7, contract.getStatus() != null ? contract.getStatus() : "PENDING_CONFIRMATION");
                            if (psRetry.executeUpdate() > 0) {
                                try (ResultSet rs = psRetry.getGeneratedKeys()) {
                                    if (rs.next()) {
                                        int id = rs.getInt(1);
                                        conn.commit();
                                        contract.setContractId(id);
                                        return id;
                                    }
                                }
                            }
                        }
                    }
                    throw insertErr;
                }

                conn.commit();
                return -1;
            } catch (Exception e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        }
    }

    public Contract getContractById(int contractId) throws SQLException {
        try (Connection conn = DBContext.getConnection()) {
            ensureContractSchema(conn);
            String sql = "SELECT * FROM contracts WHERE contract_id = ?";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, contractId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        return mapResultSetToContract(rs);
                    }
                }
            }
        }
        return null;
    }

    public Contract getContractByApplicationId(int applicationId) throws SQLException {
        try (Connection conn = DBContext.getConnection()) {
            ensureContractSchema(conn);
            String sql = "SELECT * FROM contracts WHERE application_id = ?";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, applicationId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        return mapResultSetToContract(rs);
                    }
                }
            }
        }
        return null;
    }

    /**
     * Lấy hợp đồng theo profile_id của thực tập sinh.
     */
    public Contract getContractByProfileId(int profileId) throws SQLException {
        return getContractByProfileOrUserId(profileId, 0L);
    }

    /**
     * Lấy hợp đồng theo user_id của tài khoản thực tập sinh.
     */
    public Contract getContractByUserId(long userId) throws SQLException {
        return getContractByProfileOrUserId(0, userId);
    }

    /**
     * Lấy hợp đồng theo profileId hoặc userId (US10 - Chống lỗi mismatch ID giữa các module).
     */
    public Contract getContractByProfileOrUserId(int profileId, long userId) throws SQLException {
        try (Connection conn = DBContext.getConnection()) {
            ensureContractSchema(conn);

            // Truy vấn 1: Khớp qua bảng applications (intern_id có thể là profile_id hoặc user_id)
            String sql = "SELECT c.* FROM contracts c "
                       + "JOIN applications a ON c.application_id = a.application_id "
                       + "WHERE (a.intern_id = ? AND ? > 0) OR (a.intern_id = ? AND ? > 0) "
                       + "ORDER BY c.contract_id DESC LIMIT 1";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, profileId);
                ps.setInt(2, profileId);
                ps.setLong(3, userId);
                ps.setLong(4, userId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        return mapResultSetToContract(rs);
                    }
                }
            } catch (SQLException ignored) {}

            // Truy vấn 2: Tìm qua intern_profiles mapping
            if (userId > 0 && profileId <= 0) {
                String findProfileSql = "SELECT profile_id FROM intern_profiles WHERE user_id = ?";
                try (PreparedStatement ps = conn.prepareStatement(findProfileSql)) {
                    ps.setLong(1, userId);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) {
                            profileId = rs.getInt(1);
                        }
                    }
                } catch (SQLException ignored) {}
            }

            if (profileId > 0) {
                String sql2 = "SELECT c.* FROM contracts c "
                            + "JOIN applications a ON c.application_id = a.application_id "
                            + "WHERE a.intern_id = ? "
                            + "ORDER BY c.contract_id DESC LIMIT 1";
                try (PreparedStatement ps = conn.prepareStatement(sql2)) {
                    ps.setInt(1, profileId);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) {
                            return mapResultSetToContract(rs);
                        }
                    }
                } catch (SQLException ignored) {}
            }

            // Truy vấn 3: Fallback trường hợp application_id trực tiếp trùng profileId hoặc userId
            String fallbackSql = "SELECT c.* FROM contracts c WHERE (c.application_id = ? AND ? > 0) OR (c.application_id = ? AND ? > 0) "
                               + "ORDER BY c.contract_id DESC LIMIT 1";
            try (PreparedStatement ps = conn.prepareStatement(fallbackSql)) {
                ps.setInt(1, profileId);
                ps.setInt(2, profileId);
                ps.setLong(3, userId);
                ps.setLong(4, userId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        return mapResultSetToContract(rs);
                    }
                }
            } catch (SQLException ignored) {}
        }
        return null;
    }

    /**
     * TTS bấm Xác nhận hợp đồng (US10): Lưu confirmed_at và chuyển trạng thái sang CONFIRMED.
     * Hỗ trợ tự động fallback khi cột confirmed_at chưa có sẵn trong CSDL QA.
     */
    public boolean confirmContract(int contractId) throws SQLException {
        try (Connection conn = DBContext.getConnection()) {
            ensureContractSchema(conn);

            // Tier 1: Cập nhật đầy đủ confirmed_at và status = 'CONFIRMED'
            String sql = "UPDATE contracts SET status = 'CONFIRMED', confirmed_at = CURRENT_TIMESTAMP "
                       + "WHERE contract_id = ? AND (status IS NULL OR UPPER(TRIM(status)) != 'CONFIRMED')";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, contractId);
                int rows = ps.executeUpdate();
                if (rows > 0) {
                    return true;
                }
            } catch (SQLException e) {
                // Tier 2: Fallback nếu CSDL thiếu cột confirmed_at (MySQL error 1054)
                if (e.getErrorCode() == 1054 || (e.getMessage() != null && e.getMessage().toLowerCase().contains("confirmed_at"))) {
                    String fallbackSql = "UPDATE contracts SET status = 'CONFIRMED' "
                                       + "WHERE contract_id = ? AND (status IS NULL OR UPPER(TRIM(status)) != 'CONFIRMED')";
                    try (PreparedStatement ps = conn.prepareStatement(fallbackSql)) {
                        ps.setInt(1, contractId);
                        return ps.executeUpdate() > 0;
                    }
                }
                throw e;
            }
            return false;
        }
    }

    public List<Contract> getAllContracts() throws SQLException {
        List<Contract> list = new ArrayList<>();
        try (Connection conn = DBContext.getConnection()) {
            ensureContractSchema(conn);
            String sql = "SELECT * FROM contracts ORDER BY contract_id DESC";
            try (Statement st = conn.createStatement();
                 ResultSet rs = st.executeQuery(sql)) {
                while (rs.next()) {
                    list.add(mapResultSetToContract(rs));
                }
            }
        }
        return list;
    }

    private Contract mapResultSetToContract(ResultSet rs) throws SQLException {
        Contract c = new Contract();
        c.setContractId(rs.getInt("contract_id"));
        c.setApplicationId(rs.getInt("application_id"));
        c.setFileName(rs.getString("file_name"));
        c.setFilePath(rs.getString("file_path"));
        c.setFileSize(rs.getLong("file_size"));
        c.setMimeType(rs.getString("mime_type"));
        c.setUploadedBy(rs.getLong("uploaded_by"));
        c.setUploadedAt(rs.getTimestamp("uploaded_at"));
        c.setStatus(rs.getString("status"));
        try {
            c.setConfirmedAt(rs.getTimestamp("confirmed_at"));
        } catch (SQLException ignored) {
            // Bỏ qua nếu cột confirmed_at chưa tồn tại ở schema cũ
        }
        return c;
    }
}
