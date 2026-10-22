package com.internship.service;

import com.internship.dao.DashboardDAO;
import com.internship.dao.EvaluationDAO;
import com.internship.dao.ReportFeedbackDAO;
import com.internship.dao.TaskAccessDAO;
import com.internship.dao.WeeklyReportDAO;
import com.internship.model.Evaluation;
import com.internship.model.ReportFeedback;
import com.internship.model.WeeklyReport;
import com.internship.model.WeeklyReportStatus;

import java.lang.reflect.Proxy;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;
import java.util.Objects;

/** Standalone checks until BE1 adds a JUnit test dependency to develop. */
public class Be4WorkflowChecks {
    private ConnectionSpy connection;
    private FakeReports reports;
    private FakeFeedback feedback;
    private FakeEvaluations evaluations;
    private FakeAccess access;
    private WeeklyReportService reportService;
    private EvaluationService evaluationService;

    public static void main(String[] args) throws Exception {
        Be4WorkflowChecks checks = new Be4WorkflowChecks();
        checks.setUp(); checks.invalidWeekRejected();
        checks.setUp(); checks.internCannotSubmitBlankReport();
        checks.setUp(); checks.internSubmitsDraft();
        checks.setUp(); checks.otherMentorCannotReview();
        checks.setUp(); checks.mentorFeedbackMarksReviewed();
        checks.setUp(); checks.invalidRatingRejected();
        checks.setUp(); checks.invalidEvaluationScoreRejected();
        checks.setUp(); checks.currentMentorCreatesEvaluation();
        checks.setUp(); checks.duplicateEvaluationRejected();
        checks.setUp(); checks.dashboardRateHandlesEmptyAndFraction();
        checks.setUp(); checks.dashboardRejectsUnauthorizedUser();
        System.out.println("BE4 report/evaluation/dashboard checks passed: 11");
    }

    private void setUp() {
        connection = new ConnectionSpy();
        reports = new FakeReports();
        feedback = new FakeFeedback();
        evaluations = new FakeEvaluations();
        access = new FakeAccess();
        reportService = new WeeklyReportService(() -> connection.proxy, reports, feedback, access);
        evaluationService = new EvaluationService(() -> connection.proxy, evaluations, access);
    }

