package com.internship.dao;

import com.internship.model.WeeklyReport;
import com.internship.model.WeeklyReportStatus;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

public class WeeklyReportDAO {

    public WeeklyReport createDraft(Connection connection, int enrollmentId, short weekNumber, String content)
            throws SQLException {
        String sql = """
                INSERT INTO weekly_reports (enrollment_id, week_number, content, status)
                VALUES (?, ?, ?, 'DRAFT')
                """;
        long reportId;
        try (PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setInt(1, enrollmentId);
            statement.setShort(2, weekNumber);
            statement.setString(3, content);
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (!keys.next()) {
                    throw new SQLException("Không lấy được report_id");
                }
                reportId = keys.getLong(1);
            }
        }
        return findById(connection, reportId);
    }

    public WeeklyReport findById(Connection connection, long reportId) throws SQLException {
        return find(connection, reportId, false);
    }

    public WeeklyReport findByIdForUpdate(Connection connection, long reportId) throws SQLException {
        return find(connection, reportId, true);
    }

    private WeeklyReport find(Connection connection, long reportId, boolean lock) throws SQLException {
        String sql = "SELECT weekly_reports.*, UNIX_TIMESTAMP(weekly_reports.submitted_at) AS submitted_at_epoch, UNIX_TIMESTAMP(weekly_reports.created_at) AS created_at_epoch, UNIX_TIMESTAMP(weekly_reports.updated_at) AS updated_at_epoch FROM weekly_reports WHERE report_id = ?" + (lock ? " FOR UPDATE" : "");
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, reportId);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? map(resultSet) : null;
            }
        }
    }

    public List<WeeklyReport> findByEnrollment(Connection connection, int enrollmentId) throws SQLException {
        String sql = """
                SELECT weekly_reports.*, UNIX_TIMESTAMP(weekly_reports.submitted_at) AS submitted_at_epoch, UNIX_TIMESTAMP(weekly_reports.created_at) AS created_at_epoch, UNIX_TIMESTAMP(weekly_reports.updated_at) AS updated_at_epoch FROM weekly_reports WHERE enrollment_id = ?
                ORDER BY week_number ASC, report_id ASC
                """;
        List<WeeklyReport> reports = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, enrollmentId);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    reports.add(map(resultSet));
                }
            }
        }
        return reports;
    }

    public void updateDraft(Connection connection, long reportId, String content) throws SQLException {
        updateOne(connection, "UPDATE weekly_reports SET content = ? WHERE report_id = ? AND status = 'DRAFT'",
                content, reportId);
    }

    public void markSubmitted(Connection connection, long reportId) throws SQLException {
        updateOne(connection, """
                UPDATE weekly_reports SET status = 'SUBMITTED', submitted_at = CURRENT_TIMESTAMP
                WHERE report_id = ? AND status = 'DRAFT'
                """, reportId);
    }

    public void markReviewed(Connection connection, long reportId) throws SQLException {
        updateOne(connection, """
                UPDATE weekly_reports SET status = 'REVIEWED'
                WHERE report_id = ? AND status = 'SUBMITTED'
                """, reportId);
    }

    private void updateOne(Connection connection, String sql, Object... parameters) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            for (int index = 0; index < parameters.length; index++) {
                statement.setObject(index + 1, parameters[index]);
            }
            if (statement.executeUpdate() != 1) {
                throw new SQLException("Báo cáo đã đổi trạng thái hoặc không còn tồn tại");
            }
        }
    }

    private WeeklyReport map(ResultSet resultSet) throws SQLException {
        WeeklyReport report = new WeeklyReport();
        report.setReportId(resultSet.getLong("report_id"));
        report.setEnrollmentId(resultSet.getInt("enrollment_id"));
        report.setWeekNumber(resultSet.getShort("week_number"));
        report.setContent(resultSet.getString("content"));
        report.setStatus(WeeklyReportStatus.valueOf(resultSet.getString("status")));
        java.time.LocalDateTime submittedAt = com.internship.util.SqlTime.utcLocal(resultSet, "submitted_at");
        if (submittedAt != null) {
            report.setSubmittedAt(submittedAt);
        }
        java.time.LocalDateTime createdAt = com.internship.util.SqlTime.utcLocal(resultSet, "created_at");
        if (createdAt != null) {
            report.setCreatedAt(createdAt);
        }
        java.time.LocalDateTime updatedAt = com.internship.util.SqlTime.utcLocal(resultSet, "updated_at");
        if (updatedAt != null) {
            report.setUpdatedAt(updatedAt);
        }
        return report;
    }
}
