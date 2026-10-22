package com.internship.model;

import java.time.Instant;

public record InAppNotification(long id, String kind, String sourceType, long sourceId,
        int sourceRevision, String title, String body, Instant availableAt, Instant readAt) { }