    private void invalidWeekRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> reportService.createDraft(1, 2, (short) 0, "Nội dung"));
    }

    private void internCannotSubmitBlankReport() {
        reports.report.setContent(" ");
        assertThrows(IllegalArgumentException.class, () -> reportService.submit(1, 3));
        assertEquals(WeeklyReportStatus.DRAFT, reports.report.getStatus());
        assertEquals(0, connection.commits);
        assertEquals(1, connection.rollbacks);
    }

    private void internSubmitsDraft() throws Exception {
        WeeklyReport submitted = reportService.submit(1, 3);
        assertEquals(WeeklyReportStatus.SUBMITTED, submitted.getStatus());
        assertEquals(1, connection.commits);
    }

    private void otherMentorCannotReview() {
        reports.report.setStatus(WeeklyReportStatus.SUBMITTED);
        access.currentMentor = false;
        assertThrows(SecurityException.class, () -> reportService.review(4, 3, "Tốt", 5));
        assertEquals(0, feedback.creates);
        assertEquals(1, connection.rollbacks);
    }

    private void mentorFeedbackMarksReviewed() throws Exception {
        reports.report.setStatus(WeeklyReportStatus.SUBMITTED);
        ReportFeedback created = reportService.review(4, 3, "Tốt", null);
        assertEquals(null, created.getRating());
        assertEquals(WeeklyReportStatus.REVIEWED, reports.report.getStatus());
        assertEquals(1, feedback.creates);
        assertEquals(1, connection.commits);
    }

    private void invalidRatingRejected() {
        assertThrows(IllegalArgumentException.class, () -> reportService.review(4, 3, "Tốt", 6));
    }

    private void invalidEvaluationScoreRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> evaluationService.create(4, 2, new BigDecimal("100.01"), null));
        assertThrows(IllegalArgumentException.class,
                () -> evaluationService.create(4, 2, new BigDecimal("9.999"), null));
    }

    private void currentMentorCreatesEvaluation() throws Exception {
        Evaluation evaluation = evaluationService.create(4, 2, new BigDecimal("85.50"), "Đạt");
        assertEquals(2, evaluation.getEnrollmentId());
        assertEquals(7, evaluation.getMentorId());
        assertEquals(new BigDecimal("85.50"), evaluation.getScore());
    }

    private void duplicateEvaluationRejected() {
        evaluations.duplicate = true;
        assertThrows(IllegalStateException.class,
                () -> evaluationService.create(4, 2, new BigDecimal("80"), null));
    }

    private void dashboardRateHandlesEmptyAndFraction() {
        assertEquals(new BigDecimal("0.00"), DashboardService.completionRate(0, 0));
        assertEquals(new BigDecimal("33.33"), DashboardService.completionRate(1, 3));
    }

    private void dashboardRejectsUnauthorizedUser() {
        DashboardService dashboards = new DashboardService(() -> connection.proxy, new DashboardDAO(), access);
        access.hr = false;
        access.activeMentor = false;
        assertThrows(SecurityException.class, () -> dashboards.getHrDashboard(9));
        assertThrows(SecurityException.class, () -> dashboards.getMentorDashboard(9));
    }

    @FunctionalInterface
    private interface CheckedAction {
        void run() throws Exception;
    }

    private static <T extends Throwable> T assertThrows(Class<T> expected, CheckedAction action) {
        try {
            action.run();
        } catch (Throwable actual) {
            if (expected.isInstance(actual)) {
                return expected.cast(actual);
            }
            throw new AssertionError("Expected " + expected.getName() + " but got " + actual, actual);
        }
        throw new AssertionError("Expected " + expected.getName() + " to be thrown");
    }

    private static void assertEquals(Object expected, Object actual) {
        if (!Objects.equals(expected, actual)) {
            throw new AssertionError("Expected " + expected + " but got " + actual);
        }
    }

    private static final class ConnectionSpy {
        int commits;
        int rollbacks;
        final Connection proxy = (Connection) Proxy.newProxyInstance(
                Connection.class.getClassLoader(), new Class<?>[]{Connection.class}, (ignored, method, args) -> {
                    return switch (method.getName()) {
                        case "commit" -> { commits++; yield null; }
                        case "rollback" -> { rollbacks++; yield null; }
                        case "setAutoCommit", "setTransactionIsolation", "close" -> null;
                        case "getAutoCommit" -> false;
                        case "isClosed" -> false;
                        default -> throw new UnsupportedOperationException(method.getName());
                    };
                });
    }

    private static final class FakeReports extends WeeklyReportDAO {
        final WeeklyReport report;

        FakeReports() {
            report = new WeeklyReport();
            report.setReportId(3);
            report.setEnrollmentId(2);
            report.setWeekNumber((short) 1);
            report.setContent("Đã hoàn thành công việc tuần này");
            report.setStatus(WeeklyReportStatus.DRAFT);
        }

        @Override
        public WeeklyReport findByIdForUpdate(Connection connection, long reportId) {
            return report;
        }

        @Override
        public WeeklyReport findById(Connection connection, long reportId) {
            return report;
        }

        @Override
        public void markSubmitted(Connection connection, long reportId) {
            report.setStatus(WeeklyReportStatus.SUBMITTED);
        }

        @Override
        public void markReviewed(Connection connection, long reportId) {
            report.setStatus(WeeklyReportStatus.REVIEWED);
        }
    }

    private static final class FakeFeedback extends ReportFeedbackDAO {
        int creates;

        @Override
        public ReportFeedback create(Connection connection, long reportId, long reviewerUserId,
                                     String comment, Integer rating) {
            creates++;
            ReportFeedback value = new ReportFeedback();
            value.setReportId(reportId);
            value.setReviewerUserId(reviewerUserId);
            value.setComment(comment);
            value.setRating(rating);
            return value;
        }
    }

    private static final class FakeEvaluations extends EvaluationDAO {
        boolean duplicate;

        @Override
        public Evaluation create(Connection connection, int enrollmentId, int mentorId,
                                 BigDecimal score, String comment) throws SQLException {
            if (duplicate) {
                throw new SQLIntegrityConstraintViolationException("Duplicate uq_eval_enrollment", "23000", 1062);
            }
            Evaluation value = new Evaluation();
            value.setEnrollmentId(enrollmentId);
            value.setMentorId(mentorId);
            value.setScore(score);
            value.setComment(comment);
            return value;
        }
    }

    private static final class FakeAccess extends TaskAccessDAO {
        boolean internOwner = true;
        boolean currentMentor = true;
        boolean activeMentor = true;
        boolean hr = true;

        @Override
        public void lockEnrollmentForMentorWrite(Connection connection, int enrollmentId) {
            // Fake DAO: actual locking is covered by the opt-in MySQL regression tests.
        }

        @Override
        public boolean isActiveInternOwner(Connection connection, long userId, int enrollmentId) {
            return internOwner;
        }

        @Override
        public boolean isCurrentMentor(Connection connection, long userId, int enrollmentId) {
            return currentMentor;
        }

        @Override
        public Integer findCurrentMentorId(Connection connection, long userId, int enrollmentId) {
            return currentMentor ? 7 : null;
        }

        @Override
        public boolean isActiveHrOrAdmin(Connection connection, long userId) {
            return hr;
        }

        @Override
        public boolean isActiveMentor(Connection connection, long userId) {
            return activeMentor;
        }
    }
}
