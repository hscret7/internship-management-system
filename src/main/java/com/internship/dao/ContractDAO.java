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

    public int saveOrUpdateContract(Contract contract) throws SQLException {
        // Kiểm tra xem application_id đã có hợp đồng chưa vì application_id là UNIQUE
        String checkSql = "SELECT contract_id FROM contracts WHERE application_id = ?";
        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(checkSql)) {
            ps.setInt(1, contract.getApplicationId());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    int existingId = rs.getInt("contract_id");
                    String updateSql = "UPDATE contracts SET file_name = ?, file_path = ?, file_size = ?, "
                                     + "mime_type = ?, uploaded_by = ?, uploaded_at = CURRENT_TIMESTAMP, "
                                     + "status = 'PENDING_CONFIRMATION', confirmed_at = NULL WHERE contract_id = ?";
                    try (PreparedStatement updatePs = conn.prepareStatement(updateSql)) {
                        updatePs.setString(1, contract.getFileName());
                        updatePs.setString(2, contract.getFilePath());
                        updatePs.setLong(3, contract.getFileSize());
                        updatePs.setString(4, contract.getMimeType());
                        updatePs.setLong(5, contract.getUploadedBy());
                        updatePs.setInt(6, existingId);
                        updatePs.executeUpdate();
                        contract.setContractId(existingId);
                        return existingId;
                    }
                }
            }
        }

        String insertSql = "INSERT INTO contracts (application_id, file_name, file_path, file_size, mime_type, uploaded_by, status) "
                         + "VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(insertSql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, contract.getApplicationId());
            ps.setString(2, contract.getFileName());
            ps.setString(3, contract.getFilePath());
            ps.setLong(4, contract.getFileSize());
            ps.setString(5, contract.getMimeType());
            ps.setLong(6, contract.getUploadedBy());
            ps.setString(7, contract.getStatus() != null ? contract.getStatus() : "PENDING_CONFIRMATION");

            int rows = ps.executeUpdate();
            if (rows > 0) {
                try (ResultSet rs = ps.getGeneratedKeys()) {
                    if (rs.next()) {
                        int id = rs.getInt(1);
                        contract.setContractId(id);
                        return id;
                    }
                }
            }
        }
        return -1;
    }

    public Contract getContractById(int contractId) throws SQLException {
        String sql = "SELECT * FROM contracts WHERE contract_id = ?";
        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, contractId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToContract(rs);
                }
            }
        }
        return null;
    }

    public Contract getContractByApplicationId(int applicationId) throws SQLException {
        String sql = "SELECT * FROM contracts WHERE application_id = ?";
        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, applicationId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToContract(rs);
                }
            }
        }
        return null;
    }

    /**
     * Lấy hợp đồng theo profile_id của thực tập sinh.
     */
    public Contract getContractByProfileId(int profileId) throws SQLException {
        String sql = "SELECT c.* FROM contracts c "
                   + "JOIN applications a ON c.application_id = a.application_id "
                   + "WHERE a.intern_id = ? "
                   + "ORDER BY c.contract_id DESC LIMIT 1";
        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, profileId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToContract(rs);
                }
            }
        }
        return null;
    }

    /**
     * TTS bấm Xác nhận hợp đồng (US10): Lưu confirmed_at và chuyển trạng thái sang CONFIRMED.
     */
    public boolean confirmContract(int contractId) throws SQLException {
        String sql = "UPDATE contracts SET status = 'CONFIRMED', confirmed_at = CURRENT_TIMESTAMP WHERE contract_id = ?";
        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, contractId);
            return ps.executeUpdate() > 0;
        }
    }

    public List<Contract> getAllContracts() throws SQLException {
        List<Contract> list = new ArrayList<>();
        String sql = "SELECT * FROM contracts ORDER BY contract_id DESC";
        try (Connection conn = DBContext.getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                list.add(mapResultSetToContract(rs));
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
        c.setConfirmedAt(rs.getTimestamp("confirmed_at"));
        return c;
    }
}
