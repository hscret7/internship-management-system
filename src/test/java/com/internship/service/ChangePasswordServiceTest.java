package com.internship.service;

import com.internship.dao.RoleDAO;
import com.internship.dao.UserDAO;
import com.internship.model.User;
import com.internship.model.UserStatus;
import com.internship.util.PasswordUtil;
import java.sql.SQLException;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ChangePasswordServiceTest {
    private static final String HASH = PasswordUtil.hashPassword("OldPassword1!");
    static class Users extends UserDAO {
        User user; int writes; boolean succeeds = true; String replacement;
        Users() { user = new User(); user.setUserId(42); user.setStatus(UserStatus.ACTIVE); user.setPasswordHash(HASH); }
        @Override public User findById(long id) throws SQLException { assertEquals(42, id); return user; }
        @Override public boolean changePasswordIfCurrent(long id, String expected, String hash) {
            assertEquals(42,id); assertEquals(HASH,expected); writes++; replacement=hash; return succeeds;
        }
    }
    private UserService service(Users users) { return new UserService(users, new RoleDAO()); }
    @Test void persistsHashOnlyAfterVerifyingCurrentPassword() throws Exception {
        Users users = new Users();
        service(users).changePassword(42,"OldPassword1!","NewPassword2!","NewPassword2!");
        assertEquals(1,users.writes); assertNotEquals("NewPassword2!",users.replacement);
        assertTrue(PasswordUtil.verifyPassword("NewPassword2!",users.replacement));
        assertFalse(PasswordUtil.verifyPassword("OldPassword1!",users.replacement));
    }
    @Test void wrongOldPasswordCannotWrite() {
        Users users=new Users(); assertThrows(IllegalArgumentException.class,
            ()->service(users).changePassword(42,"wrong","NewPassword2!","NewPassword2!")); assertEquals(0,users.writes);
    }
    @Test void enforcesMinimumAndUtf8ByteMaximumBeforeWrite() {
        for(String password : new String[]{"1234567","é".repeat(37)," ".repeat(8)}) {
            Users users=new Users(); assertThrows(IllegalArgumentException.class,
                ()->service(users).changePassword(42,"OldPassword1!",password,password)); assertEquals(0,users.writes);
        }
    }
    @Test void acceptsExactly72Utf8BytesWithoutTrimming() throws Exception {
        Users users=new Users(); String password=" é"+"x".repeat(69);
        assertEquals(72,password.getBytes(java.nio.charset.StandardCharsets.UTF_8).length);
        service(users).changePassword(42,"OldPassword1!",password,password);
        assertTrue(PasswordUtil.verifyPassword(password,users.replacement));
        assertFalse(PasswordUtil.verifyPassword(password.trim(),users.replacement));
    }
    @Test void rejectsConfirmationMismatchAndSamePassword() {
        Users users=new Users();
        assertThrows(IllegalArgumentException.class,()->service(users).changePassword(42,"OldPassword1!","NewPassword2!","different"));
        assertThrows(IllegalArgumentException.class,()->service(users).changePassword(42,"OldPassword1!","OldPassword1!","OldPassword1!"));
        assertEquals(0,users.writes);
    }
    @Test void inactiveOrMissingAccountCannotWrite() {
        Users users=new Users(); users.user.setStatus(UserStatus.INACTIVE);
        assertThrows(IllegalArgumentException.class,()->service(users).changePassword(42,"OldPassword1!","NewPassword2!","NewPassword2!"));
        users.user=null;
        assertThrows(IllegalArgumentException.class,()->service(users).changePassword(42,"OldPassword1!","NewPassword2!","NewPassword2!"));
        assertEquals(0,users.writes);
    }
    @Test void concurrentPasswordChangeIsNotReportedAsSuccess() {
        Users users=new Users();users.succeeds=false;
        assertThrows(IllegalArgumentException.class,()->service(users).changePassword(42,"OldPassword1!","NewPassword2!","NewPassword2!"));
    }
    @Test void databaseFailurePropagatesWithoutConvertingToSuccess() {
        Users users=new Users(){@Override public User findById(long id) throws SQLException { throw new SQLException("unavailable"); }};
        assertThrows(SQLException.class,()->service(users).changePassword(42,"OldPassword1!","NewPassword2!","NewPassword2!"));
    }
}
