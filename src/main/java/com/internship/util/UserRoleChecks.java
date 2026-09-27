package com.internship.util;

import com.internship.model.Role;
import com.internship.model.User;

/** Role checks for BE3 routes using BE1's User/Role contract. */
public final class UserRoleChecks {
    private UserRoleChecks() {
    }

    public static boolean canManageHr(User user) {
        return hasRole(user, "HR") || hasRole(user, "ADMIN");
    }

    public static boolean isIntern(User user) {
        return hasRole(user, "INTERN");
    }

    private static boolean hasRole(User user, String expected) {
        Role role = user == null ? null : user.getRole();
        return role != null && expected.equalsIgnoreCase(role.getRoleName());
    }
}
