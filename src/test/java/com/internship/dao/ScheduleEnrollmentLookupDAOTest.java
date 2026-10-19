package com.internship.dao;

import org.junit.jupiter.api.Test;
import java.lang.reflect.*;
import java.sql.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class ScheduleEnrollmentLookupDAOTest {
    String sql;
    final Map<Integer,Object> bound=new HashMap<>();
    static <T>T proxy(Class<T> type,InvocationHandler handler) {
        return type.cast(Proxy.newProxyInstance(type.getClassLoader(),new Class<?>[]{type},handler));
    }
    Connection connection() {
        ResultSet rows=proxy(ResultSet.class,(p,m,a)->m.getName().equals("next")?false:null);
        PreparedStatement statement=proxy(PreparedStatement.class,(p,m,a)->switch(m.getName()){
            case "setLong","setString"->{bound.put((Integer)a[0],a[1]);yield null;}
            case "executeQuery"->rows;
            default->null;
        });
        return proxy(Connection.class,(p,m,a)->{
            if(m.getName().equals("prepareStatement")){sql=(String)a[0];return statement;}return null;
        });
    }
    @Test void ownEnrollmentQueryBindsOwnerInSql() throws Exception {
        new ScheduleEnrollmentDAO().choices(42L,null,connection());
        assertTrue(sql.contains("AND ip.user_id=?"));assertEquals(Map.of(1,42L),bound);
    }
    @Test void emailQueryUsesBoundExactMatchAndDoesNotPickFirst() throws Exception {
        new ScheduleEnrollmentDAO().choices(null,"a@example.com",connection());
        assertFalse(sql.toUpperCase().contains("LIMIT 1"));assertFalse(sql.toUpperCase().contains(" LIKE "));
        assertTrue(sql.contains("LOWER(TRIM(u.email))=LOWER(?)"));
        assertEquals(Map.of(1,"a@example.com",2,"a@example.com"),bound);
    }
    @Test void unscopedQueryIsRejectedBeforeDatabase() {
        assertThrows(IllegalArgumentException.class,()->new ScheduleEnrollmentDAO().choices(null,null,null));
    }

    @Test void directoryUsesRoleNameAndOnlyActiveUsers()throws Exception{
        new ScheduleEnrollmentDAO().listEnrolledInterns(connection());
        assertTrue(sql.contains("role.role_name='INTERN'"));assertTrue(sql.contains("u.status='ACTIVE'"));
        assertFalse(sql.contains("u.role_id = 4"));
    }

}
