package com.internship.service;

import com.internship.dao.*;
import com.internship.model.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import java.io.*;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.sql.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;

/** Opt-in real MySQL tests. Only a newly-created, random, local fixture database is changed. */
@EnabledIfSystemProperty(named = "be3.schedule.jdbc", matches = "true")
class ScheduleLeaveJdbcTest {
    static Properties config;
    static String serverUrl, fixtureUrl, schema;
    static boolean created;
    final LocalDate day = LocalDate.of(2026, 10, 12);
    final LocalTime start = LocalTime.of(9, 0), end = LocalTime.of(12, 0);
    ScheduleLeaveService service;
    User hr, intern;

    @BeforeAll static void createFixture() throws Exception {
        config = new Properties();
        try (var reader = Files.newBufferedReader(Path.of("src/main/resources/db.properties"), StandardCharsets.UTF_8)) {
            config.load(reader);
        }
        URI uri = URI.create(config.getProperty("db.url").substring(5));
        assertTrue(Set.of("localhost", "127.0.0.1").contains(uri.getHost()), "Local fixture server only");
        assertTrue(uri.getPort() == 3306 || uri.getPort() == 3307, "Unexpected local MySQL port");
        String options = "?allowPublicKeyRetrieval=true&useSSL=false&characterEncoding=UTF-8&connectionTimeZone=UTC&forceConnectionTimeZoneToSession=true";
        serverUrl = "jdbc:mysql://" + uri.getRawAuthority() + "/" + options;
        schema = "be3_schedule_test_" + UUID.randomUUID().toString().replace("-", "");
        fixtureUrl = "jdbc:mysql://" + uri.getRawAuthority() + "/" + schema + options;
        try (Connection c = connect(serverUrl); Statement s = c.createStatement()) {
            s.execute("CREATE DATABASE `" + schema + "` CHARACTER SET utf8mb4"); created = true;
        }
        try (Connection c = connect(fixtureUrl); Statement s = c.createStatement()) {
            s.execute("CREATE TABLE users(user_id BIGINT PRIMARY KEY) ENGINE=InnoDB");
            s.execute("CREATE TABLE intern_profiles(profile_id INT PRIMARY KEY,user_id BIGINT NOT NULL,full_name VARCHAR(100)) ENGINE=InnoDB");
            s.execute("CREATE TABLE internship_programs(program_id INT PRIMARY KEY,program_name VARCHAR(100),status VARCHAR(20),start_date DATE,end_date DATE) ENGINE=InnoDB");
            s.execute("CREATE TABLE program_enrollments(enrollment_id INT PRIMARY KEY,intern_profile_id INT NOT NULL,program_id INT NOT NULL,status VARCHAR(20)) ENGINE=InnoDB");
            s.execute("INSERT INTO users VALUES(10),(11),(100)");
            s.execute("INSERT INTO intern_profiles VALUES(1,10,'TTS kiểm thử'),(2,11,'TTS khác')");
            s.execute("INSERT INTO internship_programs VALUES(1,'Program QA','ACTIVE','2026-09-01','2026-11-30')");
            s.execute("INSERT INTO program_enrollments VALUES(1,1,1,'ACTIVE'),(2,2,1,'ACTIVE')");
            String migration = Files.readString(Path.of("sql/schema_be3_schedule_leave.sql"))
                    .replaceAll("(?m)^--.*$", "").replace("USE internship_management;", "");
            for (String sql : migration.split(";")) if (!sql.isBlank()) s.execute(sql);
        }
    }
    static Connection connect(String url) throws SQLException {
        return DriverManager.getConnection(url, config.getProperty("db.username"), config.getProperty("db.password"));
    }
    static Connection open() throws SQLException { return connect(fixtureUrl); }
    @AfterAll static void removeOwnFixture() throws Exception {
        if (created && schema != null && schema.matches("be3_schedule_test_[0-9a-f]{32}")) {
            try (Connection c = connect(serverUrl); Statement s = c.createStatement()) { s.execute("DROP DATABASE `" + schema + "`"); }
        }
    }
    @BeforeEach void setup() throws Exception {
        try (Connection c = open(); Statement s = c.createStatement()) {
            s.execute("DELETE FROM leave_requests"); s.execute("DELETE FROM work_schedules");
            s.execute("UPDATE program_enrollments SET status='ACTIVE'");
        }
        service = new ScheduleLeaveService(new ScheduleEnrollmentDAO(), new WorkScheduleDAO(), new LeaveRequestDAO(),
                ScheduleLeaveJdbcTest::open, Clock.fixed(Instant.parse("2026-10-09T16:00:00Z"), ZoneOffset.UTC));
        hr = ScheduleLeaveServiceTest.user(100, "HR"); intern = ScheduleLeaveServiceTest.user(10, "INTERN");
    }
    @Test void scheduleRoundTripAndCancellationPreserveRecord() throws Exception {
        int id = service.createSchedule(hr, 1, day, start, end, "Ghi chú kiểm thử");
        WorkSchedule saved = service.schedules(intern, 1, null, day, day).getFirst();
        assertEquals(id, saved.scheduleId()); assertEquals(start, saved.startTime()); assertEquals("Ghi chú kiểm thử", saved.note());
        service.updateSchedule(hr, id, day, start.plusMinutes(30), end, "Sửa lịch");
        service.cancelSchedule(hr, id);
        assertEquals("CANCELLED", service.schedules(intern, 1, null, day, day).getFirst().status());
    }
    @Test void approvedLeaveMarksCalendarAndDecisionPersistsAcrossConnections() throws Exception {
        service.createSchedule(hr, 1, day, start, end, "");
        int id = service.requestLeave(intern, 1, day, day, "Lý do kiểm thử");
        assertFalse(service.schedules(intern, 1, null, day, day).getFirst().approvedLeave());
        service.decideLeave(hr, id, true, null);
        LeaveRequest saved = service.leaves(intern, 1, null, "APPROVED", day, day).getFirst();
        assertEquals(100L, saved.reviewedBy()); assertNotNull(saved.reviewedAt());
        assertTrue(service.schedules(intern, 1, null, day, day).getFirst().approvedLeave());
        assertThrows(ScheduleLeaveException.class, () -> service.decideLeave(hr, id, false, "Again"));
        assertEquals("APPROVED", service.leaves(intern, 1, null, null, day, day).getFirst().status());
    }
    @Test void rejectionKeepsReasonAndDoesNotMarkCalendar() throws Exception {
        service.createSchedule(hr, 1, day, start, end, "");
        int id = service.requestLeave(intern, 1, day, day, "Reason");
        service.decideLeave(hr, id, false, "Thiếu thông tin");
        assertEquals("Thiếu thông tin", service.leaves(intern, 1, null, "REJECTED", day, day).getFirst().rejectReason());
        assertFalse(service.schedules(intern, 1, null, day, day).getFirst().approvedLeave());
        assertTrue(service.requestLeave(intern, 1, day, day, "New reason") > id);
    }
    @Test void ownershipFiltersPreventCrossInternData() throws Exception {
        service.createSchedule(hr, 2, day, start, end, "Other intern");
        assertTrue(service.schedules(intern, null, null, day, day).isEmpty());
        assertThrows(ScheduleLeaveException.class, () -> service.schedules(intern, 2, null, day, day));
        assertThrows(ScheduleLeaveException.class, () -> service.requestLeave(intern, 2, day, day, "Reason"));
    }
    private void concurrent(Callable<Integer> action) throws Exception {
        try (var pool = Executors.newFixedThreadPool(2)) {
            var together = new CountDownLatch(1);
            Callable<Boolean> task = () -> { together.await();
                try { action.call(); return true; } catch (ScheduleLeaveException e) { assertEquals(409, e.status()); return false; }
            };
            var a = pool.submit(task); var b = pool.submit(task); together.countDown();
            assertNotEquals(a.get(10, TimeUnit.SECONDS), b.get(10, TimeUnit.SECONDS));
        }
    }
    @Test void mysqlParentLockPreventsConcurrentOverlappingShifts() throws Exception {
        concurrent(() -> service.createSchedule(hr, 1, day, start, end, ""));
        assertEquals(1, service.schedules(intern, 1, null, day, day).size());
    }
    @Test void mysqlParentLockPreventsConcurrentLeaveRequests() throws Exception {
        concurrent(() -> service.requestLeave(intern, 1, day, day, "Reason"));
        assertEquals(1, service.leaves(intern, 1, null, null, day, day).size());
    }
}
