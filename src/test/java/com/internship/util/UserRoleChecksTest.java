package com.internship.util;

import com.internship.model.Role;
import com.internship.model.User;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UserRoleChecksTest {
    @Test
    void hrAndAdminCanManageHrRoutes() {
        assertTrue(UserRoleChecks.canManageHr(userWithRole("HR")));
        assertTrue(UserRoleChecks.canManageHr(userWithRole("ADMIN")));
        assertFalse(UserRoleChecks.canManageHr(userWithRole("INTERN")));
    }

    @Test
    void onlyInternCanUseInternRoutes() {
        assertTrue(UserRoleChecks.isIntern(userWithRole("INTERN")));
        assertFalse(UserRoleChecks.isIntern(userWithRole("HR")));
        assertFalse(UserRoleChecks.isIntern(userWithRole("ADMIN")));
    }

    @Test
    void missingUserOrRoleIsDenied() {
        assertFalse(UserRoleChecks.canManageHr(null));
        assertFalse(UserRoleChecks.isIntern(null));
        assertFalse(UserRoleChecks.canManageHr(new User()));
        assertFalse(UserRoleChecks.isIntern(new User()));
    }

    private User userWithRole(String roleName) {
        Role role = new Role();
        role.setRoleName(roleName);
        User user = new User();
        user.setRole(role);
        return user;
    }
}
