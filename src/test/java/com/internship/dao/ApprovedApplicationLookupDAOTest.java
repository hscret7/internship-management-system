package com.internship.dao;

import org.junit.jupiter.api.Test;
import java.lang.reflect.*;
import java.sql.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
class ApprovedApplicationLookupDAOTest {
    String sql;final Map<Integer,Object> bound=new HashMap<>();
    static <T>T proxy(Class<T> type,InvocationHandler handler){return type.cast(Proxy.newProxyInstance(type.getClassLoader(),new Class<?>[]{type},handler));}
    Connection connection(){
        ResultSet rows=proxy(ResultSet.class,(p,m,a)->m.getName().equals("next")?false:null);
        PreparedStatement s=proxy(PreparedStatement.class,(p,m,a)->switch(m.getName()){
            case "setString","setInt"->{bound.put((Integer)a[0],a[1]);yield null;}case "executeQuery"->rows;default->null;});
        return proxy(Connection.class,(p,m,a)->{if(m.getName().equals("prepareStatement")){sql=(String)a[0];return s;}return null;});
    }
    @Test void queryIncludesApprovalAndBothEnrollmentExclusions()throws Exception{
        new ApprovedApplicationLookupDAO().find("a@example.com",null,connection());
        assertTrue(sql.contains("a.status='APPROVED'"));assertTrue(sql.contains("used.application_id=a.application_id"));
        assertTrue(sql.contains("active_enrollment.status='ACTIVE'"));assertFalse(sql.contains("LIMIT 1"));
        assertEquals(Map.of(1,"a@example.com",2,"a@example.com"),bound);
    }
    @Test void idIsBoundAndStillChecksEligibility()throws Exception{new ApprovedApplicationLookupDAO().find(null,8,connection());assertTrue(sql.contains("a.application_id=?"));assertEquals(Map.of(1,8),bound);}
    @Test void missingOrDuplicateKeysAreRejected(){
        assertThrows(IllegalArgumentException.class,()->new ApprovedApplicationLookupDAO().find(null,null,null));
        assertThrows(IllegalArgumentException.class,()->new ApprovedApplicationLookupDAO().find("a@b.com",1,null));
    }
}
