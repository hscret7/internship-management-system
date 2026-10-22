package com.internship.service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;

/** Server-side input validation; participant user IDs/email are resolved from enrollments. */
public record MeetingRequest(int programId, String title, String description, String location,
                             Instant startsAt, Instant endsAt, List<Integer> enrollmentIds) {
    public MeetingRequest {
        if (programId <= 0) throw new IllegalArgumentException("Invalid program");
        title = required(title, 200);
        location = required(location, 500);
        description = description == null ? null : description.trim();
        if (description != null && description.length() > 10000)
            throw new IllegalArgumentException("Description too long");
        if (startsAt == null || endsAt == null || !startsAt.isBefore(endsAt))
            throw new IllegalArgumentException("Invalid meeting time");
        // MySQL TIMESTAMP supports this portable range; preserve microsecond precision.
        Instant min = Instant.parse("1970-01-01T00:00:01Z");
        Instant max = Instant.parse("2038-01-19T03:14:07Z");
        startsAt = startsAt.truncatedTo(ChronoUnit.MICROS);
        endsAt = endsAt.truncatedTo(ChronoUnit.MICROS);
        if (startsAt.isBefore(min) || endsAt.isAfter(max) || !startsAt.isBefore(endsAt))
            throw new IllegalArgumentException("Time outside supported range");
        if (enrollmentIds == null || enrollmentIds.isEmpty() || enrollmentIds.size() > 200
                || enrollmentIds.stream().anyMatch(id -> id == null || id <= 0)
                || new HashSet<>(enrollmentIds).size() != enrollmentIds.size())
            throw new IllegalArgumentException("Invalid participants");
        enrollmentIds = enrollmentIds.stream().sorted().toList();
    }
    private static String required(String value, int max) {
        if (value == null || value.trim().isEmpty() || value.trim().length() > max)
            throw new IllegalArgumentException("Invalid meeting text");
        return value.trim();
    }
}
