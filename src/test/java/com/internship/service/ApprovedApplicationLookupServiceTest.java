package com.internship.service;

import com.internship.dao.ApprovedApplicationLookupDAO;
import com.internship.dao.ApprovedApplicationLookupDAO.Choice;
import com.internship.model.*;
import org.junit.jupiter.api.Test;
import java.sql.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class ApprovedApplicationLookupServiceTest {
    String email;Integer id;int queries;boolean fail;
    final ApprovedApplicationLookupService service=new ApprovedApplicationLookupService(new ApprovedApplicationLookupDAO(){
        @Override public List<Choice> find(String address,Integer key,Connection c)throws SQLException {
            queries++;email=address;id=key;if(fail)throw new SQLException("private detail");
            return List.of(new Choice(1,"A","a@example.com"),new Choice(2,"A","a@example.com"));
        }
    },new TestConnections());
    User actor(String role){User u=new User();u.setUserId(7);u.setStatus(UserStatus.ACTIVE);Role r=new Role();r.setRoleName(role);u.setRole(r);return u;}
    @Test void exactEmailLookupReturnsAllEligibleApplications()throws Exception {
        assertEquals(2,service.find(actor("HR")," a@example.com ",null).size());
        assertEquals("a@example.com",email);assertNull(id);
    }
    @Test void approvedApplicationLinkCanPrefillById()throws Exception {
        service.find(actor("ADMIN"),null,12);assertEquals(12,id);assertNull(email);
    }
    @Test void rejectedActorsNeverReachDatabase(){
        for(String role:List.of("INTERN","MENTOR"))assertThrows(ScheduleLeaveException.class,()->service.find(actor(role),"a@example.com",null));
        User locked=actor("HR");locked.setStatus(UserStatus.LOCKED);
        assertThrows(ScheduleLeaveException.class,()->service.find(locked,"a@example.com",null));assertEquals(0,queries);
    }
    @Test void missingOrInvalidEmailDoesNotEnumerateApplications(){
        for(String address:Arrays.asList(null,"","bad","a b@example.com"))assertThrows(IllegalArgumentException.class,()->service.find(actor("HR"),address,null));
        assertEquals(0,queries);
    }
    @Test void contradictoryAndInvalidKeysAreRejected(){
        assertThrows(IllegalArgumentException.class,()->service.find(actor("HR"),"a@example.com",1));
        assertThrows(IllegalArgumentException.class,()->service.find(actor("HR"),null,0));assertEquals(0,queries);
    }
    @Test void databaseFailurePropagates(){fail=true;assertThrows(SQLException.class,()->service.find(actor("HR"),"a@example.com",null));}
}
