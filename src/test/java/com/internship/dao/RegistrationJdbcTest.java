package com.internship.dao;

import com.internship.model.*;
import com.internship.service.UserService;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import java.nio.file.*;
import java.sql.*;
import static org.junit.jupiter.api.Assertions.*;

/** Destructive fixture limited to the script-owned database on loopback port 13321. */
@EnabledIfSystemProperty(named="be1.registration.jdbc", matches="true")
class RegistrationJdbcTest {
    private static final String URL="jdbc:mysql://127.0.0.1:13321/be1_registration_test?characterEncoding=UTF-8";
    private Connection open() throws SQLException { return DriverManager.getConnection(URL,"root",""); }
    private UserDAO dao() { return new UserDAO() {
        @Override protected Connection openRegistrationConnection() throws SQLException { return open(); }
        @Override public boolean existsByUsername(String name) { return false; }
        @Override public boolean existsByEmail(String email) { return false; }
    }; }
    private UserService service() { return new UserService(dao(),new RoleDAO() {
        @Override public Role findByName(String name) { Role r=new Role();r.setRoleId(4);r.setRoleName("INTERN");return r; }
    }); }
    @BeforeEach void fixture() throws Exception {
        assertEquals(URL,System.getProperty("be1.registration.jdbc.url",URL));
        try(var c=open();var s=c.createStatement()) {
            try(var rs=s.executeQuery("SELECT DATABASE()")) { assertTrue(rs.next());assertEquals("be1_registration_test",rs.getString(1)); }
            s.execute("DROP TABLE IF EXISTS audit_logs");s.execute("DROP TABLE IF EXISTS intern_profiles");s.execute("DROP TABLE IF EXISTS users");s.execute("DROP TABLE IF EXISTS roles");
            String core=Files.readString(Path.of("database/schema.sql"));
            for(String command:core.replaceAll("(?m)^--.*$","").split(";"))
                if(!command.isBlank() && !command.strip().toUpperCase().startsWith("CREATE DATABASE") && !command.strip().toUpperCase().startsWith("USE ")) s.execute(command);
            String schema=Files.readString(Path.of("sql/schema_be2.sql"));
            String profile=schema.substring(schema.indexOf("CREATE TABLE IF NOT EXISTS intern_profiles"),schema.indexOf("-- 4."));
            s.execute(profile.substring(0,profile.lastIndexOf(';')));
            String audit=Files.readString(Path.of("sql/schema_be1_audit.sql")).replaceAll("(?m)^--.*$","");
            for(String command:audit.split(";"))if(!command.isBlank())s.execute(command);
        }
    }
    private int count(String table) throws Exception {
        try(var c=open();var s=c.createStatement();var rs=s.executeQuery("SELECT COUNT(*) FROM "+table)) {rs.next();return rs.getInt(1);}
    }
    @Test void publicRegistrationPersistsAllFieldsAndLinksProfile() throws Exception {
        User u=service().registerIntern("hue","hue@example.com","Minh Huệ","Pass12345","Pass12345","0912345678","ICTU","An toàn thông tin","3.75");
        try(var c=open();var s=c.createStatement();var rs=s.executeQuery("SELECT * FROM intern_profiles")) {
            assertTrue(rs.next());assertEquals(u.getUserId(),rs.getLong("user_id"));assertEquals("Minh Huệ",rs.getString("full_name"));
            assertEquals("0912345678",rs.getString("phone"));assertEquals("ICTU",rs.getString("university"));
            assertEquals("An toàn thông tin",rs.getString("major"));assertEquals("3.75",rs.getBigDecimal("gpa").toPlainString());
        }
        assertEquals(1,count("users"));assertEquals(1,count("intern_profiles"));
        assertTrue(com.internship.util.PasswordUtil.verifyPassword("Pass12345",u.getPasswordHash()));
    }
    @Test void registrationAuditIsAnonymousAndContainsOnlyObjectMetadata() throws Exception {
        User user=service().registerIntern("hue","hue@example.com","Minh Huệ","Pass12345","Pass12345",null,null,null,null);
        try(var c=open();var st=c.createStatement();var r=st.executeQuery("SELECT * FROM audit_logs")) {
            assertTrue(r.next());assertNull(r.getObject("actor_user_id"));assertEquals("REGISTER",r.getString("action"));
            assertEquals("USER",r.getString("object_type"));assertEquals(user.getUserId(),r.getLong("object_id"));
            assertEquals("SUCCESS",r.getString("result"));assertFalse(r.next());
        }
    }
    @Test void auditFailureRollsBackRegistrationAndProfile() throws Exception {
        try(var c=open();var st=c.createStatement()){st.execute("CREATE TRIGGER fail_audit BEFORE INSERT ON audit_logs FOR EACH ROW SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='fixture audit failure'");}
        assertThrows(IllegalStateException.class,()->service().registerIntern("hue","hue@example.com","Minh Huệ","Pass12345","Pass12345",null,null,null,null));
        assertEquals(0,count("users"));assertEquals(0,count("intern_profiles"));assertEquals(0,count("audit_logs"));
    }
    @Test void optionalProfileFieldsAreSavedAsEmptyAndGpaNull() throws Exception {
        service().registerIntern("hue","hue@example.com","Minh Huệ","Pass12345","Pass12345",null,null,null,null);
        try(var c=open();var s=c.createStatement();var rs=s.executeQuery("SELECT phone,university,major,gpa FROM intern_profiles")) {
            assertTrue(rs.next());assertEquals("",rs.getString(1));assertEquals("",rs.getString(2));assertEquals("",rs.getString(3));assertNull(rs.getBigDecimal(4));
        }
    }
    @Test void profileFailureRollsBackAccount() throws Exception {
        try(var c=open();var s=c.createStatement()) {s.execute("CREATE TRIGGER fail_profile BEFORE INSERT ON intern_profiles FOR EACH ROW SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='fixture failure'");}
        assertThrows(IllegalStateException.class,()->service().registerIntern("hue","hue@example.com","Minh Huệ","Pass12345","Pass12345",null,null,null,null));
        assertEquals(0,count("users"));assertEquals(0,count("intern_profiles"));assertEquals(0,count("audit_logs"));
    }
    @Test void duplicateAccountDoesNotCreateAnotherProfile() throws Exception {
        service().registerIntern("hue","hue@example.com","Minh Huệ","Pass12345","Pass12345",null,null,null,null);
        assertThrows(IllegalStateException.class,()->service().registerIntern("hue","other@example.com","Other","Pass12345","Pass12345",null,null,null,null));
        assertEquals(1,count("users"));assertEquals(1,count("intern_profiles"));
    }
    @Test void invalidGpaAndProfileLengthsCreateNoAccount() throws Exception {
        assertThrows(IllegalArgumentException.class,()->service().registerIntern("hue","hue@example.com","Minh Huệ","Pass12345","Pass12345",null,null,null,"4.01"));
        assertThrows(IllegalArgumentException.class,()->service().registerIntern("hue","hue@example.com","x".repeat(101),"Pass12345","Pass12345",null,null,null,null));
        assertEquals(0,count("users"));assertEquals(0,count("intern_profiles"));assertEquals(0,count("audit_logs"));
    }
}
