package com.internship.dao;

import com.internship.model.Department;
import com.internship.util.DBContext;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * DepartmentDAO: Xử lý CSDL cho bảng departments (BE3).
 */
public class DepartmentDAO {

    public int createDepartment(Department dept) throws SQLException {
        String sql = "INSERT INTO departments (department_name, description) VALUES (?, ?)";
        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, dept.getDepartmentName());
            ps.setString(2, dept.getDescription());
            int rows = ps.executeUpdate();
            if (rows > 0) {
                try (ResultSet rs = ps.getGeneratedKeys()) {
                    if (rs.next()) {
                        int id = rs.getInt(1);
                        dept.setDepartmentId(id);
                        return id;
                    }
                }
            }
        }
        return -1;
    }

    public Department getDepartmentById(int departmentId) throws SQLException {
        String sql = "SELECT * FROM departments WHERE department_id = ?";
        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, departmentId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapResultSet(rs);
                }
            }
        }
        return null;
    }

    public List<Department> getAllDepartments() throws SQLException {
        List<Department> list = new ArrayList<>();
        String sql = "SELECT * FROM departments ORDER BY department_name";
        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(mapResultSet(rs));
            }
        }
        return list;
    }

    public boolean updateDepartment(Department dept) throws SQLException {
        String sql = "UPDATE departments SET department_name = ?, description = ? WHERE department_id = ?";
        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, dept.getDepartmentName());
            ps.setString(2, dept.getDescription());
            ps.setInt(3, dept.getDepartmentId());
            return ps.executeUpdate() > 0;
        }
    }

    private Department mapResultSet(ResultSet rs) throws SQLException {
        Department d = new Department();
        d.setDepartmentId(rs.getInt("department_id"));
        d.setDepartmentName(rs.getString("department_name"));
        d.setDescription(rs.getString("description"));
        d.setCreatedAt(rs.getTimestamp("created_at"));
        d.setUpdatedAt(rs.getTimestamp("updated_at"));
        return d;
    }
}
