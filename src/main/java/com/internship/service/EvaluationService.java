package com.internship.service;

import com.internship.dao.EvaluationDAO;
import com.internship.dao.TaskAccessDAO;
import com.internship.model.Evaluation;
import com.internship.model.EvaluationSummary;
import com.internship.util.DBConnection;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;
import java.util.List;

public class EvaluationService {
    private final Be4ConnectionFactory connections;
    private final EvaluationDAO evaluations;
    private final TaskAccessDAO access;

    public EvaluationService() {
        this(DBConnection::getConnection, new EvaluationDAO(), new TaskAccessDAO());
    }

    EvaluationService(Be4ConnectionFactory connections, EvaluationDAO evaluations, TaskAccessDAO access) {
        this.connections = connections;
        this.evaluations = evaluations;
        this.access = access;
    }

    /** Final evaluation is created by the current mentor before HR completes the enrollment. */
    public Evaluation create(long mentorUserId, int enrollmentId, BigDecimal score, String comment)
            throws SQLException {
        requirePositive(mentorUserId, "mentorUserId");
        requirePositive(enrollmentId, "enrollmentId");
        if (score == null || score.compareTo(BigDecimal.ZERO) < 0
                || score.compareTo(new BigDecimal("100")) > 0) {
            throw new IllegalArgumentException("Điểm tổng kết phải từ 0 đến 100");
        }
        BigDecimal normalized;
        try {
            normalized = score.setScale(2, RoundingMode.UNNECESSARY);
        } catch (ArithmeticException exception) {
            throw new IllegalArgumentException("Điểm tổng kết tối đa 2 chữ số thập phân", exception);
        }
        try (Connection connection = connections.open()) {
            connection.setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);
            connection.setAutoCommit(false);
            try {
                access.lockEnrollmentForMentorWrite(connection, enrollmentId);
                Integer mentorId = access.findCurrentMentorId(connection, mentorUserId, enrollmentId);
                if (mentorId == null) {
                    throw new SecurityException("Chỉ mentor hiện tại của enrollment ACTIVE được đánh giá");
                }
                Evaluation created;
                try {
                    created = evaluations.create(connection, enrollmentId, mentorId, normalized, comment);
                } catch (SQLIntegrityConstraintViolationException exception) {
                    if (exception.getErrorCode() == 1062) {
                        throw new IllegalStateException("Đợt thực tập này đã có đánh giá tổng kết", exception);
                    }
                    throw exception;
                }
                connection.commit();
                return created;
            } catch (SQLException | RuntimeException exception) {
                try { connection.rollback(); }
                catch (SQLException rollbackFailure) { exception.addSuppressed(rollbackFailure); }
                throw exception;
            }
        }
    }

    public Evaluation findForActor(long actorUserId, int enrollmentId) throws SQLException {
        requirePositive(actorUserId, "actorUserId");
        requirePositive(enrollmentId, "enrollmentId");
        try (Connection connection = connections.open()) {
            boolean allowed = access.isInternOwner(connection, actorUserId, enrollmentId)
                    || access.isAssignedMentor(connection, actorUserId, enrollmentId)
                    || access.isActiveHrOrAdmin(connection, actorUserId);
            if (!allowed) {
                throw new SecurityException("Không có quyền xem đánh giá này");
            }
            return evaluations.findByEnrollment(connection, enrollmentId);
        }
    }

    public List<EvaluationSummary> findAllForHr(long hrUserId) throws SQLException {
        requirePositive(hrUserId, "hrUserId");
        try (Connection connection = connections.open()) {
            if (!access.isActiveHrOrAdmin(connection, hrUserId)) {
                throw new SecurityException("Chỉ HR/Admin được xem danh sách đánh giá");
            }
            return evaluations.findAllSummaries(connection);
        }
    }

    private static void requirePositive(long value, String field) {
        if (value <= 0) {
            throw new IllegalArgumentException(field + " phải lớn hơn 0");
        }
    }
}
