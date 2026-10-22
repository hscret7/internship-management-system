package com.internship.dao;

import com.internship.model.TaskProgress;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

public class TaskProgressDAO {

    public TaskProgress append(Connection connection, long assignmentId, int completionPercentage, String note)
            throws SQLException {
        String sql = """
                INSERT INTO task_progress (assignment_id, completion_percentage, note)
                VALUES (?, ?, ?)
                """;
        long progressId;
        try (PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setLong(1, assignmentId);
            statement.setInt(2, completionPercentage);
            statement.setString(3, note);
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (!keys.next()) {
                    throw new SQLException("Không lấy được progress_id sau khi cập nhật tiến độ");
                }
                progressId = keys.getLong(1);
            }
        }
        return findById(connection, progressId);
    }

    public Integer findLatestPercentage(Connection connection, long assignmentId) throws SQLException {
        String sql = """
                SELECT completion_percentage FROM task_progress
                WHERE assignment_id = ?
                ORDER BY progress_id DESC LIMIT 1
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, assignmentId);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? resultSet.getInt("completion_percentage") : null;
            }
        }
    }

    public List<TaskProgress> findByAssignment(Connection connection, long assignmentId) throws SQLException {
        String sql = """
                SELECT task_progress.*, UNIX_TIMESTAMP(task_progress.created_at) AS created_at_epoch FROM task_progress
                WHERE assignment_id = ?
                ORDER BY progress_id ASC
                """;
        List<TaskProgress> history = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, assignmentId);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    history.add(map(resultSet));
                }
            }
        }
        return history;
    }

    private TaskProgress findById(Connection connection, long progressId) throws SQLException {
        String sql = "SELECT task_progress.*, UNIX_TIMESTAMP(task_progress.created_at) AS created_at_epoch FROM task_progress WHERE progress_id = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, progressId);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? map(resultSet) : null;
            }
        }
    }

    private TaskProgress map(ResultSet resultSet) throws SQLException {
        TaskProgress progress = new TaskProgress();
        progress.setProgressId(resultSet.getLong("progress_id"));
        progress.setAssignmentId(resultSet.getLong("assignment_id"));
        progress.setNote(resultSet.getString("note"));
        progress.setCompletionPercentage(resultSet.getInt("completion_percentage"));
        java.time.LocalDateTime createdAt = com.internship.util.SqlTime.utcLocal(resultSet, "created_at");
        if (createdAt != null) {
            progress.setCreatedAt(createdAt);
        }
        return progress;
    }
}
