package com.internship.service;

import com.internship.dao.UserDAO;
import com.internship.model.User;
import com.internship.model.UserStatus;

public class AuthServiceTest {

    private static final String PASSWORD_ENV =
            "INTERNSHIP_DEV_PASSWORD";

    private static final String TEST_USERNAME =
            "admin_test";

    public static void main(String[] args) throws Exception {

        String password = System.getenv(PASSWORD_ENV);

        if (password == null || password.isBlank()) {
            throw new IllegalStateException(
                    "Missing environment variable: "
                            + PASSWORD_ENV
            );
        }

        AuthService authService = new AuthService();
        UserDAO userDAO = new UserDAO();

        User testUser =
                userDAO.findByUsername(TEST_USERNAME);

        if (testUser == null) {
            throw new IllegalStateException(
                    "Development account not found: "
                            + TEST_USERNAME
            );
        }

        long userId = testUser.getUserId();

        try {
            // Luôn bắt đầu test từ ACTIVE.
            userDAO.updateStatus(
                    userId,
                    UserStatus.ACTIVE
            );

            System.out.println(
                    "=== AUTH SERVICE TEST ==="
            );

            User validUser =
                    authService.authenticate(
                            TEST_USERNAME,
                            password
                    );

            assertCondition(
                    validUser != null,
                    "Valid login"
            );

            assertCondition(
                    validUser != null
                            && "ADMIN".equals(
                            validUser.getRole().getRoleName()
                    ),
                    "ADMIN role loaded"
            );

            User wrongPassword =
                    authService.authenticate(
                            TEST_USERNAME,
                            "DefinitelyWrongPassword"
                    );

            assertCondition(
                    wrongPassword == null,
                    "Wrong password rejected"
            );

            User unknownUser =
                    authService.authenticate(
                            "user_does_not_exist",
                            password
                    );

            assertCondition(
                    unknownUser == null,
                    "Unknown user rejected"
            );

            User blankUsername =
                    authService.authenticate(
                            "   ",
                            password
                    );

            assertCondition(
                    blankUsername == null,
                    "Blank username rejected"
            );

            User blankPassword =
                    authService.authenticate(
                            TEST_USERNAME,
                            "   "
                    );

            assertCondition(
                    blankPassword == null,
                    "Blank password rejected"
            );

            userDAO.updateStatus(
                    userId,
                    UserStatus.INACTIVE
            );

            User inactiveUser =
                    authService.authenticate(
                            TEST_USERNAME,
                            password
                    );

            assertCondition(
                    inactiveUser == null,
                    "INACTIVE user rejected"
            );

            userDAO.updateStatus(
                    userId,
                    UserStatus.LOCKED
            );

            User lockedUser =
                    authService.authenticate(
                            TEST_USERNAME,
                            password
                    );

            assertCondition(
                    lockedUser == null,
                    "LOCKED user rejected"
            );

            System.out.println(
                    "=== AUTH SERVICE TEST SUCCESS ==="
            );

        } finally {

            // Không để admin_test bị INACTIVE/LOCKED
            // nếu test lỗi giữa chừng.
            userDAO.updateStatus(
                    userId,
                    UserStatus.ACTIVE
            );

            System.out.println(
                    "Test account restored to ACTIVE."
            );
        }
    }

    private static void assertCondition(
            boolean condition,
            String testName
    ) {

        if (!condition) {
            throw new IllegalStateException(
                    "FAILED: " + testName
            );
        }

        System.out.println(
                "PASS: " + testName
        );
    }
}