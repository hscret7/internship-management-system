package com.internship.dao;

import com.internship.model.Role;
import com.internship.model.User;
import com.internship.model.UserStatus;
import com.internship.util.DBConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

public class UserDAO {

    public User findById(long userId) throws SQLException {
        String sql = """
                SELECT
                    u.user_id,
                    u.username,
                    u.email,
                    u.password_hash,
                    u.full_name,
                    u.status,
                    u.created_at,
                    u.updated_at,
                    r.role_id,
                    r.role_name,
                    r.description,
                    r.created_at AS role_created_at
                FROM users u
                JOIN roles r ON u.role_id = r.role_id
                WHERE u.user_id = ?
                """;

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setLong(1, userId);

            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return mapUser(resultSet);
                }
            }
        }

        return null;
    }

    public User findByUsername(String username) throws SQLException {
        String sql = """
                SELECT
                    u.user_id,
                    u.username,
                    u.email,
                    u.password_hash,
                    u.full_name,
                    u.status,
                    u.created_at,
                    u.updated_at,
                    r.role_id,
                    r.role_name,
                    r.description,
                    r.created_at AS role_created_at
                FROM users u
                JOIN roles r ON u.role_id = r.role_id
                WHERE u.username = ?
                """;

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, username);

            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return mapUser(resultSet);
                }
            }
        }

        return null;
    }

    public User findByEmail(String email) throws SQLException {
        String sql = """
                SELECT
                    u.user_id,
                    u.username,
                    u.email,
                    u.password_hash,
                    u.full_name,
                    u.status,
                    u.created_at,
                    u.updated_at,
                    r.role_id,
                    r.role_name,
                    r.description,
                    r.created_at AS role_created_at
                FROM users u
                JOIN roles r ON u.role_id = r.role_id
                WHERE u.email = ?
                """;

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, email);

            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return mapUser(resultSet);
                }
            }
        }

        return null;
    }

    public List<User> findAll() throws SQLException {
        String sql = """
                SELECT
                    u.user_id,
                    u.username,
                    u.email,
                    u.password_hash,
                    u.full_name,
                    u.status,
                    u.created_at,
                    u.updated_at,
                    r.role_id,
                    r.role_name,
                    r.description,
                    r.created_at AS role_created_at
                FROM users u
                JOIN roles r ON u.role_id = r.role_id
                ORDER BY u.user_id
                """;

        List<User> users = new ArrayList<>();

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {

            while (resultSet.next()) {
                users.add(mapUser(resultSet));
            }
        }

        return users;
    }

    public long create(User user) throws SQLException {
        String sql = """
                INSERT INTO users
                    (username, email, password_hash, full_name, role_id, status)
                VALUES (?, ?, ?, ?, ?, ?)
                """;

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     sql,
                     Statement.RETURN_GENERATED_KEYS
             )) {

            statement.setString(1, user.getUsername());
            statement.setString(2, user.getEmail());
            statement.setString(3, user.getPasswordHash());
            statement.setString(4, user.getFullName());
            statement.setLong(5, user.getRole().getRoleId());
            statement.setString(6, user.getStatus().name());

            int affectedRows = statement.executeUpdate();

            if (affectedRows == 0) {
                throw new SQLException("Creating user failed, no rows affected.");
            }

            try (ResultSet generatedKeys = statement.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    return generatedKeys.getLong(1);
                }
            }

            throw new SQLException("Creating user failed, no ID obtained.");
        }
    }

    public boolean updateStatus(long userId, UserStatus status)
            throws SQLException {

        String sql = """
                UPDATE users
                SET status = ?
                WHERE user_id = ?
                """;

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, status.name());
            statement.setLong(2, userId);

            return statement.executeUpdate() > 0;
        }
    }

    public boolean existsByUsername(String username) throws SQLException {
        String sql = """
                SELECT 1
                FROM users
                WHERE username = ?
                LIMIT 1
                """;

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, username);

            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next();
            }
        }
    }

    public boolean existsByEmail(String email) throws SQLException {
        String sql = """
                SELECT 1
                FROM users
                WHERE email = ?
                LIMIT 1
                """;

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, email);

            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next();
            }
        }
    }

    private User mapUser(ResultSet resultSet) throws SQLException {
        Role role = new Role();

        role.setRoleId(resultSet.getLong("role_id"));
        role.setRoleName(resultSet.getString("role_name"));
        role.setDescription(resultSet.getString("description"));

        Timestamp roleCreatedAt =
                resultSet.getTimestamp("role_created_at");

        if (roleCreatedAt != null) {
            role.setCreatedAt(roleCreatedAt.toLocalDateTime());
        }

        User user = new User();

        user.setUserId(resultSet.getLong("user_id"));
        user.setUsername(resultSet.getString("username"));
        user.setEmail(resultSet.getString("email"));
        user.setPasswordHash(resultSet.getString("password_hash"));
        user.setFullName(resultSet.getString("full_name"));
        user.setRole(role);
        user.setStatus(
                UserStatus.valueOf(resultSet.getString("status"))
        );

        Timestamp createdAt =
                resultSet.getTimestamp("created_at");

        if (createdAt != null) {
            user.setCreatedAt(createdAt.toLocalDateTime());
        }

        Timestamp updatedAt =
                resultSet.getTimestamp("updated_at");

        if (updatedAt != null) {
            user.setUpdatedAt(updatedAt.toLocalDateTime());
        }

        return user;
    }
}