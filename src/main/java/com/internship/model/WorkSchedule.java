package com.internship.model;

import java.time.LocalDate;
import java.time.LocalTime;

/** Local calendar date/time in Asia/Ho_Chi_Minh; never tied to a mentor assignment. */
public record WorkSchedule(int scheduleId, int enrollmentId, LocalDate workDate,
                           LocalTime startTime, LocalTime endTime, String note, String status,
                           String internName, String programName, boolean approvedLeave) { }
