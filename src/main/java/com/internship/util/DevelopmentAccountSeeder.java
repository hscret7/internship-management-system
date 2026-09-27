package com.internship.util;

import com.internship.dao.RoleDAO;
import com.internship.dao.UserDAO;
import com.internship.model.Role;
import com.internship.model.User;
import com.internship.model.UserStatus;

import java.sql.SQLException;

public class DevelopmentAccountSeeder {

    private static final String PASSWORD_ENV = "INTERNSHIP_DEV_PASSWORD";

    private final UserDAO userDAO;
    private final RoleDAO roleDAO;

    public DevelopmentAccountSeeder() {
        this.userDAO = new UserDAO();
        this.roleDAO = new RoleDAO();
    }

    public static void main(String[] args) {
        String password = System.getenv(PASSWORD_ENV);

        if (password == null || password.isBlank()) {
            System.err.println(
                    "ERROR: Environment variable "
                            + PASSWORD_ENV
                            + " is not set."
            );
            System.exit(1);
        }

        try {
            DevelopmentAccountSeeder seeder =
                    new DevelopmentAccountSeeder();

            seeder.seed(password);

            System.out.println(
                    "Development account seeding completed."
            );

        } catch (Exception e) {
            System.err.println(
                    "Development account seeding failed: "
                            + e.getMessage()
            );
            System.exit(1);
        }
    }

    public void seed(String plainPassword) throws SQLException {
        createAccountIfMissing(
                "admin.dev",
                "admin.dev@internship.local",
                "Development Admin",
                "ADMIN",
                plainPassword
        );

        createAccountIfMissing(
                "hr.dev",
                "hr.dev@internship.local",
                "Development HR",
                "HR",
                plainPassword
        );

        createAccountIfMissing(
                "mentor.dev",
                "mentor.dev@internship.local",
                "Development Mentor",
                "MENTOR",
                plainPassword
        );

        createAccountIfMissing(
                "intern.dev",
                "intern.dev@internship.local",
                "Development Intern",
                "INTERN",
                plainPassword
        );
    }

    private void createAccountIfMissing(
            String username,
            String email,
            String fullName,
            String roleName,
            String plainPassword
    ) throws SQLException {

        if (userDAO.existsByUsername(username)) {
            System.out.println(
                    "SKIP: username already exists -> " + username
            );
            return;
        }

        if (userDAO.existsByEmail(email)) {
            System.out.println(
                    "SKIP: email already exists -> " + email
            );
            return;
        }

        Role role = roleDAO.findByName(roleName);

        if (role == null) {
            throw new IllegalStateException(
                    "Role not found: " + roleName
            );
        }

        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user.setFullName(fullName);
        user.setPasswordHash(
                PasswordUtil.hashPassword(plainPassword)
        );
        user.setRole(role);
        user.setStatus(UserStatus.ACTIVE);

        long userId = userDAO.create(user);

        System.out.println(
                "CREATED: "
                        + username
                        + " | role="
                        + roleName
                        + " | userId="
                        + userId
        );
    }
}
