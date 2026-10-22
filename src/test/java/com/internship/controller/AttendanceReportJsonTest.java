package com.internship.controller;

import com.internship.service.AttendanceService.AttendanceReportRow;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Payload nay la hop dong ma FE2 dua vao (handoff: "lay mang JSON bao cao chuyen can").
 * DB QA hien khong co dong work_schedules/attendances nao nen khong the kiem chung
 * tuan tu hoa bang du lieu song - do la ly do test nay ton tai.
 */
class AttendanceReportJsonTest {

    private static AttendanceReportRow row() {
        AttendanceReportRow r = new AttendanceReportRow();
        r.enrollmentId = 7;
        r.internName = "Trần Văn B";
        r.programName = "Java Backend";
        r.workDate = LocalDate.of(2026, 10, 12);
        r.startTime = LocalTime.of(8, 30);
        r.endTime = LocalTime.of(17, 30);
        r.status = "PRESENT";
        return r;
    }

    @Test void emptyReportIsAnEmptyJsonArray() {
        assertEquals("[]", AttendanceServlet.toJson(List.of()));
    }

    @Test void fullRowMatchesTheDocumentedContract() {
        AttendanceReportRow r = row();
        r.checkInTime = LocalDateTime.of(2026, 10, 12, 8, 31);
        r.checkOutTime = LocalDateTime.of(2026, 10, 12, 17, 29);
        r.approvedLeave = false;
        r.workedHours = 8.97;

        assertEquals(
                "[{\"enrollmentId\":7"
              + ",\"internName\":\"Trần Văn B\""
              + ",\"programName\":\"Java Backend\""
              + ",\"workDate\":\"2026-10-12\""
              + ",\"startTime\":\"08:30\""
              + ",\"endTime\":\"17:30\""
              + ",\"checkInTime\":\"2026-10-12T08:31\""
              + ",\"checkOutTime\":\"2026-10-12T17:29\""
              + ",\"approvedLeave\":false"
              + ",\"status\":\"PRESENT\""
              + ",\"workedHours\":8.97}]",
                AttendanceServlet.toJson(List.of(r)));
    }

    @Test void vietnameseTextIsKeptAsIsNotEscapedIntoEntities() {
        String json = AttendanceServlet.toJson(List.of(row()));
        assertTrue(json.contains("Trần Văn B"), json);
        assertFalse(json.contains("\\u1ea7"), "khong duoc escape chu co dau thanh \\uXXXX");
    }

    @Test void missingTimesBecomeJsonNullNotTheStringNull() {
        AttendanceReportRow r = row();
        r.startTime = null;
        r.endTime = null;
        r.checkInTime = null;
        r.checkOutTime = null;
        r.status = "ABSENT";
        r.workedHours = 0.0;

        String json = AttendanceServlet.toJson(List.of(r));
        assertTrue(json.contains("\"startTime\":null"), json);
        assertTrue(json.contains("\"endTime\":null"), json);
        assertTrue(json.contains("\"checkInTime\":null"), json);
        assertTrue(json.contains("\"checkOutTime\":null"), json);
        assertFalse(json.contains("\"null\""), "null phai la literal JSON, khong phai chuoi");
    }

    @Test void quoteBackslashAndControlCharsAreEscaped() {
        AttendanceReportRow r = row();
        r.internName = "Nhóm \"A\" \\ B\nC\tD";
        r.workedHours = 0.0;

        String json = AttendanceServlet.toJson(List.of(r));
        assertTrue(json.contains("Nhóm \\\"A\\\" \\\\ B\\nC\\tD"), json);
    }

    /**
     * Quan trong: cung chuoi nay con duoc nhung vao the &lt;script&gt; cua trang HTML.
     * Neu '&lt;' khong bi escape thi ten TTS chua "&lt;/script&gt;" se dong the som va pha
     * ca trang - day la loi XSS.
     */
    @Test void scriptCloseTagInDataCannotBreakOutOfTheHtmlScriptElement() {
        AttendanceReportRow r = row();
        r.internName = "</script><script>alert(1)</script>";
        r.workedHours = 0.0;

        String json = AttendanceServlet.toJson(List.of(r));
        assertFalse(json.contains("</script>"), json);
        assertFalse(json.contains("<"), "moi '<' phai thanh \\u003c: " + json);
        assertTrue(json.contains("\\u003c/script>"), json);
    }

    @Test void nanOrNegativeHoursSerializeAsZeroBecauseJsonHasNoNan() {
        AttendanceReportRow nan = row();
        nan.workedHours = Double.NaN;
        AttendanceReportRow negative = row();
        negative.workedHours = -1.5;

        assertTrue(AttendanceServlet.toJson(List.of(nan)).contains("\"workedHours\":0.0"));
        assertTrue(AttendanceServlet.toJson(List.of(negative)).contains("\"workedHours\":0.0"));
    }

    @Test void multipleRowsAreCommaSeparated() {
        AttendanceReportRow a = row();
        a.workedHours = 1.0;
        AttendanceReportRow b = row();
        b.enrollmentId = 9;
        b.workedHours = 2.0;

        String json = AttendanceServlet.toJson(List.of(a, b));
        assertEquals(2, json.split("\\{\"enrollmentId\"", -1).length - 1, json);
        assertTrue(json.contains("},{\"enrollmentId\":9"), json);
    }
}
