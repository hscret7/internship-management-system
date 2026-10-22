package com.internship.service;

import com.internship.dao.*;
import com.internship.model.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.lang.reflect.Proxy;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** Fast transaction/authorization tests, always run by Maven without MySQL. */
class Be4MentorWriteTransactionTest {
    enum Operation { TASK, FEEDBACK, EVALUATION }

    @ParameterizedTest
    @EnumSource(Operation.class)
    void locksEnrollmentBeforeAuthorizationAndCommitsAfterWriting(Operation operation) throws Exception {
        Fixture fixture = new Fixture();
        assertNotNull(fixture.run(operation));
        assertTrue(fixture.events.indexOf("lock-enrollment") < fixture.events.indexOf("check-mentor"));
        assertTrue(fixture.events.indexOf("check-mentor") < fixture.events.indexOf("write"));
        assertTrue(fixture.events.indexOf("write") < fixture.events.indexOf("commit"));
        assertFalse(fixture.events.contains("rollback"));
        if (operation == Operation.FEEDBACK) {
            assertTrue(fixture.events.indexOf("lock-enrollment") < fixture.events.indexOf("lock-report"));
        }
    }

    @ParameterizedTest
    @EnumSource(Operation.class)
    void unauthorizedMentorRollsBackWithoutWriting(Operation operation) {
        Fixture fixture = new Fixture();
        fixture.allowed = false;
        assertThrows(SecurityException.class, () -> fixture.run(operation));
        assertFalse(fixture.events.contains("write"));
        assertFalse(fixture.events.contains("commit"));
        assertEquals(1, fixture.events.stream().filter("rollback"::equals).count());
    }

    @ParameterizedTest
    @EnumSource(Operation.class)
    void databaseFailureRollsBackInsteadOfCommitting(Operation operation) {
        Fixture fixture = new Fixture();
        fixture.failWrite = true;
        assertThrows(SQLException.class, () -> fixture.run(operation));
        assertFalse(fixture.events.contains("commit"));
        assertEquals(1, fixture.events.stream().filter("rollback"::equals).count());
    }

    private static class Fixture {
        final List<String> events = new ArrayList<>();
        boolean allowed = true;
        boolean failWrite;
        boolean autoCommit = true;
        int isolation = Connection.TRANSACTION_REPEATABLE_READ;
        final Connection connection = (Connection) Proxy.newProxyInstance(
                Connection.class.getClassLoader(), new Class<?>[]{Connection.class}, (proxy, method, arguments) -> {
                    return switch (method.getName()) {
                        case "setTransactionIsolation" -> { isolation = (int) arguments[0]; yield null; }
                        case "getTransactionIsolation" -> isolation;
                        case "setAutoCommit" -> { autoCommit = (boolean) arguments[0]; yield null; }
                        case "getAutoCommit" -> autoCommit;
                        case "commit", "rollback" -> { events.add(method.getName()); yield null; }
                        case "close" -> null;
                        default -> throw new UnsupportedOperationException(method.getName());
                    };
                });
        final TaskAccessDAO access = new TaskAccessDAO() {
            @Override public void lockEnrollmentForMentorWrite(Connection ignored, int enrollmentId) {
                assertEquals(7, enrollmentId);
                assertFalse(autoCommit, "Lock must be inside the write transaction");
                assertEquals(Connection.TRANSACTION_READ_COMMITTED, isolation);
                events.add("lock-enrollment");
            }
            @Override public boolean isCurrentMentor(Connection ignored, long userId, int enrollmentId) {
                events.add("check-mentor");
                return allowed;
            }
            @Override public Integer findCurrentMentorId(Connection ignored, long userId, int enrollmentId) {
                events.add("check-mentor");
                return allowed ? 3 : null;
            }
        };

        Object run(Operation operation) throws SQLException {
            return switch (operation) {
                case TASK -> new TaskService(() -> connection, new TaskDAO() {
                    @Override public Task findById(Connection ignored, long taskId) {
                        Task task = new Task();
                        task.setCreatedByUserId(20);
                        task.setStatus(TaskStatus.OPEN);
                        return task;
                    }
                }, new TaskAssignmentDAO() {
                    @Override public TaskAssignment create(Connection ignored, long taskId, int enrollmentId)
                            throws SQLException {
                        write();
                        return new TaskAssignment();
                    }
                }, new TaskProgressDAO(), access).assignTask(20, 1, 7);
                case FEEDBACK -> new WeeklyReportService(() -> connection, new WeeklyReportDAO() {
                    @Override public WeeklyReport findById(Connection ignored, long reportId) {
                        events.add("read-linked-report");
                        return report();
                    }
                    @Override public WeeklyReport findByIdForUpdate(Connection ignored, long reportId) {
                        events.add("lock-report");
                        return report();
                    }
                    @Override public void markReviewed(Connection ignored, long reportId) { }
                }, new ReportFeedbackDAO() {
                    @Override public ReportFeedback create(Connection ignored, long reportId, long reviewer,
                            String comment, Integer rating) throws SQLException {
                        write();
                        return new ReportFeedback();
                    }
                }, access).review(20, 1, "Nhận xét", 4);
                case EVALUATION -> new EvaluationService(() -> connection, new EvaluationDAO() {
                    @Override public Evaluation create(Connection ignored, int enrollmentId, int mentorId,
                            BigDecimal score, String comment) throws SQLException {
                        write();
                        return new Evaluation();
                    }
                }, access).create(20, 7, BigDecimal.TEN, "Đánh giá");
            };
        }

        private WeeklyReport report() {
            WeeklyReport report = new WeeklyReport();
            report.setEnrollmentId(7);
            report.setStatus(WeeklyReportStatus.SUBMITTED);
            return report;
        }

        private void write() throws SQLException {
            events.add("write");
            if (failWrite) throw new SQLException("Simulated write failure");
        }
    }
}
