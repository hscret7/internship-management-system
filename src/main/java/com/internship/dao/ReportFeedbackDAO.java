package com.internship.dao;

import com.internship.model.ReportFeedback;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

public class ReportFeedbackDAO {

    public ReportFeedback create(Connection connection, long reportId, long reviewerUserId,
                                 String comment, Integer rating) throws SQLException {
        String sql = """
                INSERT INTO report_feedbacks (report_id, reviewer_user_id, comment, rating)
                VALUES (?, ?, ?, ?)
                """;
        long feedbackId;
        try (PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setLong(1, reportId);
            statement.setLong(2, reviewerUserId);
            statement.setString(3, comment);
            if (rating == null) {
                statement.setNull(4, Types.TINYINT);
            } else {
                statement.setInt(4, rating);
            }
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (!keys.next()) {
                    throw new SQLException("Không lấy được feedback_id");
                }
                feedbackId = keys.getLong(1);
            }
        }
        return findById(connection, feedbackId);
    }

    public ReportFeedback findById(Connection connection, long feedbackId) throws SQLException {
        String sql = "SELECT report_feedbacks.*, UNIX_TIMESTAMP(report_feedbacks.created_at) AS created_at_epoch FROM report_feedbacks WHERE feedback_id = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, feedbackId);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? map(resultSet) : null;
            }
        }
    }

    public List<ReportFeedback> findByReport(Connection connection, long reportId) throws SQLException {
        String sql = """
                SELECT report_feedbacks.*, UNIX_TIMESTAMP(report_feedbacks.created_at) AS created_at_epoch FROM report_feedbacks WHERE report_id = ?
                ORDER BY created_at ASC, feedback_id ASC
                """;
        List<ReportFeedback> feedback = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, reportId);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    feedback.add(map(resultSet));
                }
            }
        }
        return feedback;
    }

    private ReportFeedback map(ResultSet resultSet) throws SQLException {
        ReportFeedback feedback = new ReportFeedback();
        feedback.setFeedbackId(resultSet.getLong("feedback_id"));
        feedback.setReportId(resultSet.getLong("report_id"));
        feedback.setReviewerUserId(resultSet.getLong("reviewer_user_id"));
        feedback.setComment(resultSet.getString("comment"));
        int rating = resultSet.getInt("rating");
        feedback.setRating(resultSet.wasNull() ? null : rating);
        java.time.LocalDateTime createdAt = com.internship.util.SqlTime.utcLocal(resultSet, "created_at");
        if (createdAt != null) {
            feedback.setCreatedAt(createdAt);
        }
        return feedback;
    }
}
