package com.internship.service;

import java.security.SecureRandom;
import java.util.Locale;
import jakarta.mail.internet.AddressException;
import jakarta.mail.internet.InternetAddress;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.internship.dao.RoleDAO;
import com.internship.dao.UserDAO;
import com.internship.model.Role;
import com.internship.model.RegistrationProfile;
import com.internship.model.User;
import com.internship.model.UserStatus;
import com.internship.service.email.EmailException;
import com.internship.service.email.EmailService;
import com.internship.util.PasswordUtil;

public class UserService {

    private static final Logger LOGGER
            = Logger.getLogger(UserService.class.getName());

    private static final String PASSWORD_CHARS
            = "ABCDEFGHJKLMNPQRSTUVWXYZ"
            + "abcdefghijkmnopqrstuvwxyz"
            + "23456789"
            + "!@#$%";

    private static final int TEMP_PASSWORD_LENGTH = 10;

    private final UserDAO userDAO;
    private final RoleDAO roleDAO;
    private final EmailService emailService;

    public UserService(
            UserDAO userDAO,
            RoleDAO roleDAO
    ) {
        this(userDAO, roleDAO, (recipient, subject, content) -> {
            throw new EmailException(
                    "Email service is not configured for this operation."
            );
        });
    }

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
        String normalizedEmail = email.trim().toLowerCase(Locale.ROOT);
        String normalizedFullName = fullName.trim();
        String normalizedRole = roleName.trim().toUpperCase(Locale.ROOT);

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

    /** Changes only the authenticated actor's password; never trust a submitted user ID. */
    public void changePassword(long userId, String currentPassword, String newPassword,
            String confirmPassword) throws SQLException {
        if (userId <= 0 || currentPassword == null || currentPassword.isBlank()) {
            throw new IllegalArgumentException("Vui lòng nhập mật khẩu hiện tại.");
        }
        if (newPassword == null || newPassword.isBlank() || newPassword.length() < 8) {
            throw new IllegalArgumentException("Mật khẩu mới phải có ít nhất 8 ký tự.");
        }
        if (newPassword.getBytes(java.nio.charset.StandardCharsets.UTF_8).length > 72) {
            throw new IllegalArgumentException("Mật khẩu mới tối đa 72 byte UTF-8.");
        }
        if (!newPassword.equals(confirmPassword)) {
            throw new IllegalArgumentException("Mật khẩu xác nhận không khớp.");
        }
        User user = userDAO.findById(userId);
        if (user == null || user.getStatus() != UserStatus.ACTIVE
                || !PasswordUtil.verifyPassword(currentPassword, user.getPasswordHash())) {
            throw new IllegalArgumentException("Mật khẩu hiện tại không đúng hoặc tài khoản không khả dụng.");
        }
        if (PasswordUtil.verifyPassword(newPassword, user.getPasswordHash())) {
            throw new IllegalArgumentException("Mật khẩu mới phải khác mật khẩu hiện tại.");
        }
        String newHash = PasswordUtil.hashPassword(newPassword);
        if (!userDAO.changePasswordIfCurrent(userId, user.getPasswordHash(), newHash)) {
            throw new IllegalArgumentException("Thông tin tài khoản đã thay đổi. Vui lòng đăng nhập lại và thử lại.");
        }
    }

    public User registerIntern(
            String username,
            String email,
            String fullName,
            String password,
            String confirmPassword
    ) {
        return registerInternInternal(username, email, fullName, password, confirmPassword, null);
    }

    public User registerIntern(String username, String email, String fullName,
            String password, String confirmPassword, String phone, String university,
            String major, String gpa) {
        RegistrationProfile profile = RegistrationProfile.parse(phone, university, major, gpa);
        if (fullName != null && fullName.trim().length() > 100
                || email != null && email.trim().length() > 100) {
            throw new IllegalArgumentException("Họ tên và email tối đa 100 ký tự cho hồ sơ thực tập.");
        }
        return registerInternInternal(username, email, fullName, password, confirmPassword, profile);
    }

    private User registerInternInternal(String username, String email, String fullName,
            String password, String confirmPassword, RegistrationProfile profile) {
        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException(
                    "Tên đăng nhập không được để trống."
            );
        }

        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException(
                    "Email không được để trống."
            );
        }

        if (!isValidEmail(email)) {
            throw new IllegalArgumentException(
                    "Email không hợp lệ."
            );
        }

        if (fullName == null || fullName.isBlank()) {
            throw new IllegalArgumentException(
                    "Họ tên không được để trống."
            );
        }

        validateFieldLengths(username, email, fullName);

        if (password == null || password.length() < 8) {
            throw new IllegalArgumentException(
                    "Mật khẩu phải có ít nhất 8 ký tự."
            );
        }

        if (!password.equals(confirmPassword)) {
            throw new IllegalArgumentException(
                    "Mật khẩu xác nhận không khớp."
            );
        }

        String normalizedUsername = username.trim();
        String normalizedEmail = email.trim().toLowerCase(Locale.ROOT);
        String normalizedFullName = fullName.trim();

        try {
            if (userDAO.existsByUsername(normalizedUsername)) {
                throw new IllegalArgumentException(
                        "Tên đăng nhập đã tồn tại."
                );
            }

            if (userDAO.existsByEmail(normalizedEmail)) {
                throw new IllegalArgumentException(
                        "Email đã tồn tại."
                );
            }

            // Public registration luôn do server gán INTERN.
            Role internRole = roleDAO.findByName("INTERN");

            if (internRole == null) {
                throw new IllegalStateException(
                        "INTERN role is not configured."
                );
            }

            User user = new User();
            user.setUsername(normalizedUsername);
            user.setEmail(normalizedEmail);
            user.setFullName(normalizedFullName);
            user.setPasswordHash(
                    PasswordUtil.hashPassword(password)
            );
            user.setRole(internRole);
            user.setStatus(UserStatus.ACTIVE);

            long userId = profile == null ? userDAO.create(user) : userDAO.createWithProfile(user, profile);
            user.setUserId(userId);

            return user;

        } catch (SQLException e) {
            throw new IllegalStateException(
                    "Unable to register intern because of a database error.",
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

        validateFieldLengths(username, email, fullName);

        if (!isValidEmail(email)) {
            throw new IllegalArgumentException(
                    "Invalid email format."
            );
        }
    }

    private static void validateFieldLengths(String username, String email, String fullName) {
        if (username.trim().length() > 100 || email.trim().length() > 255
                || fullName.trim().length() > 150) {
            throw new IllegalArgumentException("Thông tin tài khoản vượt quá độ dài cho phép.");
        }
    }

    private static boolean isValidEmail(String email) {
        String value = email.trim();
        if (value.chars().anyMatch(Character::isWhitespace)
                || value.contains("\r") || value.contains("\n")) {
            return false;
        }
        try {
            InternetAddress address = new InternetAddress(value, true);
            address.validate();
            return !address.isGroup() && address.getPersonal() == null
                    && value.equals(address.getAddress());
        } catch (AddressException e) {
            return false;
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
