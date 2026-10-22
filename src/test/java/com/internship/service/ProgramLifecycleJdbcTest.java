package com.internship.service;

import com.internship.dao.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.sql.*;
import java.util.*;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;

@EnabledIfSystemProperty(named="be3.lifecycle.jdbc", matches="true")
class ProgramLifecycleJdbcTest {
    static Properties config = new Properties();
    static String server, url, schema;
    static boolean created;
    static Connection connect(String target) throws SQLException { return DriverManager.getConnection(target, config.getProperty("db.username"), config.getProperty("db.password")); }
    static Connection open() throws SQLException { return connect(url); }
    @BeforeAll static void create() throws Exception {
        try (var r = Files.newBufferedReader(Path.of("src/main/resources/db.properties"), StandardCharsets.UTF_8)) { config.load(r); }
        URI uri = URI.create(config.getProperty("db.url").substring(5));
        assertTrue(Set.of("localhost","127.0.0.1").contains(uri.getHost()));
        assertTrue(Set.of(3306,3307).contains(uri.getPort()));
        String opts = "?allowPublicKeyRetrieval=true&useSSL=false&characterEncoding=UTF-8";
        server = "jdbc:mysql://"+uri.getRawAuthority()+"/"+opts;
        schema = "be3_lifecycle_test_"+UUID.randomUUID().toString().replace("-", "");
        url = "jdbc:mysql://"+uri.getRawAuthority()+"/"+schema+opts;
        try (var c = connect(server); var s = c.createStatement()) { s.execute("CREATE DATABASE `"+schema+"` CHARACTER SET utf8mb4"); created=true; }
        try (var c = open(); var s = c.createStatement()) {
            s.execute("CREATE TABLE users(user_id BIGINT PRIMARY KEY,status VARCHAR(20),role_id INT)");
            s.execute("CREATE TABLE roles(role_id INT PRIMARY KEY,role_name VARCHAR(20))");
            s.execute("CREATE TABLE intern_profiles(profile_id INT PRIMARY KEY,university VARCHAR(100),major VARCHAR(100))");
            s.execute("CREATE TABLE internship_programs(program_id INT PRIMARY KEY,status VARCHAR(20),start_date DATE)");
            s.execute("CREATE TABLE program_enrollments(enrollment_id INT PRIMARY KEY,program_id INT,intern_profile_id INT,status VARCHAR(20),completed_at TIMESTAMP NULL)");
            s.execute("CREATE TABLE mentor_assignments(assignment_id INT PRIMARY KEY,enrollment_id INT,ended_at TIMESTAMP NULL,end_reason VARCHAR(200))");
            s.execute("INSERT INTO roles VALUES(1,'HR'),(2,'INTERN')");
            s.execute("INSERT INTO users VALUES(10,'ACTIVE',1),(11,'ACTIVE',2)");
            s.execute("INSERT INTO intern_profiles VALUES(1,'ĐH A','CNTT'),(2,NULL,''),(3,'ĐH A','Kinh tế')");
            String migration = Files.readString(Path.of("sql/schema_be3_lifecycle.sql")).replaceAll("(?m)^--.*$", "").replace("USE internship_management;", "");
            for (String statement : migration.split(";")) if (!statement.isBlank()) s.execute(statement);
        }
    }
    @AfterAll static void cleanup() throws Exception {
        if (created && schema.matches("be3_lifecycle_test_[0-9a-f]{32}")) try (var c=connect(server);var s=c.createStatement()) { s.execute("DROP DATABASE `"+schema+"`"); }
    }
    @BeforeEach void fixture() throws Exception {
        try (var c=open();var s=c.createStatement()) {
            s.execute("DELETE FROM enrollment_lifecycle_events");s.execute("DELETE FROM program_lifecycle_events");
            s.execute("DELETE FROM mentor_assignments");s.execute("DELETE FROM program_enrollments");s.execute("DELETE FROM internship_programs");
            s.execute("INSERT INTO internship_programs VALUES(1,'ACTIVE','2026-09-01'),(2,'ACTIVE','2025-09-01'),(3,'PLANNED','2026-11-01')");
            s.execute("INSERT INTO program_enrollments VALUES(1,1,1,'ACTIVE',NULL),(2,1,2,'ACTIVE',NULL),(3,1,3,'CANCELLED',NULL),(4,2,1,'COMPLETED','2025-10-01')");
            s.execute("INSERT INTO mentor_assignments VALUES(1,1,NULL,NULL),(2,2,NULL,NULL)");
        }
    }
    ProgramLifecycleService service() { return new ProgramLifecycleService(new ProgramLifecycleDAO(),new TaskAccessDAO(),ProgramLifecycleJdbcTest::open); }
    long count(String sql) throws Exception { try(var c=open();var s=c.createStatement();var r=s.executeQuery(sql)) { r.next();return r.getLong(1); } }
    @Test void programCompletionClosesActiveEnrollmentsAndMentorsWithoutDeletingHistory() throws Exception {
        assertEquals(2, service().closeProgram(10,1,"COMPLETED","HR xác nhận"));
        assertEquals(0,count("SELECT COUNT(*) FROM program_enrollments WHERE program_id=1 AND status='ACTIVE'"));
        assertEquals(2,count("SELECT COUNT(*) FROM program_enrollments WHERE completed_at IS NOT NULL AND program_id=1"));
        assertEquals(4,count("SELECT COUNT(*) FROM program_enrollments"));
        assertEquals(2,count("SELECT COUNT(*) FROM mentor_assignments WHERE ended_at IS NOT NULL"));
        assertEquals(2,count("SELECT COUNT(*) FROM enrollment_lifecycle_events WHERE actor_user_id=10 AND reason='HR xác nhận'"));
        assertThrows(IllegalStateException.class,()->service().closeProgram(10,1,"COMPLETED","Again"));
    }
    @Test void cancellationKeepsCompletedAndCancelledRecords() throws Exception {
        assertEquals(2,service().closeProgram(10,1,"CANCELLED","Dừng tuyển"));
        assertEquals(3,count("SELECT COUNT(*) FROM program_enrollments WHERE program_id=1 AND status='CANCELLED'"));
        assertEquals(1,count("SELECT COUNT(*) FROM program_enrollments WHERE status='COMPLETED'"));
        assertEquals(0,count("SELECT COUNT(*) FROM program_enrollments WHERE program_id=1 AND completed_at IS NOT NULL"));
    }
    @Test void individualCloseDoesNotCloseOtherInternsOrProgram() throws Exception {
        service().closeEnrollment(10,1,"COMPLETED","Hoàn thành" );
        assertEquals(1,count("SELECT COUNT(*) FROM program_enrollments WHERE program_id=1 AND status='ACTIVE'"));
        assertEquals(1,count("SELECT COUNT(*) FROM internship_programs WHERE program_id=1 AND status='ACTIVE'"));
        assertEquals(1,count("SELECT COUNT(*) FROM mentor_assignments WHERE ended_at IS NULL"));
    }
    @Test void roleAndReasonValidationDoNotMutate() throws Exception {
        assertThrows(SecurityException.class,()->service().closeProgram(11,1,"COMPLETED","Reason"));
        assertThrows(IllegalArgumentException.class,()->service().closeEnrollment(10,1,"COMPLETED"," "));
        assertThrows(IllegalStateException.class,()->service().closeProgram(10,3,"COMPLETED","Reason"));
        assertEquals(0,count("SELECT COUNT(*) FROM enrollment_lifecycle_events"));
    }
    @Test void auditFailureRollsBackStatusAndMentorEnd() throws Exception {
        try(var c=open();var s=c.createStatement()) { s.execute("CREATE TRIGGER reject_event BEFORE INSERT ON enrollment_lifecycle_events FOR EACH ROW SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='fixture failure'"); }
        try {
            assertThrows(SQLException.class,()->service().closeProgram(10,1,"COMPLETED","Reason"));
            assertEquals(2,count("SELECT COUNT(*) FROM program_enrollments WHERE status='ACTIVE'"));
            assertEquals(2,count("SELECT COUNT(*) FROM mentor_assignments WHERE ended_at IS NULL"));
        } finally { try(var c=open();var s=c.createStatement()) { s.execute("DROP TRIGGER reject_event"); } }
    }
    @Test void statisticsDeduplicateInternsAndSeparateEnrollmentsAndMissingGroups() throws Exception {
        try(var c=open()) {
            var dao=new ProgramStatisticsDAO(); var all=dao.summarize(c,new ProgramStatisticsDAO.Filter(null,null,null,null));
            assertEquals(3,all.interns());assertEquals(4,all.enrollments());assertEquals("25.00",all.completionRate().toString());
            assertEquals(3,all.universities().stream().mapToLong(ProgramStatisticsDAO.Group::interns).sum());
            assertTrue(all.universities().stream().anyMatch(g->g.name().equals("Chưa khai báo")&&g.interns()==1));
            var one=dao.summarize(c,new ProgramStatisticsDAO.Filter(1,null,null,null));
            assertEquals(3,one.enrollments());assertEquals("0.00",one.completionRate().toString());
            var empty=dao.summarize(c,new ProgramStatisticsDAO.Filter(3,null,null,null));assertEquals(0,empty.interns());assertEquals("0.00",empty.completionRate().toString());
        }
    }
    @Test void concurrentCloseCreatesOneDecision() throws Exception {
        try(var pool=Executors.newFixedThreadPool(2)) {
            var latch=new CountDownLatch(1);
            Callable<Boolean> work=()->{latch.await();try{service().closeEnrollment(10,1,"COMPLETED","Reason");return true;}catch(IllegalStateException e){return false;}};
            var a=pool.submit(work);var b=pool.submit(work);latch.countDown();
            assertNotEquals(a.get(10,TimeUnit.SECONDS),b.get(10,TimeUnit.SECONDS));
            assertEquals(1,count("SELECT COUNT(*) FROM enrollment_lifecycle_events"));
        }
    }
}
