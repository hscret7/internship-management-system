package com.internship.dao;

import com.internship.model.Allowance;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class AllowanceDAO {
    
    public void create(Allowance allowance, Connection conn) throws SQLException {
        String sql = "INSERT INTO allowances (enrollment_id, allowance_type, period, amount, created_by) VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setInt(1, allowance.getEnrollmentId());
            stmt.setString(2, allowance.getAllowanceType());
            stmt.setString(3, allowance.getPeriod());
            stmt.setBigDecimal(4, allowance.getAmount());
            stmt.setLong(5, allowance.getCreatedBy());
            stmt.executeUpdate();
        }
    }

    public boolean updateStatus(int allowanceId, String status, Connection conn) throws SQLException {
        String sql = "UPDATE allowances SET status = ?, paid_at = ? WHERE allowance_id = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, status);
            if ("PAID".equals(status)) {
                stmt.setTimestamp(2, Timestamp.valueOf(LocalDateTime.now()));
            } else {
                stmt.setNull(2, Types.TIMESTAMP);
            }
            stmt.setInt(3, allowanceId);
            return stmt.executeUpdate() > 0;
        }
    }

    public List<Allowance> list(Integer enrollmentId, Long ownerId, Connection conn) throws SQLException {
        StringBuilder sql = new StringBuilder(
            "SELECT a.*, ip.full_name as intern_name FROM allowances a " +
            "JOIN program_enrollments pe ON a.enrollment_id = pe.enrollment_id " +
            "JOIN intern_profiles ip ON pe.intern_profile_id = ip.profile_id WHERE 1=1"
        );
        List<Object> params = new ArrayList<>();
        if (enrollmentId != null) {
            sql.append(" AND a.enrollment_id = ?");
            params.add(enrollmentId);
        }
        if (ownerId != null) {
            sql.append(" AND ip.user_id = ?");
            params.add(ownerId);
        }
        sql.append(" ORDER BY a.period DESC, a.created_at DESC");

        List<Allowance> result = new ArrayList<>();
        try (PreparedStatement stmt = conn.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) {
                stmt.setObject(i + 1, params.get(i));
            }
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Allowance a = new Allowance();
                    a.setAllowanceId(rs.getInt("allowance_id"));
                    a.setEnrollmentId(rs.getInt("enrollment_id"));
                    a.setAllowanceType(rs.getString("allowance_type"));
                    a.setPeriod(rs.getString("period"));
                    a.setAmount(rs.getBigDecimal("amount"));
                    a.setStatus(rs.getString("status"));
                    Timestamp paid = rs.getTimestamp("paid_at");
                    if (paid != null) a.setPaidAt(paid.toLocalDateTime());
                    a.setCreatedBy(rs.getLong("created_by"));
                    a.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
                    a.setInternName(rs.getString("intern_name"));
                    result.add(a);
                }
            }
        }
        return result;
    }
}
