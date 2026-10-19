package com.internship.service;

import com.internship.dao.ScheduleEnrollmentDAO;
import com.internship.dao.ScheduleEnrollmentDAO.Choice;
import com.internship.model.*;
import org.junit.jupiter.api.Test;
import java.sql.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class EnrollmentLookupServiceTest {
    private final TestConnections connections = new TestConnections();
    private Long owner;
    private String email;
    private int queries;
    private boolean fail;
    private final List<Choice> choices = List.of(
            new Choice(1, 10, "A", "ACTIVE", "ACTIVE", "Intern", "intern@example.com"),
            new Choice(2, 20, "B", "ACTIVE", "ACTIVE", "Intern", "intern@example.com"));
    private final EnrollmentLookupService service = new EnrollmentLookupService(new ScheduleEnrollmentDAO() {
        @Override public List<Choice> choices(Long userId, String address, Connection c) throws SQLException {
            queries++; owner=userId; email=address;
            if(fail) throw new SQLException("fixture database error");
            return choices;
        }
    }, connections);
    private static User user(String role) {
        User u = new User();u.setUserId(42L);u.setStatus(UserStatus.ACTIVE);
        Role r = new Role();r.setRoleName(role);u.setRole(r);return u;
    }
    @Test void internIsAlwaysScopedToSessionOwner() throws Exception {
        assertEquals(choices,service.lookup(user("INTERN"),null));
        assertEquals(42L,owner);assertNull(email);
    }
    @Test void internCannotSearchSomeoneElsesEmail() {
        assertEquals(403,assertThrows(ScheduleLeaveException.class,
                ()->service.lookup(user("INTERN"),"other@example.com")).status());
        assertEquals(0,queries);
    }
    @Test void hrGetsAllMatchingEnrollmentsWithoutChoosingFirst() throws Exception {
        assertEquals(2,service.lookup(user("HR")," intern@example.com ").size());
        assertNull(owner);assertEquals("intern@example.com",email);
    }
    @Test void adminMaySearchExactEmail() throws Exception {
        service.lookup(user("ADMIN"),"intern@example.com");assertEquals(1,queries);
    }
    @Test void hrCannotEnumerateAllInternsWithoutAnEmail() {
        for(String address:new String[]{null,"","invalid","a b@example.com"})
            assertEquals(400,assertThrows(ScheduleLeaveException.class,
                    ()->service.lookup(user("HR"),address)).status());
        assertEquals(0,queries);
    }
    @Test void mentorAndInactiveUsersCannotLookup() {
        assertThrows(ScheduleLeaveException.class,()->service.lookup(user("MENTOR"),"a@b.com"));
        User inactive=user("HR");inactive.setStatus(UserStatus.LOCKED);
        assertThrows(ScheduleLeaveException.class,()->service.lookup(inactive,"a@b.com"));
        assertThrows(ScheduleLeaveException.class,()->service.lookup(null,"a@b.com"));
        assertEquals(0,queries);
    }
    @Test void sqlFailureIsPropagatedNotConvertedToDummyEnrollment() {
        fail=true;assertThrows(SQLException.class,()->service.lookup(user("HR"),"intern@example.com"));
    }

    @Test void emailResolutionRequiresExplicitProgramWhenMultipleActiveMatchesExist()throws Exception{
        assertEquals(400,assertThrows(ScheduleLeaveException.class,
                ()->service.activeByEmail(user("HR"),"intern@example.com",null)).status());
        assertEquals(2,service.activeByEmail(user("HR"),"intern@example.com",20));
        assertThrows(ScheduleLeaveException.class,()->service.activeByEmail(user("HR"),"intern@example.com",999));
    }
    @Test void internAndMentorCannotReadDirectoryWithoutOpeningConnection(){
        assertThrows(ScheduleLeaveException.class,()->service.internDirectory(user("INTERN")));
        assertThrows(ScheduleLeaveException.class,()->service.internDirectory(user("MENTOR")));
        assertThrows(ScheduleLeaveException.class,()->service.internDirectory(null));
        assertEquals(0,queries);
    }

}
