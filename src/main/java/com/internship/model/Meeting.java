package com.internship.model;

import java.time.Instant;
import java.util.List;

public record Meeting(long id, int programId, long organizerUserId, String title,
                      String description, String location, Instant startsAt, Instant endsAt,
                      String status, int revision, List<Integer> enrollmentIds) {
    public Meeting { enrollmentIds = List.copyOf(enrollmentIds); }
}
