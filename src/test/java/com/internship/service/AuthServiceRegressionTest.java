package com.internship.service;

import com.internship.dao.UserDAO;
import com.internship.model.*;
import org.junit.jupiter.api.Test;
import org.mindrot.jbcrypt.BCrypt;
import java.sql.SQLException;
import static org.junit.jupiter.api.Assertions.*;

/** Automatic, no real accounts or database writes; distinct from the legacy main harness. */
class AuthServiceRegressionTest {
    private static final String PASSWORD="  FixturePassword!2026  ";
    private static final String HASH=BCrypt.hashpw(PASSWORD,BCrypt.gensalt(4));
    private static class FixtureDAO extends UserDAO {
        User result; String queried; int calls; boolean unavailable;
        @Override public User findByUsername(String name)throws SQLException {
            calls++;queried=name;
            if(unavailable)throw new SQLException("fixture outage","08001");
            return result;
        }
    }
    private User account(UserStatus status) {
        User u=new User();u.setUserId(7);u.setStatus(status);u.setPasswordHash(HASH);
        Role role=new Role();role.setRoleName("INTERN");u.setRole(role);return u;
    }
    @Test void blankCredentialsNeverQueryDatabase()throws Exception {
        FixtureDAO d=new FixtureDAO();AuthService s=new AuthService(d);
        for(String name:new String[]{null,"","  "})assertNull(s.authenticate(name,PASSWORD));
        for(String password:new String[]{null,"","  "})assertNull(s.authenticate("fixture",password));
        assertEquals(0,d.calls);
    }
    @Test void usernameIsTrimmedButPasswordWhitespaceIsSignificant()throws Exception {
        FixtureDAO d=new FixtureDAO();d.result=account(UserStatus.ACTIVE);AuthService s=new AuthService(d);
        assertSame(d.result,s.authenticate("  fixture  ",PASSWORD));assertEquals("fixture",d.queried);
        assertNull(s.authenticate("fixture",PASSWORD.trim()));
    }
    @Test void wrongPasswordAndMissingAccountReturnNoUser()throws Exception {
        FixtureDAO d=new FixtureDAO();AuthService s=new AuthService(d);
        assertNull(s.authenticate("missing",PASSWORD));d.result=account(UserStatus.ACTIVE);
        assertNull(s.authenticate("fixture","wrong"));
    }
    @Test void lockedInactiveAndMissingStatusNeverAuthenticate()throws Exception {
        FixtureDAO d=new FixtureDAO();AuthService s=new AuthService(d);
        for(UserStatus status:new UserStatus[]{UserStatus.LOCKED,UserStatus.INACTIVE,null}) {
            d.result=account(status);assertNull(s.authenticate("fixture",PASSWORD));
        }
    }
    @Test void invalidOrMissingStoredHashFailsClosed()throws Exception {
        FixtureDAO d=new FixtureDAO();d.result=account(UserStatus.ACTIVE);AuthService s=new AuthService(d);
        for(String hash:new String[]{null,"","invalid-hash"}) {
            d.result.setPasswordHash(hash);assertNull(s.authenticate("fixture",PASSWORD));
        }
    }
    @Test void databaseOutageIsPropagatedRatherThanReportedAsBadPassword() {
        FixtureDAO d=new FixtureDAO();d.unavailable=true;
        SQLException error=assertThrows(SQLException.class,()->new AuthService(d).authenticate("fixture",PASSWORD));
        assertEquals("08001",error.getSQLState());
    }
    @Test void validLoginPreservesAccountIdentityAndRole()throws Exception {
        FixtureDAO d=new FixtureDAO();d.result=account(UserStatus.ACTIVE);
        User u=new AuthService(d).authenticate("fixture",PASSWORD);
        assertSame(d.result,u);assertEquals(7,u.getUserId());assertEquals("INTERN",u.getRole().getRoleName());
    }
    @Test void nullDaoIsRejected(){assertThrows(IllegalArgumentException.class,()->new AuthService(null));}
}
