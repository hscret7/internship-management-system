package com.internship.dao;

import org.junit.jupiter.api.Test;
import java.lang.reflect.Proxy;
import java.sql.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class RoleAssignmentDAOTest {
    static Object proxy(Class<?> type, java.lang.reflect.InvocationHandler h) {
        return Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type}, h);
    }
    static Object fallback(Class<?> type) {
        if (type == boolean.class) return false;
        if (type == int.class) return 0;
        if (type == long.class) return 0L;
        return null;
    }
    static class Fixture extends RoleAssignmentDAO {
        String actorRole = "ADMIN", targetRole = "INTERN";
        boolean commit, rollback, failAudit;
        List<String> writes = new ArrayList<>();
        @Override protected Connection open() {
            return (Connection) proxy(Connection.class, (p,m,a) -> {
                if (m.getName().equals("commit")) commit = true;
                if (m.getName().equals("rollback")) rollback = true;
                if (m.getName().equals("prepareStatement")) {
                    String sql = (String) a[0];
                    return proxy(PreparedStatement.class, (q,n,b) -> {
                        if (n.getName().equals("executeUpdate")) {
                            writes.add(sql);
                            if (failAudit && sql.startsWith("INSERT")) throw new SQLException("audit unavailable");
                            return 1;
                        }
                        if (n.getName().equals("executeQuery")) {
                            int[] index = {-1};
                            return proxy(ResultSet.class, (r,k,c) -> {
                                if (k.getName().equals("next")) return ++index[0] < 2;
                                if (k.getName().equals("getLong")) return index[0] == 0 ? 1L : 2L;
                                if (k.getName().equals("getString"))
                                    return (int)c[0] == 3 ? "ACTIVE" : index[0] == 0 ? actorRole : targetRole;
                                return fallback(k.getReturnType());
                            });
                        }
                        return fallback(n.getReturnType());
                    });
                }
                return fallback(m.getReturnType());
            });
        }
    }
    @Test void commitsRoleAndAuditTogether() throws Exception {
        Fixture f = new Fixture();
        assertTrue(f.change(1,2,"INTERN","HR"));
        assertTrue(f.commit);
        assertEquals(2,f.writes.size());
        assertTrue(f.writes.get(1).contains("ROLE_CHANGE"));
    }
    @Test void auditFailureRollsBackRole() {
        Fixture f = new Fixture(); f.failAudit = true;
        assertThrows(SQLException.class, () -> f.change(1,2,"INTERN","HR"));
        assertTrue(f.rollback); assertFalse(f.commit);
    }
    @Test void staleFormCannotOverwriteNewRole() {
        Fixture f = new Fixture(); f.targetRole = "MENTOR";
        assertThrows(IllegalArgumentException.class, () -> f.change(1,2,"INTERN","HR"));
        assertTrue(f.rollback); assertTrue(f.writes.isEmpty());
    }
    @Test void revokedAdminCannotWrite() {
        Fixture f = new Fixture(); f.actorRole = "HR";
        assertThrows(SecurityException.class, () -> f.change(1,2,"INTERN","HR"));
        assertTrue(f.rollback); assertTrue(f.writes.isEmpty());
    }
    @Test void unchangedRoleDoesNotCreateAudit() throws Exception {
        Fixture f = new Fixture();
        assertFalse(f.change(1,2,"INTERN","INTERN"));
        assertTrue(f.writes.isEmpty()); assertFalse(f.commit);
    }
    @Test void rejectsSelfChangeAndForgedRole() {
        Fixture f = new Fixture();
        assertThrows(IllegalArgumentException.class, () -> f.change(1,1,"ADMIN","HR"));
        assertThrows(IllegalArgumentException.class, () -> f.change(1,2,"INTERN","SUPERUSER"));
        assertThrows(IllegalArgumentException.class, () -> f.change(1,2,"INTERN",null));
    }
}
