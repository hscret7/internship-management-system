package com.internship.service;
import com.internship.dao.EmailSuggestionDAO;
import com.internship.dao.EmailSuggestionDAO.Scope;
import com.internship.model.*;
import org.junit.jupiter.api.Test;
import java.sql.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
class EmailSuggestionServiceTest {
    int queries;String searched;Scope searchedScope;
    EmailSuggestionService service=new EmailSuggestionService(new EmailSuggestionDAO(){
        @Override public List<Suggestion> search(Connection connection,Scope scope,String query){
            queries++;searched=query;searchedScope=scope;return List.of(new Suggestion("abc@example.com","A"));
        }
    },new TestConnections());
    User user(String role){User user=new User();user.setUserId(7);user.setStatus(UserStatus.ACTIVE);Role r=new Role();r.setRoleName(role);user.setRole(r);return user;}
    @Test void substringIsTrimmedWithoutChangingLiteralWildcards()throws Exception{
        service.search(user("HR"),Scope.ENROLLMENT," Ab_% ");assertEquals("Ab_%",searched);assertEquals(Scope.ENROLLMENT,searchedScope);
    }
    @Test void shortQueriesNeverOpenSearch()throws Exception{
        assertTrue(service.search(user("HR"),Scope.ENROLLMENT," ").isEmpty());
        assertTrue(service.search(user("ADMIN"),Scope.ACTOR,"a").isEmpty());assertEquals(0,queries);
    }
    @Test void internMentorAndInactiveCannotEnumerate()throws Exception{
        for(String role:List.of("INTERN","MENTOR"))assertThrows(SecurityException.class,()->service.search(user(role),Scope.ENROLLMENT,"ab"));
        User locked=user("ADMIN");locked.setStatus(UserStatus.LOCKED);
        assertThrows(SecurityException.class,()->service.search(locked,Scope.ACTOR,"ab"));assertEquals(0,queries);
    }
    @Test void auditActorsAreOnlyVisibleToAdmin()throws Exception{
        assertThrows(SecurityException.class,()->service.search(user("HR"),Scope.ACTOR,"ad"));
        assertEquals(1,service.search(user("ADMIN"),Scope.ACTOR,"ad").size());assertEquals(1,queries);
    }
    @Test void oversizedQueryCannotReachDao(){assertThrows(IllegalArgumentException.class,()->service.search(user("HR"),Scope.APPLICATION,"a".repeat(255)));assertEquals(0,queries);}
}
