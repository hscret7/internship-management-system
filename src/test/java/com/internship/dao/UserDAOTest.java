package com.internship.dao;

import com.internship.model.Role;
import com.internship.model.User;
import com.internship.model.UserStatus;
import com.internship.util.DBConnection;
import com.internship.util.PasswordUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;

public class UserDAOTest {

    private static final String TEST_USERNAME = "be1_test_user";
    private static final String TEST_EMAIL = "be1_test_user@example.com";

    public static void main(String[] args) {

        UserDAO userDAO = new UserDAO();
        RoleDAO roleDAO = new RoleDAO();

        try {
            // Đảm bảo dữ liệu test cũ không còn nếu lần test trước bị dừng giữa chừng.
            deleteTestUser();

            System.out.println("=== USER DAO TEST ===");

            Role internRole = roleDAO.findByName("INTERN");

            if (internRole == null) {
                throw new IllegalStateException(
                        "INTERN role not found."
                );
            }

            String plainPassword = "Test@123";
            String passwordHash =
                    PasswordUtil.hashPassword(plainPassword);

            User user = new User();

            user.setUsername(TEST_USERNAME);
            user.setEmail(TEST_EMAIL);
            user.setPasswordHash(passwordHash);
            user.setFullName("BE1 Test User");
            user.setRole(internRole);
            user.setStatus(UserStatus.ACTIVE);

            long userId = userDAO.create(user);

            System.out.println(
                    "Created user ID: " + userId
            );

            if (userId <= 0) {
                throw new IllegalStateException(
                        "Generated user ID is invalid."
                );
            }

            User savedUser =
                    userDAO.findByUsername(TEST_USERNAME);

            if (savedUser == null) {
                throw new IllegalStateException(
                        "Created user could not be read."
                );
            }

            System.out.println(
                    "Username: " + savedUser.getUsername()
            );

            System.out.println(
                    "Role: " + savedUser.getRole().getRoleName()
            );

            System.out.println(
                    "Status: " + savedUser.getStatus()
            );

            boolean passwordMatches =
                    PasswordUtil.verifyPassword(
                            plainPassword,
                            savedUser.getPasswordHash()
                    );

            System.out.println(
                    "Password verified: " + passwordMatches
            );

            if (!passwordMatches) {
                throw new IllegalStateException(
                        "Stored password hash verification failed."
                );
            }

            if (!"INTERN".equals(
                    savedUser.getRole().getRoleName())) {

                throw new IllegalStateException(
                        "User role mapping failed."
                );
            }

            if (savedUser.getStatus()
                    != UserStatus.ACTIVE) {

                throw new IllegalStateException(
                        "User status mapping failed."
                );
            }

            System.out.println(
                    "=== USER DAO TEST SUCCESS ==="
            );

        } catch (Exception e) {

            System.err.println(
                    "=== USER DAO TEST FAILED ==="
            );

            e.printStackTrace();

        } finally {

            try {
                deleteTestUser();

                System.out.println(
                        "Test user cleaned up."
                );

            } catch (Exception e) {

                System.err.println(
                        "Could not clean up test user."
                );

                e.printStackTrace();
            }
        }
    }

    private static void deleteTestUser()
            throws Exception {

        String sql = """
                DELETE FROM users
                WHERE username = ?
                   OR email = ?
                """;

        try (Connection connection =
                     DBConnection.getConnection();

             PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setString(
                    1,
                    TEST_USERNAME
            );

            statement.setString(
                    2,
                    TEST_EMAIL
            );

            statement.executeUpdate();
        }
    }
}
