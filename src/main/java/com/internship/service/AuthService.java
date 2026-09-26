package com.internship.service;

import com.internship.dao.UserDAO;
import com.internship.model.User;
import com.internship.model.UserStatus;
import com.internship.util.PasswordUtil;

public class AuthService {

    private final UserDAO userDAO;

    public AuthService() {
        this.userDAO = new UserDAO();
    }

    public AuthService(UserDAO userDAO) {
        if (userDAO == null) {
            throw new IllegalArgumentException(
                    "UserDAO must not be null."
            );
        }

        this.userDAO = userDAO;
    }

    public User authenticate(
            String username,
            String password
    ) throws Exception {

        if (username == null
                || username.isBlank()
                || password == null
                || password.isBlank()) {

            return null;
        }

        String normalizedUsername = username.trim();

        User user =
                userDAO.findByUsername(normalizedUsername);

        if (user == null) {
            return null;
        }

        if (user.getStatus() != UserStatus.ACTIVE) {
            return null;
        }

        boolean passwordMatches =
                PasswordUtil.verifyPassword(
                        password,
                        user.getPasswordHash()
                );

        if (!passwordMatches) {
            return null;
        }

        return user;
    }
}