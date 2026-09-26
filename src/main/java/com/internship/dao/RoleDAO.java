package com.internship.dao;

import com.internship.model.Role;
import com.internship.util.DBConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

public class RoleDAO {

    public Role findById(long roleId) throws SQLException {
        String sql = """
                SELECT role_id, role_name, description, created_at
                FROM roles
                WHERE role_id = ?
                """;

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setLong(1, roleId);

            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return mapRole(resultSet);
                }
            }
        }

        return null;
    }

    public Role findByName(String roleName) throws SQLException {
        String sql = """
                SELECT role_id, role_name, description, created_at
                FROM roles
                WHERE role_name = ?
                """;

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, roleName);

            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return mapRole(resultSet);
                }
            }
        }

        return null;
    }

    public List<Role> findAll() throws SQLException {
        String sql = """
                SELECT role_id, role_name, description, created_at
                FROM roles
                ORDER BY role_id
                """;

        List<Role> roles = new ArrayList<>();

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {

            while (resultSet.next()) {
                roles.add(mapRole(resultSet));
            }
        }

        return roles;
    }

    private Role mapRole(ResultSet resultSet) throws SQLException {
        Role role = new Role();

        role.setRoleId(resultSet.getLong("role_id"));
        role.setRoleName(resultSet.getString("role_name"));
        role.setDescription(resultSet.getString("description"));

        Timestamp createdAt = resultSet.getTimestamp("created_at");
        if (createdAt != null) {
            role.setCreatedAt(createdAt.toLocalDateTime());
        }

        return role;
    }
}