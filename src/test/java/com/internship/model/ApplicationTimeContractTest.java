package com.internship.model;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.ResourceLock;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.TimeZone;

import static org.junit.jupiter.api.Assertions.*;

@ResourceLock("java.util.TimeZone.default")
class ApplicationTimeContractTest {
    @ParameterizedTest
    @ValueSource(strings = {"UTC", "Asia/Ho_Chi_Minh", "America/New_York"})
    void isoInstantsDoNotDependOnJvmZoneAndLegacyGettersRemainDates(String zone) {
        TimeZone original = TimeZone.getDefault();
        try {
            TimeZone.setDefault(TimeZone.getTimeZone(zone));
            Instant submitted = Instant.parse("2026-10-12T02:00:00.123456Z");
            Instant reviewed = Instant.parse("2026-10-12T03:30:00.654321Z");
            Application application = new Application();
            application.setSubmittedAt(Timestamp.from(submitted));
            application.setReviewedAt(Timestamp.from(reviewed));

            assertInstanceOf(java.util.Date.class, application.getSubmittedAt());
            assertInstanceOf(java.util.Date.class, application.getReviewedAt());
            assertEquals(submitted, application.getSubmittedAt().toInstant());
            assertEquals(reviewed, application.getReviewedAt().toInstant());
            assertEquals(submitted.toString(), application.getSubmittedAtIso());
            assertEquals(reviewed.toString(), application.getReviewedAtIso());
            assertEquals(submitted, Instant.parse(application.getSubmittedAtIso()));
            assertEquals(reviewed, Instant.parse(application.getReviewedAtIso()));
            assertTrue(application.toString().contains("submittedAt=" + submitted));
        } finally {
            TimeZone.setDefault(original);
        }
    }

    @Test void unreviewedAndAbsentDatesRemainNull() {
        Application application = new Application();
        assertNull(application.getSubmittedAt());
        assertNull(application.getSubmittedAtIso());
        assertNull(application.getReviewedAt());
        assertNull(application.getReviewedAtIso());
    }

    @Test void isoGettersReflectCurrentValuesInsteadOfCachingOrInventingReviewTime() {
        Application application = new Application();
        application.setSubmittedAt(Timestamp.from(Instant.parse("2026-10-12T23:59:59Z")));
        application.setReviewedAt(Timestamp.from(Instant.parse("2026-10-13T00:00:00Z")));
        assertEquals("2026-10-12T23:59:59Z", application.getSubmittedAtIso());
        assertEquals("2026-10-13T00:00:00Z", application.getReviewedAtIso());
        application.setSubmittedAt(null);
        application.setReviewedAt(null);
        assertNull(application.getSubmittedAtIso());
        assertNull(application.getReviewedAtIso());
    }
}
