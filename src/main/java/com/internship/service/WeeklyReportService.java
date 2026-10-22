package com.internship.service;

import com.internship.dao.ReportFeedbackDAO;
import com.internship.dao.TaskAccessDAO;
import com.internship.dao.WeeklyReportDAO;
import com.internship.model.ReportFeedback;
import com.internship.model.WeeklyReport;
import com.internship.model.WeeklyReportStatus;
import com.internship.util.DBConnection;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;
import java.util.List;

public class WeeklyReportService {
    private final Be4ConnectionFactory connections;
    private final WeeklyReportDAO reports;
    private final ReportFeedbackDAO feedback;
    private final TaskAccessDAO access;

    public WeeklyReportService() {
        this(DBConnection::getConnection, new WeeklyReportDAO(), new ReportFeedbackDAO(), new TaskAccessDAO());
    }

    WeeklyReportService(Be4ConnectionFactory connections, WeeklyReportDAO reports,
                        ReportFeedbackDAO feedback, TaskAccessDAO access) {
        this.connections = connections;
        this.reports = reports;
        this.feedback = feedback;
        this.access = access;
    }

    public WeeklyReport createDraft(long internUserId, int enrollmentId, short weekNumber, String content)
            throws SQLException {
        requirePositive(internUserId, "internUserId");
        requirePositive(enrollmentId, "enrollmentId");
        if (weekNumber < 1) {
            throw new IllegalArgumentException("Tuần thực tập phải bắt đầu từ 1");
        }
        try (Connection connection = connections.open()) {
            connection.setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);
            connection.setAutoCommit(false);
            try {
                access.lockEnrollmentForMentorWrite(connection, enrollmentId);
                requireInternOwner(connection, internUserId, enrollmentId);
                WeeklyReport created = reports.createDraft(connection, enrollmentId, weekNumber, content);
                connection.commit();
                return created;
            } catch (SQLIntegrityConstraintViolationException exception) {
                rollback(connection, exception);
                if (exception.getErrorCode() == 1062) {
                    throw new IllegalStateException("Tuần này đã có báo cáo", exception);
                }
                throw exception;
            } catch (SQLException | RuntimeException exception) {
                rollback(connection, exception);
                throw exception;
            }
        }
    }

    public WeeklyReport updateDraft(long internUserId, long reportId, String content) throws SQLException {
        requirePositive(internUserId, "internUserId");
        requirePositive(reportId, "reportId");
        try (Connection connection = connections.open()) {
            connection.setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);
            connection.setAutoCommit(false);
            try {
                lockLinkedEnrollment(connection, reportId);
                WeeklyReport report = requireReportForUpdate(connection, reportId);
                requireInternOwner(connection, internUserId, report.getEnrollmentId());
                if (report.getStatus() != WeeklyReportStatus.DRAFT) {
                    throw new IllegalStateException("Chỉ báo cáo nháp mới được sửa");
                }
                reports.updateDraft(connection, reportId, content);
                WeeklyReport updated = reports.findById(connection, reportId);
                connection.commit();
                return updated;
            } catch (SQLException | RuntimeException exception) {
                rollback(connection, exception);
                throw exception;
            }
        }
    }

    public WeeklyReport submit(long internUserId, long reportId) throws SQLException {
        requirePositive(internUserId, "internUserId");
        requirePositive(reportId, "reportId");
        try (Connection connection = connections.open()) {
            connection.setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);
            connection.setAutoCommit(false);
            try {
                lockLinkedEnrollment(connection, reportId);
                WeeklyReport report = requireReportForUpdate(connection, reportId);
                requireInternOwner(connection, internUserId, report.getEnrollmentId());
                if (report.getStatus() != WeeklyReportStatus.DRAFT) {
                    throw new IllegalStateException("Báo cáo đã nộp hoặc đã được duyệt");
                }
                if (report.getContent() == null || report.getContent().isBlank()) {
                    throw new IllegalArgumentException("Báo cáo cần có nội dung trước khi nộp");
                }
                reports.markSubmitted(connection, reportId);
                WeeklyReport submitted = reports.findById(connection, reportId);
                connection.commit();
                return submitted;
            } catch (SQLException | RuntimeException exception) {
                rollback(connection, exception);
                throw exception;
            }
        }
    }

    public ReportFeedback review(long mentorUserId, long reportId, String comment, Integer rating)
            throws SQLException {
        requirePositive(mentorUserId, "mentorUserId");
        requirePositive(reportId, "reportId");
        if (comment == null || comment.isBlank() || comment.strip().length() > 10000) {
            throw new IllegalArgumentException("Cần nhận xét từ 1 đến 10000 ký tự");
        }
        if (rating != null && (rating < 1 || rating > 5)) {
            throw new IllegalArgumentException("Điểm phản hồi phải từ 1 đến 5");
        }
        try (Connection connection = connections.open()) {
            connection.setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);
            connection.setAutoCommit(false);
            try {
                // Read only the stable enrollment link first; lock enrollment before report.
                // The shared lock order matches BE3 mentor handover and avoids a stale check.
                WeeklyReport linkedReport = reports.findById(connection, reportId);
                if (linkedReport == null) throw new IllegalArgumentException("Không tìm thấy báo cáo");
                access.lockEnrollmentForMentorWrite(connection, linkedReport.getEnrollmentId());
                WeeklyReport report = requireReportForUpdate(connection, reportId);
                if (!access.isCurrentMentor(connection, mentorUserId, report.getEnrollmentId())) {
                    throw new SecurityException("Mentor không phụ trách báo cáo này");
                }
                if (report.getStatus() != WeeklyReportStatus.SUBMITTED && report.getStatus() != WeeklyReportStatus.REVIEWED) {
                    throw new IllegalStateException("Chỉ báo cáo đã nộp mới được phản hồi");
                }
                ReportFeedback created = feedback.create(connection, reportId, mentorUserId, comment, rating);
                if (report.getStatus() == WeeklyReportStatus.SUBMITTED) reports.markReviewed(connection, reportId);
                connection.commit();
                return created;
            } catch (SQLException | RuntimeException exception) {
                rollback(connection, exception);
                throw exception;
            }
        }
    }

    public List<WeeklyReport> findForIntern(long internUserId, int enrollmentId) throws SQLException {
        requirePositive(internUserId, "internUserId");
        requirePositive(enrollmentId, "enrollmentId");
        try (Connection connection = connections.open()) {
            if (!access.isInternOwner(connection, internUserId, enrollmentId)) {
                throw new SecurityException("TTS không sở hữu đợt thực tập này");
            }
            return reports.findByEnrollment(connection, enrollmentId);
        }
    }

    public List<WeeklyReport> findForMentor(long mentorUserId, int enrollmentId) throws SQLException {
        requirePositive(mentorUserId, "mentorUserId");
        requirePositive(enrollmentId, "enrollmentId");
        try (Connection connection = connections.open()) {
            if (!access.isAssignedMentor(connection, mentorUserId, enrollmentId)) {
                throw new SecurityException("Mentor không phụ trách đợt thực tập này");
            }
            // Authorization must hold at the server, not just a client-side filter.
            return reports.findByEnrollment(connection, enrollmentId).stream()
                    .filter(report -> report.getStatus() != WeeklyReportStatus.DRAFT).toList();
        }
    }

    public List<ReportFeedback> findFeedback(long actorUserId, long reportId) throws SQLException {
        requirePositive(actorUserId, "actorUserId");
        requirePositive(reportId, "reportId");
        try (Connection connection = connections.open()) {
            WeeklyReport report = reports.findById(connection, reportId);
            if (report == null) {
                throw new IllegalArgumentException("Không tìm thấy báo cáo");
            }
            boolean allowed = access.isInternOwner(connection, actorUserId, report.getEnrollmentId())
                    || access.isAssignedMentor(connection, actorUserId, report.getEnrollmentId());
            if (!allowed) {
                throw new SecurityException("Không có quyền xem phản hồi");
            }
            return feedback.findByReport(connection, reportId);
        }
    }

    private WeeklyReport requireReportForUpdate(Connection connection, long reportId) throws SQLException {
        WeeklyReport report = reports.findByIdForUpdate(connection, reportId);
        if (report == null) {
            throw new IllegalArgumentException("Không tìm thấy báo cáo");
        }
        return report;
    }

    private void lockLinkedEnrollment(Connection connection, long reportId) throws SQLException {
        WeeklyReport linked = reports.findById(connection, reportId);
        if (linked == null) throw new IllegalArgumentException("Không tìm thấy báo cáo");
        access.lockEnrollmentForMentorWrite(connection, linked.getEnrollmentId());
    }

    private void requireInternOwner(Connection connection, long userId, int enrollmentId) throws SQLException {
        if (!access.isActiveInternOwner(connection, userId, enrollmentId)) {
            throw new SecurityException("TTS không sở hữu đợt thực tập ACTIVE này");
        }
    }

    private static void requirePositive(long value, String field) {
        if (value <= 0) {
            throw new IllegalArgumentException(field + " phải lớn hơn 0");
        }
    }

    private static void rollback(Connection connection, Exception original) {
        try {
            connection.rollback();
        } catch (SQLException rollbackFailure) {
            original.addSuppressed(rollbackFailure);
        }
    }
}
