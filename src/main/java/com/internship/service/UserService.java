package com.internship.service;

import java.sql.SQLException;
import com.internship.dao.RoleDAO;
import com.internship.dao.UserDAO;
import com.internship.model.Role;
import com.internship.model.User;
import com.internship.model.UserStatus;
import com.internship.service.email.EmailException;
import com.internship.service.email.EmailService;
import com.internship.util.PasswordUtil;

import java.security.SecureRandom;
import java.util.logging.Level;
import java.util.logging.Logger;

public class UserService {

    private static final Logger LOGGER =
            Logger.getLogger(UserService.class.getName());

    private static final String PASSWORD_CHARS =
            "ABCDEFGHJKLMNPQRSTUVWXYZ" +
            "abcdefghijkmnopqrstuvwxyz" +
            "23456789" +
            "!@#$%";

    private static final int TEMP_PASSWORD_LENGTH = 10;

    private final UserDAO userDAO;
    private final RoleDAO roleDAO;
    private final EmailService emailService;

    public UserService(
            UserDAO userDAO,
            RoleDAO roleDAO,
            EmailService emailService
    ) {
        if (userDAO == null) {
            throw new IllegalArgumentException("UserDAO must not be null.");
        }

        if (roleDAO == null) {
            throw new IllegalArgumentException("RoleDAO must not be null.");
        }

        if (emailService == null) {
            throw new IllegalArgumentException("EmailService must not be null.");
        }

        this.userDAO = userDAO;
        this.roleDAO = roleDAO;
        this.emailService = emailService;
    }

    public CreateUserResult createUser(
            String username,
            String email,
            String fullName,
            String roleName
    ) {

        validateInput(username, email, fullName, roleName);

        String normalizedUsername = username.trim();
        String normalizedEmail = email.trim().toLowerCase();
        String normalizedFullName = fullName.trim();
        String normalizedRole = roleName.trim().toUpperCase();

        try {
            if (userDAO.existsByUsername(normalizedUsername)) {
                throw new IllegalArgumentException(
                        "Username already exists."
                );
            }

            if (userDAO.existsByEmail(normalizedEmail)) {
                throw new IllegalArgumentException(
                        "Email already exists."
                );
            }

            Role role = roleDAO.findByName(normalizedRole);

            if (role == null) {
                throw new IllegalArgumentException(
                        "Invalid role: " + normalizedRole
                );
            }

            String temporaryPassword = generateTemporaryPassword();

            User user = new User();
            user.setUsername(normalizedUsername);
            user.setEmail(normalizedEmail);
            user.setFullName(normalizedFullName);
            user.setPasswordHash(
                    PasswordUtil.hashPassword(temporaryPassword)
            );
            user.setRole(role);
            user.setStatus(UserStatus.ACTIVE);

            long userId = userDAO.create(user);
            user.setUserId(userId);

            boolean emailSent = true;

            try {
                emailService.sendEmail(
                        normalizedEmail,
                        "Internship Management System - Thông tin tài khoản",
                        buildAccountEmail(
                                normalizedUsername,
                                temporaryPassword,
                                normalizedRole
                        )
                );

            } catch (EmailException e) {
                LOGGER.log(
                        Level.WARNING,
                        "Failed to send account creation email to user {0} ({1}): [{2}] {3}",
                        new Object[]{
                                normalizedUsername,
                                normalizedEmail,
                                e.getClass().getSimpleName(),
                                e.getMessage()
                        }
                );
                emailSent = false;
            }

            return new CreateUserResult(
                    user,
                    temporaryPassword,
                    emailSent
            );

        } catch (SQLException e) {
            throw new IllegalStateException(
                    "Unable to create user because of a database error.",
                    e
            );
        }
    }

    private void validateInput(
            String username,
            String email,
            String fullName,
            String roleName
    ) {

        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException(
                    "Username must not be blank."
            );
        }

        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException(
                    "Email must not be blank."
            );
        }

        if (fullName == null || fullName.isBlank()) {
            throw new IllegalArgumentException(
                    "Full name must not be blank."
            );
        }

        if (roleName == null || roleName.isBlank()) {
            throw new IllegalArgumentException(
                    "Role must not be blank."
            );
        }

        if (!email.contains("@")) {
            throw new IllegalArgumentException(
                    "Invalid email format."
            );
        }
    }

    private String generateTemporaryPassword() {

        SecureRandom random = new SecureRandom();
        StringBuilder password = new StringBuilder();

        for (int i = 0; i < TEMP_PASSWORD_LENGTH; i++) {
            int index = random.nextInt(PASSWORD_CHARS.length());
            password.append(PASSWORD_CHARS.charAt(index));
        }

        return password.toString();
    }

    private String buildAccountEmail(
            String username,
            String temporaryPassword,
            String roleName
    ) {

        return """
                Xin chào,

                Tài khoản Internship Management System của bạn đã được tạo.

                Username: %s
                Mật khẩu tạm thời: %s
                Vai trò: %s

                Vui lòng đăng nhập và đổi mật khẩu sau khi sử dụng lần đầu.

                Internship Management System
                """.formatted(
                username,
                temporaryPassword,
                roleName
        );
    }

    public record CreateUserResult(
            User user,
            String temporaryPassword,
            boolean emailSent
    ) {
    }
}