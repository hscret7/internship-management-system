package com.internship.service;

import java.time.Instant;
import java.util.Set;

/** Trusted server-side event; recipients and source identifiers must be authorized by its caller. */
public record NotificationEvent(String key, String kind, String sourceType, long sourceId,
        int sourceRevision, String title, String body, Instant availableAt) {
    public NotificationEvent {
        if (key == null || !key.matches("[A-Za-z0-9:._-]{1,160}")
                || !Set.of("CREATE", "CHANGE", "CANCEL", "REMINDER", "TASK", "DEADLINE", "FEEDBACK").contains(kind == null ? "" : kind)
                || !Set.of("MEETING", "TASK", "REPORT", "EVALUATION").contains(sourceType == null ? "" : sourceType)
                || sourceId <= 0 || sourceRevision < 1 || title == null || title.isBlank()
                || title.length() > 200 || body == null || body.length() > 10000 || availableAt == null)
            throw new IllegalArgumentException("Invalid notification event");
    }
}
