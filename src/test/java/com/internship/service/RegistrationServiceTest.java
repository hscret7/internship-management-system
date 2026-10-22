package com.internship.service;

import com.internship.dao.*;
import com.internship.model.*;
import org.junit.jupiter.api.Test;
import java.sql.SQLException;
import static org.junit.jupiter.api.Assertions.*;

class RegistrationServiceTest {
    private static class Store extends UserDAO {
        User user;RegistrationProfile profile;int calls;boolean failure;
        @Override public boolean existsByUsername(String name) {return false;}
        @Override public boolean existsByEmail(String email) {return false;}
        @Override public long create(User user) {throw new AssertionError("Public profile must not use account-only insert");}
        @Override public long createWithProfile(User user,RegistrationProfile profile) throws SQLException {
            calls++;if(failure)throw new SQLException("fixture");
            this.user=user;this.profile=profile;return 99;
        }
    }
    private UserService service(Store store) {
        return new UserService(store,new RoleDAO() {
            @Override public Role findByName(String name) {
                assertEquals("INTERN",name);Role role=new Role();role.setRoleName(name);role.setRoleId(4);return role;
            }
        });
    }
    @Test void normalizedAccountAndAllProfileFieldsReachAtomicStore() {
        Store s=new Store();User u=service(s).registerIntern(" hue "," HUE@example.com "," Minh Huệ ","Pass12345","Pass12345","0912345678"," ICTU "," ATTT ","3.75");
        assertEquals(99,u.getUserId());assertEquals("hue",u.getUsername());assertEquals("hue@example.com",u.getEmail());
        assertEquals("Minh Huệ",u.getFullName());assertEquals("INTERN",u.getRole().getRoleName());
        assertEquals("ICTU",s.profile.university());assertEquals("ATTT",s.profile.major());
        assertEquals("0912345678",s.profile.phone());assertEquals("3.75",s.profile.gpa().toPlainString());
        assertTrue(com.internship.util.PasswordUtil.verifyPassword("Pass12345",u.getPasswordHash()));
    }
    @Test void omittedFieldsStillCreateLinkedEmptyProfile() {
        Store s=new Store();service(s).registerIntern("hue","hue@example.com","Minh Huệ","Pass12345","Pass12345",null,null,null,null);
        assertNotNull(s.profile);assertEquals("",s.profile.major());assertNull(s.profile.gpa());assertEquals(1,s.calls);
    }
    @Test void badGpaDoesNotReachStore() {
        Store s=new Store();assertThrows(IllegalArgumentException.class,()->service(s).registerIntern("hue","hue@example.com","Huệ","Pass12345","Pass12345",null,null,null,"3.999"));
        assertEquals(0,s.calls);
    }
    @Test void profileColumnLengthsRejectBeforePersistence() {
        Store s=new Store();assertThrows(IllegalArgumentException.class,()->service(s).registerIntern("hue","hue@example.com","x".repeat(101),"Pass12345","Pass12345",null,null,null,null));
        assertEquals(0,s.calls);
    }
    @Test void passwordMismatchCannotInsertAccountOrProfile() {
        Store s=new Store();assertThrows(IllegalArgumentException.class,()->service(s).registerIntern("hue","hue@example.com","Huệ","Pass12345","Different",null,null,null,null));
        assertEquals(0,s.calls);
    }
    @Test void databaseFailureIsNotReportedAsSuccessfulRegistration() {
        Store s=new Store();s.failure=true;
        assertThrows(IllegalStateException.class,()->service(s).registerIntern("hue","hue@example.com","Huệ","Pass12345","Pass12345",null,null,null,null));
        assertEquals(1,s.calls);
    }
}
