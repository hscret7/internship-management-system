package com.internship.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** HR-facing evaluation list item with stable enrollment and evaluator details. */
public record EvaluationSummary(
        long evaluationId,
        int enrollmentId,
        String internName,
        String programName,
        String mentorName,
        BigDecimal score,
        String comment,
        LocalDateTime createdAt,
        Integer internProfileId,
        Integer skillScore,
        Integer attitudeScore) {
    /** Retain source compatibility for older consumers of this list item. */
    public EvaluationSummary(long evaluationId, int enrollmentId, String internName,
                             String programName, String mentorName, BigDecimal score,
                             String comment, LocalDateTime createdAt, Integer internProfileId) {
        this(evaluationId, enrollmentId, internName, programName, mentorName,
                score, comment, createdAt, internProfileId, null, null);
    }
    public EvaluationSummary(long evaluationId, int enrollmentId, String internName,
                             String programName, String mentorName, BigDecimal score,
                             String comment, LocalDateTime createdAt) {
        this(evaluationId, enrollmentId, internName, programName, mentorName,
                score, comment, createdAt, null, null, null);
    }
}
