package com.internship.controller;

import com.internship.model.*;
import org.junit.jupiter.api.Test;
import java.time.*;
import static org.junit.jupiter.api.Assertions.*;

class Be4TimeContractTest {
    @Test void metadataUsesExplicitInstantsAndNullsNotZoneLessStrings() {
        LocalDateTime utc=LocalDateTime.of(2026,10,12,2,0);
        WeeklyReport report=new WeeklyReport();report.setStatus(WeeklyReportStatus.DRAFT);report.setCreatedAt(utc);
        String json=Be4Json.stringify(Be4Json.report(report));assertTrue(json.contains("\"createdAt\":\"2026-10-12T02:00:00Z\""));assertTrue(json.contains("\"submittedAt\":null"));
        TaskProgress progress=new TaskProgress();progress.setCreatedAt(utc);assertEquals(Instant.parse("2026-10-12T02:00:00Z"),Be4Json.progress(progress).get("createdAt"));
        ReportFeedback feedback=new ReportFeedback();feedback.setCreatedAt(utc);assertEquals(Instant.parse("2026-10-12T02:00:00Z"),Be4Json.feedback(feedback).get("createdAt"));
        Evaluation evaluation=new Evaluation();evaluation.setCreatedAt(utc);assertEquals(Instant.parse("2026-10-12T02:00:00Z"),Be4Json.evaluation(evaluation).get("createdAt"));
    }
}
