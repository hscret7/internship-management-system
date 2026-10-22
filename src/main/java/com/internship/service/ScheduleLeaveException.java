package com.internship.service;

/** Stable error codes for FE1/FE2; messages never include SQL/configuration details. */
public class ScheduleLeaveException extends RuntimeException {
    private final int status;
    private final String code;
    public ScheduleLeaveException(int status, String code, String message) {
        super(message); this.status = status; this.code = code;
    }
    public int status() { return status; }
    public String code() { return code; }
}
