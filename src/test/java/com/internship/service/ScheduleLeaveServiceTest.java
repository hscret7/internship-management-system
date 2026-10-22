package com.internship.service;

import com.internship.dao.*;
import com.internship.dao.ScheduleEnrollmentDAO.Context;
import com.internship.model.*;
import org.junit.jupiter.api.*;
import java.sql.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.locks.ReentrantLock;
import static org.junit.jupiter.api.Assertions.*;

class ScheduleLeaveServiceTest {
    final LocalDate day = LocalDate.of(2026, 10, 12);
    final LocalTime start = LocalTime.of(9, 0), end = LocalTime.of(12, 0);
    final TestConnections connections = new TestConnections();
    Context context = new Context(1, 10L, "ACTIVE", "ACTIVE", day.minusDays(30), day.plusDays(30));
    final ReentrantLock parent = new ReentrantLock();
    final List<WorkSchedule> shifts = new CopyOnWriteArrayList<>();
    final List<LeaveRequest> requests = new CopyOnWriteArrayList<>();
    boolean overlap, databaseFailure;
    Long filteredOwner;
    ScheduleLeaveService service;
    final User hr = user(100L, "HR"), intern = user(10L, "INTERN");

    @BeforeEach void setup() {
        ScheduleEnrollmentDAO contexts = new ScheduleEnrollmentDAO() {
            @Override public Context find(int id, boolean lock, Connection c) {
                if (lock) { parent.lock(); connections.onFinish(parent::unlock); }
                return id == 1 ? context : null;
            }
        };
        WorkScheduleDAO schedules = new WorkScheduleDAO() {
            @Override public boolean overlaps(int id, LocalDate date, LocalTime from, LocalTime to, int except, Connection c) {
                return overlap || shifts.stream().anyMatch(s -> s.scheduleId() != except && s.workDate().equals(date)
                        && s.status().equals("ACTIVE") && from.isBefore(s.endTime()) && to.isAfter(s.startTime()));
            }
            @Override public WorkSchedule find(int id, Connection c) { return shifts.stream().filter(s -> s.scheduleId() == id).findFirst().orElse(null); }
            @Override public int create(int id, LocalDate date, LocalTime from, LocalTime to, String note, long actor, Connection c) throws SQLException {
                if (databaseFailure) throw new SQLException("fixture failure");
                int generated = shifts.size() + 1;
                shifts.add(new WorkSchedule(generated, id, date, from, to, note, "ACTIVE", "Intern", "Program", false));
                return generated;
            }
            @Override public boolean update(int id, LocalDate date, LocalTime from, LocalTime to, String note, long actor, Connection c) {
                WorkSchedule old = find(id, c); shifts.remove(old);
                shifts.add(new WorkSchedule(id, old.enrollmentId(), date, from, to, note, "ACTIVE", "Intern", "Program", false)); return true;
            }
            @Override public boolean cancel(int id, long actor, Connection c) {
                WorkSchedule old = find(id, c); shifts.remove(old);
                shifts.add(new WorkSchedule(id, old.enrollmentId(), old.workDate(), old.startTime(), old.endTime(), old.note(), "CANCELLED", "Intern", "Program", false)); return true;
            }
            @Override public List<WorkSchedule> list(Long owner, Integer enrollment, Integer program, LocalDate from, LocalDate to, Connection c) {
                filteredOwner = owner; return List.copyOf(shifts);
            }
        };
        LeaveRequestDAO leaves = new LeaveRequestDAO() {
            @Override public LeaveRequest find(int id, Connection c) { return requests.stream().filter(l -> l.leaveId() == id).findFirst().orElse(null); }
            @Override public boolean overlaps(int id, LocalDate from, LocalDate to, int except, Connection c) {
                return requests.stream().anyMatch(l -> l.leaveId() != except && !"REJECTED".equals(l.status())
                        && !l.fromDate().isAfter(to) && !l.toDate().isBefore(from));
            }
            @Override public int create(int id, LocalDate from, LocalDate to, String reason, Connection c) {
                int generated = requests.size() + 1;
                requests.add(new LeaveRequest(generated, id, from, to, reason, "PENDING", null, null, null, Instant.now(), "Intern", "Program")); return generated;
            }
            @Override public boolean decide(int id, boolean approve, long reviewer, String reason, Connection c) {
                LeaveRequest old = find(id, c);
                if (!"PENDING".equals(old.status())) return false;
                requests.remove(old);
                requests.add(new LeaveRequest(id, old.enrollmentId(), old.fromDate(), old.toDate(), old.reason(),
                        approve ? "APPROVED" : "REJECTED", reviewer, Instant.now(), reason, old.createdAt(), "Intern", "Program")); return true;
            }
            @Override public List<LeaveRequest> list(Long owner, Integer enrollment, Integer program, String status, LocalDate from, LocalDate to, Connection c) {
                filteredOwner = owner; return List.copyOf(requests);
            }
        };
        service = new ScheduleLeaveService(contexts, schedules, leaves, connections,
                Clock.fixed(Instant.parse("2026-10-09T16:00:00Z"), ZoneOffset.UTC));
    }
    static User user(long id, String roleName) {
        User user = new User(); user.setUserId(id); user.setStatus(UserStatus.ACTIVE);
        Role role = new Role(); role.setRoleName(roleName); user.setRole(role); return user;
    }
    void error(String code, org.junit.jupiter.api.function.Executable action) {
        assertEquals(code, assertThrows(ScheduleLeaveException.class, action).code());
    }
    @Test void hrCreatesAndCommitsAndInternReadsWithOwnerFilter() throws Exception {
        assertEquals(1, service.createSchedule(hr, 1, day, start, end, " Test "));
        assertEquals("Test", shifts.getFirst().note()); assertEquals(1, connections.commits.get());
        service.schedules(intern, 1, null, day, day); assertEquals(10L, filteredOwner);
    }
    @Test void wrongRoleAndInactiveAccountsCannotWrite() {
        error("FORBIDDEN", () -> service.createSchedule(intern, 1, day, start, end, ""));
        hr.setStatus(UserStatus.LOCKED);
        error("FORBIDDEN", () -> service.createSchedule(hr, 1, day, start, end, ""));
        error("FORBIDDEN", () -> service.requestLeave(user(30, "MENTOR"), 1, day, day, "Reason"));
    }
    @Test void ownershipIsCheckedForBothReadAndLeaveCreation() {
        User other = user(11, "INTERN");
        error("FORBIDDEN", () -> service.schedules(other, 1, null, day, day));
        error("FORBIDDEN", () -> service.leaves(other, 1, null, null, day, day));
        error("FORBIDDEN", () -> service.requestLeave(other, 1, day, day, "Reason"));
    }
    @Test void rejectsUnknownEnrollmentAndInvalidDateOrTimes() {
        error("NOT_FOUND", () -> service.createSchedule(hr, 999, day, start, end, ""));
        error("VALIDATION_ERROR", () -> service.createSchedule(hr, 1, day, end, start, ""));
        error("VALIDATION_ERROR", () -> service.createSchedule(hr, 1, context.startDate().minusDays(1), start, end, ""));
        error("VALIDATION_ERROR", () -> service.createSchedule(hr, 1, day, start, start, ""));
        error("VALIDATION_ERROR", () -> service.createSchedule(hr, 1, day, start.plusNanos(1), end, ""));
    }
    @Test void inactiveEnrollmentAndProgramCannotReceiveSchedulesOrLeave() {
        context = new Context(1, 10L, "COMPLETED", "ACTIVE", day.minusDays(30), day.plusDays(30));
        error("ENROLLMENT_INACTIVE", () -> service.createSchedule(hr, 1, day, start, end, ""));
        error("ENROLLMENT_INACTIVE", () -> service.requestLeave(intern, 1, day, day, "Reason"));
        context = new Context(1, 10L, "ACTIVE", "CANCELLED", day.minusDays(30), day.plusDays(30));
        error("ENROLLMENT_INACTIVE", () -> service.createSchedule(hr, 1, day, start, end, ""));
    }
    @Test void overlappingShiftsFailButAdjacentShiftsAreAllowed() throws Exception {
        service.createSchedule(hr, 1, day, start, end, "");
        error("SCHEDULE_OVERLAP", () -> service.createSchedule(hr, 1, day, start.plusMinutes(30), end, ""));
        assertEquals(2, service.createSchedule(hr, 1, day, end, end.plusHours(1), ""));
        assertEquals(1, connections.rollbacks.get());
    }
    @Test void updateExcludesItselfAndCancellationPreservesHistory() throws Exception {
        service.createSchedule(hr, 1, day, start, end, "");
        service.updateSchedule(hr, 1, day, start, end, "Edited");
        assertEquals("Edited", shifts.getFirst().note());
        service.cancelSchedule(hr, 1); assertEquals("CANCELLED", shifts.getFirst().status());
        error("SCHEDULE_CONFLICT", () -> service.cancelSchedule(hr, 1));
    }
    @Test void startedShiftsAreImmutableInVietnamTimezone() {
        LocalDate today = LocalDate.of(2026, 10, 9);
        shifts.add(new WorkSchedule(1, 1, today, start, end, "", "ACTIVE", "Intern", "Program", false));
        error("SHIFT_ALREADY_STARTED", () -> service.updateSchedule(hr, 1, day, start, end, ""));
        error("SHIFT_ALREADY_STARTED", () -> service.cancelSchedule(hr, 1));
        error("SHIFT_ALREADY_STARTED", () -> service.createSchedule(hr, 1, today, start, end, ""));
    }
    @Test void databaseFailureRollsBackWithoutCommitting() {
        databaseFailure = true;
        assertThrows(SQLException.class, () -> service.createSchedule(hr, 1, day, start, end, ""));
        assertEquals(0, connections.commits.get()); assertEquals(1, connections.rollbacks.get());
    }
    @Test void leaveValidationChecksReasonDatesAndOverlap() throws Exception {
        error("VALIDATION_ERROR", () -> service.requestLeave(intern, 1, day, day, " "));
        error("VALIDATION_ERROR", () -> service.requestLeave(intern, 1, day.plusDays(1), day, "Reason"));
        error("VALIDATION_ERROR", () -> service.requestLeave(intern, 1, day.minusDays(10), day.minusDays(10), "Reason"));
        assertEquals(1, service.requestLeave(intern, 1, day, day.plusDays(1), " Reason "));
        error("LEAVE_OVERLAP", () -> service.requestLeave(intern, 1, day.plusDays(1), day.plusDays(2), "Reason"));
    }
    @Test void approvalIsOneTimeAndReviewedByIsLong() throws Exception {
        service.requestLeave(intern, 1, day, day, "Reason");
        service.decideLeave(user(4_000_000_000L, "HR"), 1, true, null);
        assertEquals("APPROVED", requests.getFirst().status()); assertEquals(4_000_000_000L, requests.getFirst().reviewedBy());
        assertNotNull(requests.getFirst().reviewedAt());
        error("LEAVE_ALREADY_REVIEWED", () -> service.decideLeave(hr, 1, false, "Reason"));
        error("LEAVE_OVERLAP", () -> service.requestLeave(intern, 1, day, day, "Reason"));
    }
    @Test void rejectionRequiresReasonAndRejectedDaysCanBeRequestedAgain() throws Exception {
        service.requestLeave(intern, 1, day, day, "Reason");
        error("VALIDATION_ERROR", () -> service.decideLeave(hr, 1, false, ""));
        service.decideLeave(hr, 1, false, " Not enough notice ");
        assertEquals("Not enough notice", requests.getFirst().rejectReason());
        assertEquals(2, service.requestLeave(intern, 1, day, day, "New reason"));
    }
    @Test void cannotApproveAfterEnrollmentEndsButHistoryRemainsReadable() throws Exception {
        service.requestLeave(intern, 1, day, day, "Reason");
        context = new Context(1, 10L, "COMPLETED", "COMPLETED", day.minusDays(30), day.plusDays(30));
        error("ENROLLMENT_INACTIVE", () -> service.decideLeave(hr, 1, true, null));
        assertEquals(1, service.leaves(intern, 1, null, null, day, day).size());
    }
    @Test void groupCreationReportsEachEnrollmentIncludingFailures() throws Exception {
        List<ScheduleLeaveService.BulkResult> results = service.createSchedules(hr, List.of(1, 999), day, start, end, "");
        assertTrue(results.getFirst().success()); assertFalse(results.getLast().success());
        assertEquals("NOT_FOUND", results.getLast().errorCode());
        assertEquals(1, connections.commits.get());
        error("VALIDATION_ERROR", () -> service.createSchedules(hr, List.of(1, 1), day, start, end, ""));
    }
    @Test void unknownStatusAndExcessiveQueryWindowsAreRejected() {
        error("VALIDATION_ERROR", () -> service.leaves(intern, null, null, "ALL", day, day));
        error("VALIDATION_ERROR", () -> service.schedules(hr, null, null, day, day.plusYears(2)));
    }
    @Test void concurrentRequestsForSameDatesCreateOnlyOneLeave() throws Exception {
        try (var pool = Executors.newFixedThreadPool(2)) {
            var startTogether = new CountDownLatch(1);
            Callable<Boolean> action = () -> { startTogether.await();
                try { service.requestLeave(intern, 1, day, day, "Reason"); return true; }
                catch (ScheduleLeaveException e) { assertEquals("LEAVE_OVERLAP", e.code()); return false; }
            };
            Future<Boolean> first = pool.submit(action), second = pool.submit(action); startTogether.countDown();
            assertNotEquals(first.get(5, TimeUnit.SECONDS), second.get(5, TimeUnit.SECONDS)); assertEquals(1, requests.size());
        }
    }
    @Test void concurrentOverlappingShiftsCreateOnlyOneSchedule() throws Exception {
        try (var pool = Executors.newFixedThreadPool(2)) {
            var startTogether = new CountDownLatch(1);
            Callable<Boolean> action = () -> { startTogether.await();
                try { service.createSchedule(hr, 1, day, start, end, ""); return true; }
                catch (ScheduleLeaveException e) { assertEquals("SCHEDULE_OVERLAP", e.code()); return false; }
            };
            Future<Boolean> first = pool.submit(action), second = pool.submit(action); startTogether.countDown();
            assertNotEquals(first.get(5, TimeUnit.SECONDS), second.get(5, TimeUnit.SECONDS)); assertEquals(1, shifts.size());
        }
    }
}
