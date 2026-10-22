package com.internship.dao;

import com.internship.util.DBConnection;
import java.sql.*;
import java.util.Set;

/** Changes an account role and its audit event atomically. */
public class RoleAssignmentDAO {
    protected Connection open() throws SQLException { return DBConnection.getConnection(); }

    public boolean change(long actor, long target, String expectedRole, String role) throws SQLException {
        Set<String> allowed = Set.of("ADMIN", "HR", "MENTOR", "INTERN");
        if (actor <= 0 || target <= 0 || role == null || expectedRole == null
                || !allowed.contains(role) || !allowed.contains(expectedRole))
            throw new IllegalArgumentException("Vai trò hoặc tài khoản không hợp lệ.");
        if (actor == target)
            throw new IllegalArgumentException("Không được đổi quyền tài khoản đang đăng nhập.");
        try (Connection c = open()) {
            c.setAutoCommit(false);
            try {
                // Lock actor and target in ID order to serialize concurrent role changes.
                String actorRole = null, actorStatus = null, targetRole = null;
                try (PreparedStatement s = c.prepareStatement("SELECT u.user_id,r.role_name,u.status FROM users u JOIN roles r ON r.role_id=u.role_id WHERE u.user_id IN (?,?) ORDER BY u.user_id FOR UPDATE")) {
                    s.setLong(1, actor); s.setLong(2, target);
                    try (ResultSet r = s.executeQuery()) {
                        while (r.next()) {
                            if (r.getLong(1) == actor) { actorRole = r.getString(2); actorStatus = r.getString(3); }
                            if (r.getLong(1) == target) targetRole = r.getString(2);
                        }
                    }
                }
                if (!"ADMIN".equals(actorRole) || !"ACTIVE".equals(actorStatus))
                    throw new SecurityException("Admin access required");
                if (targetRole == null) throw new IllegalArgumentException("Tài khoản không tồn tại.");
                if (!expectedRole.equals(targetRole))
                    throw new IllegalArgumentException("Vai trò đã thay đổi. Hãy tải lại trang.");
                if (role.equals(targetRole)) { c.rollback(); return false; }
                try (PreparedStatement s = c.prepareStatement("UPDATE users SET role_id=(SELECT role_id FROM roles WHERE role_name=?) WHERE user_id=?")) {
                    s.setString(1, role); s.setLong(2, target);
                    if (s.executeUpdate() != 1) throw new SQLException("Role update failed");
                }
                try (PreparedStatement s = c.prepareStatement("INSERT INTO audit_logs(actor_user_id,action,object_type,object_id,result) VALUES(?,'ROLE_CHANGE','USER',?,'SUCCESS')")) {
                    s.setLong(1, actor); s.setLong(2, target);
                    if (s.executeUpdate() != 1) throw new SQLException("Audit insert failed");
                }
                c.commit(); return true;
            } catch (SQLException | RuntimeException e) {
                try { c.rollback(); } catch (SQLException rollback) { e.addSuppressed(rollback); }
                throw e;
            }
        }
    }
}
