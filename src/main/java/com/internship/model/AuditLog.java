package com.internship.model;

import java.time.Instant;

public record AuditLog(long id, Long actorUserId, String action, String objectType,
                       Long objectId, Instant occurredAt, String result, String actorEmail, String actorName) {
    public AuditLog(long id, Long actorUserId, String action, String objectType,
                    Long objectId, Instant occurredAt, String result) {
        this(id, actorUserId, action, objectType, objectId, occurredAt, result, null, null);
    }
}
