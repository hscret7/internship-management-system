package com.internship.dao;

import com.internship.model.Role;
import com.internship.model.RegistrationProfile;
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

    /** Compare-and-set prevents overwriting a concurrent password change/reset. */
    public boolean changePasswordIfCurrent(long userId, String expectedHash, String newHash)
            throws SQLException {
        String sql = "UPDATE users SET password_hash = ?, updated_at = CURRENT_TIMESTAMP "
                + "WHERE user_id = ? AND BINARY password_hash = BINARY ? AND status = 'ACTIVE'";
        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, newHash);
            statement.setLong(2, userId);
            statement.setString(3, expectedHash);
            return statement.executeUpdate() == 1;
        }
    }

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

    /** Owns one transaction: neither a partial account nor a partial profile is committed. */
    public long createWithProfile(User user, RegistrationProfile profile) throws SQLException {
        try (Connection connection = openRegistrationConnection()) {
            connection.setAutoCommit(false);
            try {
                long id;
                try (PreparedStatement ps = connection.prepareStatement(
                        "INSERT INTO users (username,email,password_hash,full_name,role_id,status) VALUES (?,?,?,?,?,?)",
                        Statement.RETURN_GENERATED_KEYS)) {
                    ps.setString(1, user.getUsername()); ps.setString(2, user.getEmail());
                    ps.setString(3, user.getPasswordHash()); ps.setString(4, user.getFullName());
                    ps.setLong(5, user.getRole().getRoleId()); ps.setString(6, user.getStatus().name());
                    if (ps.executeUpdate() != 1) throw new SQLException("Account insert failed.");
                    try (ResultSet keys = ps.getGeneratedKeys()) {
                        if (!keys.next() || (id = keys.getLong(1)) <= 0) throw new SQLException("Missing account ID.");
                    }
                }
                try (PreparedStatement ps = connection.prepareStatement(
                        "INSERT INTO intern_profiles (user_id,full_name,email,phone,university,major,gpa,status) VALUES (?,?,?,?,?,?,?,?)")) {
                    ps.setLong(1, id); ps.setString(2, user.getFullName()); ps.setString(3, user.getEmail());
                    ps.setString(4, profile.phone()); ps.setString(5, profile.university());
                    ps.setString(6, profile.major());
                    if (profile.gpa() == null) ps.setNull(7, java.sql.Types.DECIMAL);
                    else ps.setBigDecimal(7, profile.gpa());
                    ps.setString(8, "ACTIVE");
                    if (ps.executeUpdate() != 1) throw new SQLException("Profile insert failed.");
                }
                recordRegistrationAudit(connection,id);
                connection.commit();
                return id;
            } catch (SQLException | RuntimeException failure) {
                try { connection.rollback(); } catch (SQLException rollback) { failure.addSuppressed(rollback); }
                throw failure;
            }
        }
    }

    protected void recordRegistrationAudit(Connection connection,long userId) throws SQLException {
        // Public registration has no authenticated actor. Record only the new object ID.
        new AuditLogDAO().insertInTransaction(connection,null,"REGISTER","USER",userId);
    }

    protected Connection openRegistrationConnection() throws SQLException {
        return DBConnection.getConnection();
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