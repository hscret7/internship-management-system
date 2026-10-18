package com.internship.service;

import com.internship.dao.AttendanceDAO;
import com.internship.dao.WorkScheduleDAO;
import com.internship.model.Attendance;
import com.internship.model.Role;
import com.internship.model.User;
import com.internship.model.WorkSchedule;
import com.internship.util.ConnectionProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class AttendanceServiceTest {

    private StubAttendanceDAO attendanceDAO;
    private StubWorkScheduleDAO workScheduleDAO;
    private AttendanceService attendanceService;

    private User hrUser;
    private User internUser;
    private User otherInternUser;
    private User mentorUser;
    private User invalidUser;

    @BeforeEach
    void setUp() {
        attendanceDAO = new StubAttendanceDAO();
        workScheduleDAO = new StubWorkScheduleDAO();
        ConnectionProvider connectionProvider = this::createMockConnection;
        attendanceService = new AttendanceService(attendanceDAO, workScheduleDAO, connectionProvider);

        hrUser = createUser(100L, "HR");
        internUser = createUser(200L, "INTERN");
        otherInternUser = createUser(201L, "INTERN");
        mentorUser = createUser(300L, "MENTOR");
        invalidUser = createUser(400L, "GUEST");
    }

    private User createUser(long id, String roleName) {
        User u = new User();
        u.setUserId(id);
        Role r = new Role();
        r.setRoleName(roleName);
        u.setRole(r);
        return u;
    }

    private Connection createMockConnection() {
        return (Connection) Proxy.newProxyInstance(
                Connection.class.getClassLoader(),
                new Class<?>[]{Connection.class},
                (proxy, method, args) -> {
                    String name = method.getName();
                    if ("close".equals(name) || "commit".equals(name) || "rollback".equals(name) ||
                            "setAutoCommit".equals(name) || "setTransactionIsolation".equals(name) ||
                            "setReadOnly".equals(name)) {
                        return null;
                    }
                    throw new UnsupportedOperationException("Mock connection method not implemented: " + name);
                }
        );
    }

    // ================== Stubs ==================

    static class StubAttendanceDAO extends AttendanceDAO {
        @Override public void lockOwnedEnrollment(int id, long owner, Connection c) { }
        final List<Attendance> list = new ArrayList<>();
        Attendance singleRecord;

        @Override
        public Attendance findByEnrollmentAndDate(int enrollmentId, LocalDate date, Connection conn) {
            return singleRecord;
        }

        @Override
        public List<Attendance> getAttendanceReport(Long ownerUserId, int programId, int enrollmentId, LocalDate fromDate, LocalDate toDate, Connection conn) {
            return new ArrayList<>(list);
        }

        @Override
        public void checkIn(int enrollmentId, LocalDate date, java.sql.Timestamp time, Connection conn) {
            Attendance a = new Attendance();
            a.setEnrollmentId(enrollmentId);
            a.setWorkDate(date);
            a.setCheckInTime(time.toLocalDateTime());
            singleRecord = a;
        }

        @Override
        public void checkOut(int attendanceId, java.sql.Timestamp time, Connection conn) {
            if (singleRecord != null) {
                singleRecord.setCheckOutTime(time.toLocalDateTime());
            }
        }
    }

    static class StubWorkScheduleDAO extends WorkScheduleDAO {
        final List<WorkSchedule> list = new ArrayList<>();
        Long lastQueriedOwner;
        Integer lastQueriedEnrollment;
        Integer lastQueriedProgram;
        LocalDate lastFrom;
        LocalDate lastTo;

        @Override
        public List<WorkSchedule> list(Long owner, Integer enrollment, Integer program, LocalDate from, LocalDate to, Connection c) {
            this.lastQueriedOwner = owner;
            this.lastQueriedEnrollment = enrollment;
            this.lastQueriedProgram = program;
            this.lastFrom = from;
            this.lastTo = to;
            return new ArrayList<>(list);
        }
    }

    // ================== Tests ==================

    @Test
    @DisplayName("Actor cannot be null")
    void testActorNullThrowsException() {
        assertThrows(IllegalArgumentException.class, () ->
                attendanceService.getReport(null, 1, 1, LocalDate.now(), LocalDate.now()));
    }

    @Test
    @DisplayName("Unauthorized role cannot view attendance report")
    void testActorUnauthorizedRoleThrowsException() {
        assertThrows(IllegalArgumentException.class, () ->
                attendanceService.getReport(invalidUser, 1, 1, LocalDate.now(), LocalDate.now()));
    }

    @Test
    @DisplayName("fromDate after toDate throws IllegalArgumentException")
    void testFromDateAfterToDateThrowsException() {
        LocalDate from = LocalDate.of(2026, 10, 20);
        LocalDate to = LocalDate.of(2026, 10, 10);
        assertThrows(IllegalArgumentException.class, () ->
                attendanceService.getReport(hrUser, 1, 1, from, to));
    }

    @Test
    @DisplayName("Date range exceeding 367 days throws IllegalArgumentException")
    void testRangeExceeding367DaysThrowsException() {
        LocalDate from = LocalDate.of(2025, 1, 1);
        LocalDate to = LocalDate.of(2026, 2, 1); // 396 days
        assertThrows(IllegalArgumentException.class, () ->
                attendanceService.getReport(hrUser, 1, 1, from, to));
    }

    @Test
    @DisplayName("Null dates default to last 30 days up to today safely")
    void testNullDatesDefaultGracefully() throws SQLException {
        // Freeze an instant at which the server date (UTC+9) differs from Vietnam.
        var clock = java.time.Clock.fixed(java.time.Instant.parse("2026-10-18T16:00:00Z"),
                java.time.ZoneId.of("Asia/Tokyo"));
        attendanceService = new AttendanceService(attendanceDAO, workScheduleDAO,
                this::createMockConnection, clock);
        attendanceService.getReport(hrUser, 1, 1, null, null);
        assertNotNull(workScheduleDAO.lastFrom);
        assertNotNull(workScheduleDAO.lastTo);
        assertEquals(LocalDate.of(2026, 10, 18), workScheduleDAO.lastTo);
        assertEquals(LocalDate.of(2026, 9, 18), workScheduleDAO.lastFrom);
    }

    @Test
    @DisplayName("Intern actor can only query their own schedules (owner isolation)")
    void testInternOnlyQueriesOwnSchedule() throws SQLException {
        attendanceService.getReport(internUser, 5, 2, LocalDate.now(), LocalDate.now());
        assertEquals(200L, workScheduleDAO.lastQueriedOwner);
        assertEquals(5, workScheduleDAO.lastQueriedEnrollment);
        assertEquals(2, workScheduleDAO.lastQueriedProgram);
    }

    @Test
    @DisplayName("HR actor queries without owner constraint")
    void testHrQueriesWithoutOwnerConstraint() throws SQLException {
        attendanceService.getReport(hrUser, 5, 2, LocalDate.now(), LocalDate.now());
        assertNull(workScheduleDAO.lastQueriedOwner);
        assertEquals(5, workScheduleDAO.lastQueriedEnrollment);
        assertEquals(2, workScheduleDAO.lastQueriedProgram);
    }

    @Test
    @DisplayName("Full attendance calculation: PRESENT, MISSING_CHECKOUT, LEAVE, ABSENT")
    void testAttendanceCalculations() throws SQLException {
        LocalDate day1 = LocalDate.of(2026, 10, 1);
        LocalDate day2 = LocalDate.of(2026, 10, 2);
        LocalDate day3 = LocalDate.of(2026, 10, 3);
        LocalDate day4 = LocalDate.of(2026, 10, 4);

        // Schedule 1: Attended full shift (08:00 - 12:00) -> 4 hours
        workScheduleDAO.list.add(new WorkSchedule(1, 10, day1, LocalTime.of(8, 0), LocalTime.of(12, 0), "Ca sang", "ACTIVE", "Nam", "Java Pro", false));
        Attendance a1 = new Attendance();
        a1.setEnrollmentId(10);
        a1.setWorkDate(day1);
        a1.setCheckInTime(LocalDateTime.of(2026, 10, 1, 8, 0));
        a1.setCheckOutTime(LocalDateTime.of(2026, 10, 1, 12, 0));
        attendanceDAO.list.add(a1);

        // Schedule 2: Missing checkout -> 0 hours, MISSING_CHECKOUT
        workScheduleDAO.list.add(new WorkSchedule(2, 10, day2, LocalTime.of(8, 0), LocalTime.of(12, 0), "Ca sang", "ACTIVE", "Nam", "Java Pro", false));
        Attendance a2 = new Attendance();
        a2.setEnrollmentId(10);
        a2.setWorkDate(day2);
        a2.setCheckInTime(LocalDateTime.of(2026, 10, 2, 8, 5));
        a2.setCheckOutTime(null);
        attendanceDAO.list.add(a2);

        // Schedule 3: Approved leave -> 0 hours, LEAVE
        workScheduleDAO.list.add(new WorkSchedule(3, 10, day3, LocalTime.of(8, 0), LocalTime.of(12, 0), "Ca sang", "ACTIVE", "Nam", "Java Pro", true));

        // Schedule 4: Unexcused absent -> 0 hours, ABSENT
        workScheduleDAO.list.add(new WorkSchedule(4, 10, day4, LocalTime.of(8, 0), LocalTime.of(12, 0), "Ca sang", "ACTIVE", "Nam", "Java Pro", false));

        List<AttendanceService.AttendanceReportRow> report = attendanceService.getReport(hrUser, 10, null, day1, day4);
        assertEquals(4, report.size());

        // Row 1
        AttendanceService.AttendanceReportRow r1 = report.get(0);
        assertEquals("PRESENT", r1.getStatus());
        assertEquals(4.0, r1.getWorkedHours());
        assertEquals(10, r1.getEnrollmentId());
        assertEquals("Nam", r1.getInternName());
        assertEquals("Java Pro", r1.getProgramName());

        // Row 2
        AttendanceService.AttendanceReportRow r2 = report.get(1);
        assertEquals("MISSING_CHECKOUT", r2.getStatus());
        assertEquals(0.0, r2.getWorkedHours());

        // Row 3
        AttendanceService.AttendanceReportRow r3 = report.get(2);
        assertEquals("LEAVE", r3.getStatus());
        assertTrue(r3.isApprovedLeave());
        assertEquals(0.0, r3.getWorkedHours());

        // Row 4
        AttendanceService.AttendanceReportRow r4 = report.get(3);
        assertEquals("ABSENT", r4.getStatus());
        assertFalse(r4.isApprovedLeave());
        assertEquals(0.0, r4.getWorkedHours());
    }

    @Test
    @DisplayName("Filtering report by status")
    void testFilterByStatus() throws SQLException {
        LocalDate day1 = LocalDate.of(2026, 10, 1);
        LocalDate day2 = LocalDate.of(2026, 10, 2);

        workScheduleDAO.list.add(new WorkSchedule(1, 10, day1, LocalTime.of(8, 0), LocalTime.of(12, 0), null, "ACTIVE", "Nam", "Prog", false));
        workScheduleDAO.list.add(new WorkSchedule(2, 10, day2, LocalTime.of(8, 0), LocalTime.of(12, 0), null, "ACTIVE", "Nam", "Prog", true));

        List<AttendanceService.AttendanceReportRow> leavesOnly = attendanceService.getReport(hrUser, 10, null, day1, day2, "LEAVE");
        assertEquals(1, leavesOnly.size());
        assertEquals("LEAVE", leavesOnly.get(0).getStatus());

        List<AttendanceService.AttendanceReportRow> absentsOnly = attendanceService.getReport(hrUser, 10, null, day1, day2, "ABSENT");
        assertEquals(1, absentsOnly.size());
        assertEquals("ABSENT", absentsOnly.get(0).getStatus());
    }

    @Test
    @DisplayName("Attendance summary calculation")
    void testGetSummary() throws SQLException {
        LocalDate day1 = LocalDate.of(2026, 10, 1);
        LocalDate day2 = LocalDate.of(2026, 10, 2);

        workScheduleDAO.list.add(new WorkSchedule(1, 10, day1, LocalTime.of(8, 0), LocalTime.of(12, 0), null, "ACTIVE", "Nam", "Prog", false));
        Attendance a1 = new Attendance();
        a1.setEnrollmentId(10);
        a1.setWorkDate(day1);
        a1.setCheckInTime(LocalDateTime.of(2026, 10, 1, 8, 0));
        a1.setCheckOutTime(LocalDateTime.of(2026, 10, 1, 11, 30)); // 3.5 hours
        attendanceDAO.list.add(a1);

        workScheduleDAO.list.add(new WorkSchedule(2, 10, day2, LocalTime.of(8, 0), LocalTime.of(12, 0), null, "ACTIVE", "Nam", "Prog", true));

        AttendanceService.AttendanceSummary summary = attendanceService.getSummary(hrUser, 10, null, day1, day2);
        assertEquals(2, summary.totalSchedules());
        assertEquals(1, summary.presentCount());
        assertEquals(1, summary.leaveCount());
        assertEquals(0, summary.absentCount());
        assertEquals(0, summary.missingCheckoutCount());
        assertEquals(3.5, summary.totalWorkedHours());
    }

    @Test
    @DisplayName("CheckIn rule validation: intern only, requires schedule, rejects duplicates and approved leave")
    void testCheckInValidation() {
        LocalDate today = LocalDate.now();

        // Non-intern rejected
        assertThrows(IllegalArgumentException.class, () -> attendanceService.checkIn(hrUser, 10));

        // No schedule today rejected
        assertThrows(IllegalArgumentException.class, () -> attendanceService.checkIn(internUser, 10));

        // Schedule with approved leave rejected
        workScheduleDAO.list.add(new WorkSchedule(1, 10, today, LocalTime.of(8, 0), LocalTime.of(12, 0), null, "ACTIVE", "Nam", "Prog", true));
        assertThrows(IllegalArgumentException.class, () -> attendanceService.checkIn(internUser, 10));

        // Active schedule allows check-in
        workScheduleDAO.list.clear();
        workScheduleDAO.list.add(new WorkSchedule(2, 10, today, LocalTime.of(8, 0), LocalTime.of(12, 0), null, "ACTIVE", "Nam", "Prog", false));
        assertDoesNotThrow(() -> attendanceService.checkIn(internUser, 10));
        assertNotNull(attendanceDAO.singleRecord);
        assertEquals(10, attendanceDAO.singleRecord.getEnrollmentId());

        // Duplicate check-in rejected
        assertThrows(IllegalArgumentException.class, () -> attendanceService.checkIn(internUser, 10));
    }

    @Test
    @DisplayName("CheckOut rule validation: intern only, requires checkin, rejects duplicate checkout")
    void testCheckOutValidation() {
        LocalDate today = LocalDate.now();
        workScheduleDAO.list.add(new WorkSchedule(1, 10, today, LocalTime.of(8, 0), LocalTime.of(12, 0), null, "ACTIVE", "Nam", "Prog", false));

        // Cannot checkout if not checked in
        assertThrows(IllegalArgumentException.class, () -> attendanceService.checkOut(internUser, 10));

        // Check in first
        attendanceDAO.singleRecord = new Attendance();
        attendanceDAO.singleRecord.setAttendanceId(1);
        attendanceDAO.singleRecord.setEnrollmentId(10);
        attendanceDAO.singleRecord.setWorkDate(today);
        attendanceDAO.singleRecord.setCheckInTime(LocalDateTime.now().minusHours(2));

        // Check out succeeds
        assertDoesNotThrow(() -> attendanceService.checkOut(internUser, 10));
        assertNotNull(attendanceDAO.singleRecord.getCheckOutTime());

        // Duplicate checkout rejected
        assertThrows(IllegalArgumentException.class, () -> attendanceService.checkOut(internUser, 10));
    }
}
