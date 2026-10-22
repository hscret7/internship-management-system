package com.internship.dao;

import com.internship.model.*;
import org.junit.jupiter.api.Test;
import java.lang.reflect.Proxy;
import java.sql.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class RegistrationTransactionTest {
    private static class Fixture {
        List<String> events=new ArrayList<>();Map<Integer,Object> profileBindings=new HashMap<>();
        boolean auditFailure,profileFailure,missingKey,rollbackFailure;int accountRows=1;
        Connection connection=proxy(Connection.class,(name,args)->switch(name) {
            case "setAutoCommit" -> {assertEquals(false,args[0]);events.add("begin");yield null;}
            case "prepareStatement" -> statement(((String)args[0]).contains("intern_profiles"));
            case "commit" -> {events.add("commit");yield null;}
            case "rollback" -> {events.add("rollback");if(rollbackFailure)throw new SQLException("rollback");yield null;}
            case "close" -> {events.add("close");yield null;}
            default -> null;
        });
        PreparedStatement statement(boolean profile) {
            return proxy(PreparedStatement.class,(name,args)->{
                if(name.startsWith("set") && args.length==2) {
                    if(profile)profileBindings.put((Integer)args[0],args[1]);return null;
                }
                if(name.equals("executeUpdate")) {
                    events.add(profile?"profile":"account");
                    if(profile && profileFailure)throw new SQLException("profile");
                    return profile?1:accountRows;
                }
                if(name.equals("getGeneratedKeys"))return proxy(ResultSet.class,(n,a)->switch(n) {
                    case "next" -> !missingKey;
                    case "getLong" -> 77L;
                    default -> null;
                });
                return null;
            });
        }
        UserDAO dao() {return new UserDAO() {
            @Override protected Connection openRegistrationConnection() {return connection;}
            @Override protected void recordRegistrationAudit(Connection c,long id) throws SQLException {
                assertSame(connection,c);assertEquals(77,id);events.add("audit");
                if(auditFailure)throw new SQLException("audit");
            }
        };}
        long create() throws SQLException {
            User u=new User();u.setUsername("hue");u.setEmail("hue@example.com");u.setFullName("Minh Huệ");
            u.setPasswordHash("hashed");Role r=new Role();r.setRoleId(4);u.setRole(r);u.setStatus(UserStatus.ACTIVE);
            return dao().createWithProfile(u,RegistrationProfile.parse("0912345678","ICTU","ATTT","3.75"));
        }
    }
    @Test void bothInsertsPrecedeSingleCommitAndShareGeneratedUserId() throws Exception {
        Fixture f=new Fixture();assertEquals(77,f.create());
        assertEquals(List.of("begin","account","profile","audit","commit","close"),f.events);
        assertEquals(77L,f.profileBindings.get(1));assertEquals("Minh Huệ",f.profileBindings.get(2));
        assertEquals("0912345678",f.profileBindings.get(4));assertEquals("ICTU",f.profileBindings.get(5));
        assertEquals("ATTT",f.profileBindings.get(6));assertEquals("3.75",f.profileBindings.get(7).toString());
    }
    @Test void auditFailureRollsBackAccountAndProfile() {
        Fixture f=new Fixture();f.auditFailure=true;assertThrows(SQLException.class,f::create);
        assertEquals(List.of("begin","account","profile","audit","rollback","close"),f.events);
    }
    @Test void failedProfileRollsBackAndNeverCommits() {
        Fixture f=new Fixture();f.profileFailure=true;
        assertThrows(SQLException.class,f::create);
        assertEquals(List.of("begin","account","profile","rollback","close"),f.events);
    }
    @Test void missingGeneratedKeyRollsBackBeforeProfileInsert() {
        Fixture f=new Fixture();f.missingKey=true;assertThrows(SQLException.class,f::create);
        assertEquals(List.of("begin","account","rollback","close"),f.events);
    }
    @Test void failedAccountInsertDoesNotInsertProfile() {
        Fixture f=new Fixture();f.accountRows=0;assertThrows(SQLException.class,f::create);
        assertEquals(List.of("begin","account","rollback","close"),f.events);
    }
    @Test void rollbackFailureDoesNotHideOriginalFailure() {
        Fixture f=new Fixture();f.profileFailure=true;f.rollbackFailure=true;
        SQLException error=assertThrows(SQLException.class,f::create);
        assertEquals("profile",error.getMessage());assertEquals("rollback",error.getSuppressed()[0].getMessage());
        assertFalse(f.events.contains("commit"));assertEquals("close",f.events.getLast());
    }
    interface Calls {Object call(String name,Object[] args) throws Throwable;}
    static <T> T proxy(Class<T> type,Calls calls) {
        return type.cast(Proxy.newProxyInstance(type.getClassLoader(),new Class<?>[]{type},
            (p,m,a)->calls.call(m.getName(),a==null?new Object[0]:a)));
    }
}
