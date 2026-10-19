package com.internship.dao;
import com.internship.dao.EmailSuggestionDAO.Scope;
import org.junit.jupiter.api.Test;
import java.lang.reflect.Proxy;
import java.sql.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
class EmailSuggestionDAOTest {
    String sql;Map<Integer,Object> bound=new HashMap<>();
    interface Call{Object run(String method,Object[] args);}
    @SuppressWarnings("unchecked")static<T>T p(Class<T> type,Call call){return(T)Proxy.newProxyInstance(type.getClassLoader(),new Class[]{type},(o,m,a)->call.run(m.getName(),a));}
    Connection connection(){
        ResultSet rows=p(ResultSet.class,(n,a)->n.equals("next")?false:null);
        PreparedStatement statement=p(PreparedStatement.class,(n,a)->switch(n){case "setString"->{bound.put((int)a[0],a[1]);yield null;}case "executeQuery"->rows;default->null;});
        return p(Connection.class,(n,a)->{if(n.equals("prepareStatement")){sql=(String)a[0];return statement;}return null;});
    }
    @Test void substringIsBoundAndResultsAreLimited()throws Exception{
        new EmailSuggestionDAO().search(connection(),Scope.ENROLLMENT,"a_%");
        assertEquals(Map.of(1,"a_%"),bound);assertFalse(sql.contains("a_%"));assertTrue(sql.contains("LOCATE(LOWER(?)"));assertTrue(sql.endsWith("LIMIT 20"));
    }
    @Test void applicationScopeRequiresApprovedUnusedApplicationAndNoActiveEnrollment()throws Exception{
        new EmailSuggestionDAO().search(connection(),Scope.APPLICATION,"qa");
        assertTrue(sql.contains("a.status='APPROVED'"));assertTrue(sql.contains("used.application_id=a.application_id"));assertTrue(sql.contains("active_enrollment.status='ACTIVE'"));
    }
    @Test void actorScopeUsesUserEmailWithoutInternProfileRestriction()throws Exception{
        new EmailSuggestionDAO().search(connection(),Scope.ACTOR,"admin");assertTrue(sql.contains("FROM users u"));assertFalse(sql.contains("program_enrollments"));
    }
}
