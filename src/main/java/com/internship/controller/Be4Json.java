package com.internship.controller;

import com.internship.model.DashboardCount;
import com.internship.model.Evaluation;
import com.internship.model.EvaluationSummary;
import com.internship.model.EnrollmentSummary;
import com.internship.model.HrDashboard;
import com.internship.model.MentorDashboard;
import com.internship.model.ReportFeedback;
import com.internship.model.Task;
import com.internship.model.TaskAssignment;
import com.internship.model.TaskProgress;
import com.internship.model.WeeklyReport;

import java.util.LinkedHashMap;
import java.util.Map;

/** Small explicit JSON encoder for BE4 response DTOs; never serializes User/password fields. */
final class Be4Json {
    private Be4Json() {
    }

    static Map<String, Object> fields(Object... pairs) {
        Map<String, Object> values = new LinkedHashMap<>();
        for (int index = 0; index < pairs.length; index += 2) {
            values.put((String) pairs[index], pairs[index + 1]);
        }
        return values;
    }

    static Map<String, Object> task(Task value) {
        return fields("taskId", value.getTaskId(), "title", value.getTitle(),
                "description", value.getDescription(), "status", value.getStatus().name(),
                "dueDate", value.getDueDate(), "createdByUserId", value.getCreatedByUserId());
    }

    static Map<String, Object> enrollment(EnrollmentSummary value) {
        return fields("enrollmentId", value.enrollmentId(), "internName", value.internName(),
                "programName", value.programName(), "status", value.status(),
                "startDate", value.startDate(), "endDate", value.endDate());
    }

    static Map<String, Object> assignment(TaskAssignment value) {
        return fields("assignmentId", value.getAssignmentId(), "taskId", value.getTaskId(),
                "enrollmentId", value.getEnrollmentId(), "status", value.getStatus().name(),
                "assignedAt", utc(value.getAssignedAt()), "updatedAt", utc(value.getUpdatedAt()));
    }

    static Map<String, Object> progress(TaskProgress value) {
        return fields("progressId", value.getProgressId(), "assignmentId", value.getAssignmentId(),
                "completionPercentage", value.getCompletionPercentage(), "note", value.getNote(),
                "createdAt", utc(value.getCreatedAt()));
    }

    static Map<String, Object> report(WeeklyReport value) {
        return fields("reportId", value.getReportId(), "enrollmentId", value.getEnrollmentId(),
                "weekNumber", value.getWeekNumber(), "content", value.getContent(),
                "status", value.getStatus().name(), "submittedAt", utc(value.getSubmittedAt()),
                "createdAt", utc(value.getCreatedAt()), "updatedAt", utc(value.getUpdatedAt()));
    }

    static Map<String, Object> feedback(ReportFeedback value) {
        return fields("feedbackId", value.getFeedbackId(), "reportId", value.getReportId(),
                "reviewerUserId", value.getReviewerUserId(), "comment", value.getComment(),
                "rating", value.getRating(), "createdAt", utc(value.getCreatedAt()));
    }

    static Map<String, Object> evaluation(Evaluation value) {
        return fields("evaluationId", value.getEvaluationId(), "enrollmentId", value.getEnrollmentId(),
                "mentorId", value.getMentorId(), "score", value.getScore(),
                "comment", value.getComment(), "createdAt", utc(value.getCreatedAt()),
                "updatedAt", utc(value.getUpdatedAt()));
    }

    static Map<String, Object> evaluationSummary(EvaluationSummary value) {
        return fields("evaluationId", value.evaluationId(),
                "enrollmentId", value.enrollmentId(),
                "internProfileId", value.internProfileId(),
                "internName", value.internName(),
                "programName", value.programName(),
                "mentorName", value.mentorName(),
                "score", value.score(),
                "skillScore", value.skillScore(),
                "attitudeScore", value.attitudeScore(),
                "comment", value.comment(),
                "createdAt", utc(value.createdAt()));
    }

    static Map<String, Object> nullableEvaluation(Evaluation value) {
        return value == null ? null : evaluation(value);
    }

    static Map<String, Object> dashboardCount(DashboardCount value) {
        return fields("label", value.label(), "count", value.count());
    }

    static Map<String, Object> hrDashboard(HrDashboard value) {
        return fields("totalInterns", value.totalInterns(),
                "activeEnrollments", value.activeEnrollments(),
                "completedEnrollments", value.completedEnrollments(),
                "completionRatePercent", value.completionRatePercent(),
                "internsByUniversity", value.internsByUniversity().stream().map(Be4Json::dashboardCount).toList(),
                "internsByMajor", value.internsByMajor().stream().map(Be4Json::dashboardCount).toList(),
                "activeInternsByMentor", value.activeInternsByMentor().stream().map(Be4Json::dashboardCount).toList());
    }

    static Map<String, Object> mentorDashboard(MentorDashboard value) {
        return fields("activeInterns", value.activeInterns(), "tasksTodo", value.tasksTodo(),
                "tasksInProgress", value.tasksInProgress(), "tasksDone", value.tasksDone(),
                "reportsSubmitted", value.reportsSubmitted(), "reportsReviewed", value.reportsReviewed(),
                "evaluationsCompleted", value.evaluationsCompleted());
    }

    // Legacy LocalDateTime model getters now carry UTC components from SQL epoch projections.
    private static java.time.Instant utc(java.time.LocalDateTime value) {
        return value == null ? null : value.toInstant(java.time.ZoneOffset.UTC);
    }

    static String stringify(Object value) {
        if (value == null) {
            return "null";
        }
        if (value instanceof String text) {
            return quote(text);
        }
        if (value instanceof Number || value instanceof Boolean) {
            return value.toString();
        }
        if (value instanceof java.time.temporal.TemporalAccessor) {
            return quote(value.toString());
        }
        if (value instanceof Map<?, ?> map) {
            StringBuilder output = new StringBuilder("{");
            boolean first = true;
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                if (!first) output.append(',');
                first = false;
                output.append(quote((String) entry.getKey())).append(':').append(stringify(entry.getValue()));
            }
            return output.append('}').toString();
        }
        if (value instanceof Iterable<?> items) {
            StringBuilder output = new StringBuilder("[");
            boolean first = true;
            for (Object item : items) {
                if (!first) output.append(',');
                first = false;
                output.append(stringify(item));
            }
            return output.append(']').toString();
        }
        throw new IllegalArgumentException("Unsupported JSON response type: " + value.getClass().getName());
    }

    private static String quote(String value) {
        StringBuilder output = new StringBuilder("\"");
        for (int index = 0; index < value.length(); index++) {
            char character = value.charAt(index);
            switch (character) {
                case '"' -> output.append("\\\"");
                case '\\' -> output.append("\\\\");
                case '\b' -> output.append("\\b");
                case '\f' -> output.append("\\f");
                case '\n' -> output.append("\\n");
                case '\r' -> output.append("\\r");
                case '\t' -> output.append("\\t");
                case '<' -> output.append("\\u003c");
                case '>' -> output.append("\\u003e");
                case '&' -> output.append("\\u0026");
                default -> {
                    if (character < 0x20) {
                        output.append(String.format("\\u%04x", (int) character));
                    } else {
                        output.append(character);
                    }
                }
            }
        }
        return output.append('"').toString();
    }
}
