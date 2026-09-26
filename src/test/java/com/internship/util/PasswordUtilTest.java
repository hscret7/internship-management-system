package com.internship.util;

public class PasswordUtilTest {

    public static void main(String[] args) {

        String password = "Test@123";

        System.out.println("=== PASSWORD UTIL TEST ===");

        String hash1 = PasswordUtil.hashPassword(password);
        String hash2 = PasswordUtil.hashPassword(password);

        System.out.println(
                "Hash generated: "
                        + (hash1 != null && !hash1.isBlank())
        );

        System.out.println(
                "Different salts: "
                        + !hash1.equals(hash2)
        );

        boolean correctPassword =
                PasswordUtil.verifyPassword(password, hash1);

        boolean wrongPassword =
                PasswordUtil.verifyPassword("WrongPassword", hash1);

        System.out.println(
                "Correct password: " + correctPassword
        );

        System.out.println(
                "Wrong password rejected: " + !wrongPassword
        );

        if (!correctPassword) {
            throw new IllegalStateException(
                    "Correct password verification failed."
            );
        }

        if (wrongPassword) {
            throw new IllegalStateException(
                    "Wrong password was accepted."
            );
        }

        if (hash1.equals(hash2)) {
            throw new IllegalStateException(
                    "BCrypt hashes should use different salts."
            );
        }

        System.out.println(
                "=== PASSWORD UTIL TEST SUCCESS ==="
        );
    }
}
