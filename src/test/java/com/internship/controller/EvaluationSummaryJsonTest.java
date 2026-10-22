package com.internship.controller;

import com.internship.model.EvaluationSummary;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import static org.junit.jupiter.api.Assertions.*;

class EvaluationSummaryJsonTest {
    @Test void responseIncludesStableProfileIdentityAndExistingFields() {
        var summary = new EvaluationSummary(7, 8, "Nguyễn Văn An", "Program", "Mentor",
                BigDecimal.TEN, "Good", null, 42);
        var response = Be4Json.evaluationSummary(summary);
        assertEquals(42, response.get("internProfileId"));
        assertEquals(8, response.get("enrollmentId"));
        assertEquals("Nguyễn Văn An", response.get("internName"));
    }

    @Test void legacyConstructorRemainsCompatibleWithoutInventingAnIdentity() {
        var summary = new EvaluationSummary(7, 8, "Intern", "Program", "Mentor",
                BigDecimal.TEN, "Good", null);
        assertNull(summary.internProfileId());
        assertTrue(Be4Json.evaluationSummary(summary).containsKey("internProfileId"));
    }
}
