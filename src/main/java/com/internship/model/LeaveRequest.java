package com.internship.model;

import java.time.Instant;
import java.time.LocalDate;

public record LeaveRequest(int leaveId, int enrollmentId, LocalDate fromDate, LocalDate toDate,
                           String reason, String status, Long reviewedBy, Instant reviewedAt,
                           String rejectReason, Instant createdAt, String internName,
                           String programName) { }
