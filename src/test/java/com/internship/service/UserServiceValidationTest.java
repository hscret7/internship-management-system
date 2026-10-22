package com.internship.service;

import com.internship.dao.RoleDAO;
import com.internship.dao.UserDAO;
import com.internship.model.Role;
import com.internship.model.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import java.util.Locale;
import static org.junit.jupiter.api.Assertions.*;

class UserServiceValidationTest {
    private final UserDAO users = new UserDAO() {
        @Override public boolean existsByUsername(String value) { return false; }
        @Override public boolean existsByEmail(String value) { return false; }
        @Override public long create(User user) { return 1; }
    };
    private final RoleDAO roles = new RoleDAO() {
        @Override public Role findByName(String name) {
            Role role = new Role(); role.setRoleName(name); return role;
        }
    };
    private final UserService service = new UserService(users, roles, (to, subject, body) -> {});

    @ParameterizedTest
    @ValueSource(strings = {"@", "a@", "@example.com", "a@@example.com", "a b@example.com", "a@example.com,b@example.com", "Name <a@example.com>", "a\r\n@example.com"})
    void rejectsMalformedEmailInBothAccountFlows(String email) {
        assertThrows(IllegalArgumentException.class, () -> service.createUser("intern", email, "Name", "INTERN"));
        assertThrows(IllegalArgumentException.class, () -> service.registerIntern("intern", email, "Name", "Test@123", "Test@123"));
    }

    @Test void acceptsAndNormalizesEmailAndAssignsInternOnServer() {
        User user = service.registerIntern(" intern ", " User+tag@Example.COM ", " Name ", "Test@123", "Test@123");
        assertEquals("user+tag@example.com", user.getEmail());
        assertEquals("intern", user.getUsername());
        assertEquals("INTERN", user.getRole().getRoleName());
    }

    @Test void rejectsFieldsBeyondDatabaseLimitsInBothFlows() {
        assertThrows(IllegalArgumentException.class, () -> service.createUser("x".repeat(101), "a@example.com", "Name", "HR"));
        assertThrows(IllegalArgumentException.class, () -> service.registerIntern("intern", "a@example.com", "x".repeat(151), "Test@123", "Test@123"));
    }

    @Test void normalizationDoesNotDependOnMachineLocale() {
        Locale previous = Locale.getDefault();
        try {
            Locale.setDefault(Locale.forLanguageTag("tr-TR"));
            User user = service.createUser("intern", "INTERN@example.com", "Name", "intern").user();
            assertEquals("intern@example.com", user.getEmail());
            assertEquals("INTERN", user.getRole().getRoleName());
        } finally { Locale.setDefault(previous); }
    }
}
