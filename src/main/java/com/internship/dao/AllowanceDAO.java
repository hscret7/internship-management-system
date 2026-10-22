package com.internship.dao;

import com.internship.model.Allowance;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class AllowanceDAO {
    public void requireEnrollment(int id, Connection conn) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("SELECT enrollment_id FROM program_enrollments WHERE enrollment_id=?")) {
            ps.setInt(1, id);
            try (ResultSet rs=ps.executeQuery()) { if(!rs.next()) throw new IllegalArgumentException("Không tìm thấy ghi danh."); }
        }
    }
    
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
        String sql = "UPDATE allowances SET status = ?, paid_at = ? WHERE allowance_id = ? AND status = 'PENDING'";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, status);
            stmt.setObject(2, java.time.LocalDateTime.now(java.time.ZoneId.of("Asia/Ho_Chi_Minh")));
            stmt.setInt(3, allowanceId);
            return stmt.executeUpdate() > 0;
        }
    }

    public List<Allowance> list(Integer enrollmentId, Long ownerId, Connection conn) throws SQLException {
        StringBuilder sql = new StringBuilder(
            "SELECT a.*, ip.full_name as intern_name, COALESCE(u.email,ip.email) AS intern_email, p.program_name FROM allowances a " +
            "JOIN program_enrollments pe ON a.enrollment_id = pe.enrollment_id " +
            "JOIN intern_profiles ip ON pe.intern_profile_id = ip.profile_id LEFT JOIN users u ON u.user_id=ip.user_id JOIN internship_programs p ON p.program_id=pe.program_id WHERE 1=1"
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
                    a.setPaidAt(rs.getObject("paid_at", LocalDateTime.class));
                    a.setCreatedBy(rs.getLong("created_by"));
                    a.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
                    a.setInternName(rs.getString("intern_name"));
                    a.setInternEmail(rs.getString("intern_email"));
                    a.setProgramName(rs.getString("program_name"));
                    result.add(a);
                }
            }
        }
        return result;
    }
}
