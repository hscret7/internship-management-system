package com.internship.util;

import org.mindrot.jbcrypt.BCrypt;

public final class PasswordUtil {

    private static final int LOG_ROUNDS = 12;

    private PasswordUtil() {
        // Utility class - không cho khởi tạo object
    }

    public static String hashPassword(String plainPassword) {
        validatePassword(plainPassword);

        return BCrypt.hashpw(
                plainPassword,
                BCrypt.gensalt(LOG_ROUNDS)
        );
    }

    public static boolean verifyPassword(
            String plainPassword,
            String passwordHash
    ) {
        if (plainPassword == null
                || plainPassword.isBlank()
                || passwordHash == null
                || passwordHash.isBlank()) {

            return false;
        }

        try {
            return BCrypt.checkpw(plainPassword, passwordHash);
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    private static void validatePassword(String password) {
        if (password == null || password.isBlank()) {
            throw new IllegalArgumentException(
                    "Password must not be null or blank."
            );
        }
    }
}
