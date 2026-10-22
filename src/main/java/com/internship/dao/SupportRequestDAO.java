package com.internship.dao;

import com.internship.model.SupportRequest;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class SupportRequestDAO {

    public void insert(SupportRequest req, Connection conn) throws SQLException {
        String sql = "INSERT INTO support_requests (profile_id, request_type, title, content, status) VALUES (?, ?, ?, ?, 'PENDING')";
        try (PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setInt(1, req.getProfileId());
            stmt.setString(2, req.getRequestType());
            stmt.setString(3, req.getTitle());
            stmt.setString(4, req.getContent());
            stmt.executeUpdate();
            try (ResultSet rs = stmt.getGeneratedKeys()) {
                if (rs.next()) {
                    req.setRequestId(rs.getInt(1));
                }
            }
        }
    }

    public void updateResponse(SupportRequest req, Connection conn) throws SQLException {
        String sql = "UPDATE support_requests SET status = ?, response = ?, handled_by = ?, handled_at = ? WHERE request_id = ? AND status IN ('PENDING','IN_PROGRESS')";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, req.getStatus());
            stmt.setString(2, req.getResponse());
            if (req.getHandledBy() != null) {
                stmt.setLong(3, req.getHandledBy());
            } else {
                stmt.setNull(3, Types.BIGINT);
            }
            stmt.setObject(4, req.getHandledAt());
            stmt.setInt(5, req.getRequestId());
            if (stmt.executeUpdate() != 1) throw new IllegalArgumentException("Yêu cầu đã được xử lý hoặc không còn tồn tại.");
        }
    }

    public SupportRequest findById(int requestId, Connection conn) throws SQLException {
        String sql = "SELECT r.*, p.full_name as intern_name, u.full_name as handler_name " +
                     "FROM support_requests r " +
                     "JOIN intern_profiles p ON r.profile_id = p.profile_id " +
                     "LEFT JOIN users u ON r.handled_by = u.user_id " +
                     "WHERE r.request_id = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, requestId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapRow(rs);
                }
            }
        }
        return null;
    }

    public List<SupportRequest> findByProfileId(int profileId, Connection conn) throws SQLException {
        String sql = "SELECT r.*, p.full_name as intern_name, u.full_name as handler_name " +
                     "FROM support_requests r " +
                     "JOIN intern_profiles p ON r.profile_id = p.profile_id " +
                     "LEFT JOIN users u ON r.handled_by = u.user_id " +
                     "WHERE r.profile_id = ? " +
                     "ORDER BY r.created_at DESC";
        List<SupportRequest> list = new ArrayList<>();
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, profileId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRow(rs));
                }
            }
        }
        return list;
    }

    public List<SupportRequest> findAll(Connection conn) throws SQLException {
        String sql = "SELECT r.*, p.full_name as intern_name, u.full_name as handler_name " +
                     "FROM support_requests r " +
                     "JOIN intern_profiles p ON r.profile_id = p.profile_id " +
                     "LEFT JOIN users u ON r.handled_by = u.user_id " +
                     "ORDER BY r.created_at DESC";
        List<SupportRequest> list = new ArrayList<>();
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRow(rs));
                }
            }
        }
        return list;
    }

    private SupportRequest mapRow(ResultSet rs) throws SQLException {
        SupportRequest r = new SupportRequest();
        r.setRequestId(rs.getInt("request_id"));
        r.setProfileId(rs.getInt("profile_id"));
        r.setRequestType(rs.getString("request_type"));
        r.setTitle(rs.getString("title"));
        r.setContent(rs.getString("content"));
        r.setStatus(rs.getString("status"));
        r.setResponse(rs.getString("response"));
        
        long handledBy = rs.getLong("handled_by");
        if (!rs.wasNull()) {
            r.setHandledBy(handledBy);
        }
        
        r.setHandledAt(rs.getObject("handled_at", java.time.LocalDateTime.class));
        
        Timestamp createdAt = rs.getTimestamp("created_at");
        if (createdAt != null) {
            r.setCreatedAt(createdAt.toLocalDateTime());
        }
        
        r.setInternName(rs.getString("intern_name"));
        r.setHandlerName(rs.getString("handler_name"));
        
        return r;
    }
}
