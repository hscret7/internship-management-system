package com.internship.controller;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Protect HR Sprint 2 controls during integration.
 * The FE1 intern overview is covered by controller contract tests in Be3ControllerTest,
 * not by exact JSP tags, translated copy, date formatting or layout assertions.
 */
class Be3HrViewsRegressionTest {
    @Test
    void bothHrViewsKeepOneUtf8SharedLayout() throws Exception {
        for (String name : new String[]{"catalog.jsp", "enrollments.jsp"}) {
            String source = view(name);
            assertTrue(source.contains("contentType=\"text/html; charset=UTF-8\""), name);
            assertTrue(source.contains("pageEncoding=\"UTF-8\""), name);
            assertEquals(1, occurrences(source, "/WEB-INF/views/common/header.jsp"), name);
            assertEquals(1, occurrences(source, "/WEB-INF/views/common/footer.jsp"), name);
            assertTrue(source.contains("value=\"fe3.css\""), name);
            assertTrue(source.contains("fe3-be3__table-scroll"), name);
            assertFalse(source.contains("<!DOCTYPE html>"), name);
            for (String marker : new String[]{"<<<<<<<", "=======", ">>>>>>>"}) {
                assertFalse(source.contains(marker), name + " contains merge markers");
            }
        }
    }

    @Test
    void catalogRetainsEditLockFilteringAndMentorWorkload() throws Exception {
        String source = view("catalog.jsp");
        for (String control : new String[]{"selectedDepartmentId", "editingProgram", "programStructureLocked",
                "value=\"update-program\"", "action=edit-program&programId=", "value=\"activate-program\"",
                "mentor.status", "mentor.remainingCapacity", "mentor.atCapacity"}) {
            assertTrue(source.contains(control), "Missing Sprint 2 control: " + control);
        }
        for (String field : new String[]{"departmentId", "startDate", "endDate", "maxInterns"}) {
            assertTrue(source.contains("type=\"hidden\" name=\"" + field + "\""),
                    "Locked edit form must still submit " + field);
        }
        assertEquals(2, occurrences(source, "fe3-be3__table-scroll"));
    }

    @Test
    void enrollmentRetainsInitialMentorAndHandoverControls() throws Exception {
        String source = view("enrollments.jsp");
        for (String control : new String[]{"value=\"enroll\"", "name=\"applicationId\"", "name=\"programId\"",
                "name=\"mentorId\"", "Phân mentor sau", "value=\"assign-mentor\"", "value=\"change-mentor\"",
                "name=\"newMentorId\"", "name=\"endReason\" maxlength=\"200\"", "mentorHistory",
                "enrollment.status == 'ACTIVE'", "Chưa có mentor cùng phòng ban còn chỗ",
                "Chưa có mentor khác cùng phòng ban còn chỗ"}) {
            assertTrue(source.contains(control), "Missing enrollment control: " + control);
        }
    }

    @Test
    void allHrPostFormsKeepCsrfTokensAndExplicitActions() throws Exception {
        Pattern forms = Pattern.compile("<form\\b[^>]*method=\"post\"[^>]*>(.*?)</form>", Pattern.DOTALL);
        int total = 0;
        for (String name : new String[]{"catalog.jsp", "enrollments.jsp"}) {
            var matcher = forms.matcher(view(name));
            while (matcher.find()) {
                total++;
                assertTrue(matcher.group(1).contains("name=\"csrfToken\" value=\"${csrfToken}\""), name);
                assertTrue(matcher.group(1).contains("name=\"action\""), name);
            }
        }
        assertEquals(8, total);
    }

    private static String view(String name) throws Exception {
        return Files.readString(Path.of("src/main/webapp/WEB-INF/views/hr", name), StandardCharsets.UTF_8);
    }

    private static int occurrences(String text, String fragment) {
        int count = 0;
        for (int position = 0; (position = text.indexOf(fragment, position)) >= 0; position += fragment.length()) count++;
        return count;
    }
}
