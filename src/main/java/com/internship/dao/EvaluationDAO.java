package com.internship.dao;

import com.internship.model.Evaluation;
import com.internship.model.EvaluationSummary;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

public class EvaluationDAO {

    public Evaluation create(Connection connection, int enrollmentId, int mentorId,
                             BigDecimal score, String comment) throws SQLException {
        String sql = """
                INSERT INTO evaluations (enrollment_id, mentor_id, score, comment)
                VALUES (?, ?, ?, ?)
                """;
        long evaluationId;
        try (PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setInt(1, enrollmentId);
            statement.setInt(2, mentorId);
            statement.setBigDecimal(3, score);
            statement.setString(4, comment);
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (!keys.next()) {
                    throw new SQLException("Không lấy được evaluation_id");
                }
                evaluationId = keys.getLong(1);
            }
        }
        return findById(connection, evaluationId);
    }

    public Evaluation findByEnrollment(Connection connection, int enrollmentId) throws SQLException {
        // Compatibility endpoint returns only the legacy score /100, never a /5 criteria row.
        String sql = "SELECT evaluations.*, UNIX_TIMESTAMP(evaluations.created_at) AS created_at_epoch, UNIX_TIMESTAMP(evaluations.updated_at) AS updated_at_epoch FROM evaluations WHERE enrollment_id = ? AND score IS NOT NULL";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, enrollmentId);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? map(resultSet) : null;
            }
        }
    }

    public List<EvaluationSummary> findAllSummaries(Connection connection) throws SQLException {
        String sql = """
                SELECT e.evaluation_id, e.enrollment_id, ip.profile_id AS intern_profile_id,
                       ip.full_name AS intern_name,
                       p.program_name, u.full_name AS mentor_name, e.score,
                       e.skill_score, e.attitude_score,
                       e.comment, e.created_at, UNIX_TIMESTAMP(e.created_at) AS created_at_epoch
                FROM evaluations e
                JOIN program_enrollments pe ON pe.enrollment_id = e.enrollment_id
                JOIN intern_profiles ip ON ip.profile_id = pe.intern_profile_id
                JOIN internship_programs p ON p.program_id = pe.program_id
                JOIN mentors m ON m.mentor_id = e.mentor_id
                JOIN users u ON u.user_id = m.user_id
                ORDER BY e.created_at DESC, e.evaluation_id DESC
                """;
        List<EvaluationSummary> summaries = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet rows = statement.executeQuery()) {
            while (rows.next()) {
                java.time.LocalDateTime createdAt = com.internship.util.SqlTime.utcLocal(rows, "created_at");
                Integer skillScore = rows.getObject("skill_score") != null ? rows.getInt("skill_score") : null;
                Integer attitudeScore = rows.getObject("attitude_score") != null ? rows.getInt("attitude_score") : null;
                BigDecimal score = rows.getBigDecimal("score");
                summaries.add(new EvaluationSummary(
                        rows.getLong("evaluation_id"), rows.getInt("enrollment_id"),
                        rows.getString("intern_name"), rows.getString("program_name"),
                        rows.getString("mentor_name"), score,
                        rows.getString("comment"),
                        createdAt == null ? null : createdAt,
                        rows.getInt("intern_profile_id"),
                        skillScore,
                        attitudeScore));
            }
        }
        return summaries;
    }

    private Evaluation findById(Connection connection, long evaluationId) throws SQLException {
        String sql = "SELECT evaluations.*, UNIX_TIMESTAMP(evaluations.created_at) AS created_at_epoch, UNIX_TIMESTAMP(evaluations.updated_at) AS updated_at_epoch FROM evaluations WHERE evaluation_id = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, evaluationId);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? map(resultSet) : null;
            }
        }
    }

    private Evaluation map(ResultSet resultSet) throws SQLException {
        Evaluation evaluation = new Evaluation();
        evaluation.setEvaluationId(resultSet.getLong("evaluation_id"));
        evaluation.setEnrollmentId(resultSet.getInt("enrollment_id"));
        evaluation.setMentorId(resultSet.getInt("mentor_id"));
        evaluation.setScore(resultSet.getBigDecimal("score"));
        evaluation.setComment(resultSet.getString("comment"));
        java.time.LocalDateTime createdAt = com.internship.util.SqlTime.utcLocal(resultSet, "created_at");
        if (createdAt != null) {
            evaluation.setCreatedAt(createdAt);
        }
        java.time.LocalDateTime updatedAt = com.internship.util.SqlTime.utcLocal(resultSet, "updated_at");
        if (updatedAt != null) {
            evaluation.setUpdatedAt(updatedAt);
        }
        return evaluation;
    }
}
