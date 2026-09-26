package com.internship.dao;

import com.internship.model.InternProfile;
import com.internship.util.DBContext;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * InternProfileDAO: Xử lý các thao tác CSDL cho bảng intern_profiles.
 */
public class InternProfileDAO {

    public int createProfile(InternProfile profile) throws SQLException {
        String sql = "INSERT INTO intern_profiles (user_id, full_name, dob, gender, phone, email, university, major, gpa, skills, status) "
                   + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        
        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            
            ps.setLong(1, profile.getUserId());
            ps.setString(2, profile.getFullName());
            ps.setDate(3, profile.getDob());
            ps.setString(4, profile.getGender());
            ps.setString(5, profile.getPhone());
            ps.setString(6, profile.getEmail());
            ps.setString(7, profile.getUniversity());
            ps.setString(8, profile.getMajor());
            if (profile.getGpa() != null) {
                ps.setDouble(9, profile.getGpa());
            } else {
                ps.setNull(9, Types.DECIMAL);
            }
            ps.setString(10, profile.getSkills());
            ps.setString(11, profile.getStatus() != null ? profile.getStatus() : "ACTIVE");

            int rows = ps.executeUpdate();
            if (rows > 0) {
                try (ResultSet rs = ps.getGeneratedKeys()) {
                    if (rs.next()) {
                        int id = rs.getInt(1);
                        profile.setProfileId(id);
                        return id;
                    }
                }
            }
        }
        return -1;
    }

    public boolean updateProfile(InternProfile profile) throws SQLException {
        String sql = "UPDATE intern_profiles SET full_name = ?, dob = ?, gender = ?, phone = ?, email = ?, "
                   + "university = ?, major = ?, gpa = ?, skills = ?, status = ? WHERE profile_id = ?";
        
        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            
            ps.setString(1, profile.getFullName());
            ps.setDate(2, profile.getDob());
            ps.setString(3, profile.getGender());
            ps.setString(4, profile.getPhone());
            ps.setString(5, profile.getEmail());
            ps.setString(6, profile.getUniversity());
            ps.setString(7, profile.getMajor());
            if (profile.getGpa() != null) {
                ps.setDouble(8, profile.getGpa());
            } else {
                ps.setNull(8, Types.DECIMAL);
            }
            ps.setString(9, profile.getSkills());
            ps.setString(10, profile.getStatus());
            ps.setInt(11, profile.getProfileId());

            return ps.executeUpdate() > 0;
        }
    }

    public InternProfile getProfileById(int profileId) throws SQLException {
        String sql = "SELECT * FROM intern_profiles WHERE profile_id = ?";
        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, profileId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToProfile(rs);
                }
            }
        }
        return null;
    }

    public InternProfile getProfileByUserId(long userId) throws SQLException {
        String sql = "SELECT * FROM intern_profiles WHERE user_id = ?";
        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToProfile(rs);
                }
            }
        }
        return null;
    }

    public List<InternProfile> getAllProfiles() throws SQLException {
        List<InternProfile> list = new ArrayList<>();
        String sql = "SELECT * FROM intern_profiles ORDER BY profile_id DESC";
        try (Connection conn = DBContext.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                list.add(mapResultSetToProfile(rs));
            }
        }
        return list;
    }

    /**
     * Tìm kiếm theo tên, trường, ngành (Yêu cầu BE2 - Ảnh 1).
     */
    public List<InternProfile> searchProfiles(String name, String university, String major) throws SQLException {
        List<InternProfile> list = new ArrayList<>();
        StringBuilder sql = new StringBuilder("SELECT * FROM intern_profiles WHERE 1=1 ");
        List<Object> params = new ArrayList<>();

        if (name != null && !name.trim().isEmpty()) {
            sql.append("AND LOWER(full_name) LIKE ? ");
            params.add("%" + name.trim().toLowerCase() + "%");
        }
        if (university != null && !university.trim().isEmpty()) {
            sql.append("AND LOWER(university) LIKE ? ");
            params.add("%" + university.trim().toLowerCase() + "%");
        }
        if (major != null && !major.trim().isEmpty()) {
            sql.append("AND LOWER(major) LIKE ? ");
            params.add("%" + major.trim().toLowerCase() + "%");
        }
        sql.append("ORDER BY profile_id DESC");

        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) {
                ps.setObject(i + 1, params.get(i));
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapResultSetToProfile(rs));
                }
            }
        }
        return list;
    }

    public boolean updateStatus(int profileId, String status) throws SQLException {
        String sql = "UPDATE intern_profiles SET status = ? WHERE profile_id = ?";
        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, status);
            ps.setInt(2, profileId);
            return ps.executeUpdate() > 0;
        }
    }

    private InternProfile mapResultSetToProfile(ResultSet rs) throws SQLException {
        InternProfile p = new InternProfile();
        p.setProfileId(rs.getInt("profile_id"));
        p.setUserId(rs.getLong("user_id"));
        p.setFullName(rs.getString("full_name"));
        p.setDob(rs.getDate("dob"));
        p.setGender(rs.getString("gender"));
        p.setPhone(rs.getString("phone"));
        p.setEmail(rs.getString("email"));
        p.setUniversity(rs.getString("university"));
        p.setMajor(rs.getString("major"));
        double gpa = rs.getDouble("gpa");
        p.setGpa(rs.wasNull() ? null : gpa);
        p.setSkills(rs.getString("skills"));
        p.setStatus(rs.getString("status"));
        p.setCreatedAt(rs.getTimestamp("created_at"));
        p.setUpdatedAt(rs.getTimestamp("updated_at"));
        return p;
    }
}
