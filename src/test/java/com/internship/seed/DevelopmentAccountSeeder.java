package com.internship.seed;

import com.internship.dao.RoleDAO;
import com.internship.dao.UserDAO;
import com.internship.model.Role;
import com.internship.model.User;
import com.internship.model.UserStatus;
import com.internship.util.PasswordUtil;

public class DevelopmentAccountSeeder {

    private static final String PASSWORD_ENV =
            "INTERNSHIP_DEV_PASSWORD";

    public static void main(String[] args) {

        String password = System.getenv(PASSWORD_ENV);

        if (password == null || password.isBlank()) {
            System.err.println(
                    "Missing environment variable: "
                            + PASSWORD_ENV
            );
            System.exit(1);
        }

        UserDAO userDAO = new UserDAO();
        RoleDAO roleDAO = new RoleDAO();

        try {
            createAccountIfMissing(
                    userDAO,
                    roleDAO,
                    "admin_test",
                    "admin_test@example.com",
                    "Admin Test",
                    "ADMIN",
                    password
            );

            createAccountIfMissing(
                    userDAO,
                    roleDAO,
                    "hr_test",
                    "hr_test@example.com",
                    "HR Test",
                    "HR",
                    password
            );

            createAccountIfMissing(
                    userDAO,
                    roleDAO,
                    "mentor_test",
                    "mentor_test@example.com",
                    "Mentor Test",
                    "MENTOR",
                    password
            );

            createAccountIfMissing(
                    userDAO,
                    roleDAO,
                    "intern_test",
                    "intern_test@example.com",
                    "Intern Test",
                    "INTERN",
                    password
            );

            System.out.println(
                    "=== DEVELOPMENT ACCOUNTS READY ==="
            );

        } catch (Exception e) {

            System.err.println(
                    "=== DEVELOPMENT ACCOUNT SEED FAILED ==="
            );

            e.printStackTrace();
            System.exit(1);
        }
    }

    private static void createAccountIfMissing(
            UserDAO userDAO,
            RoleDAO roleDAO,
            String username,
            String email,
            String fullName,
            String roleName,
            String password
    ) throws Exception {

        if (userDAO.existsByUsername(username)
                || userDAO.existsByEmail(email)) {

            System.out.println(
                    "Skipped existing account: " + username
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
                PasswordUtil.hashPassword(password)
        );
        user.setRole(role);
        user.setStatus(UserStatus.ACTIVE);

        long userId = userDAO.create(user);

        System.out.println(
                "Created: "
                        + username
                        + " | "
                        + roleName
                        + " | ID="
                        + userId
        );
    }
}
