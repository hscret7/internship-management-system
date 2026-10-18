package com.internship.controller;

import com.internship.dao.*;
import com.internship.model.*;
import com.internship.service.*;
import com.internship.util.CsrfToken;
import jakarta.servlet.http.*;
import org.junit.jupiter.api.*;
import java.io.*;
import java.lang.reflect.*;
import java.sql.SQLException;
import java.time.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class ScheduleLeaveServletTest {
    final Map<String, Object> attributes = new HashMap<>();
    final Map<String, String> parameters = new HashMap<>();
    String route = "/api/intern/work-schedules";
    int status;
    boolean called;
    StringWriter body;
    ScheduleLeaveServlet servlet;
    HttpServletRequest request;
    HttpServletResponse response;

    @BeforeEach void setup() {
        body = new StringWriter(); status = 200;
        ScheduleLeaveService service = new ScheduleLeaveService() {
            @Override public List<WorkSchedule> schedules(User actor, Integer enrollment, Integer program, LocalDate from, LocalDate to) {
                called = true;
                return List.of(new WorkSchedule(5, 1, from, LocalTime.of(9, 0), LocalTime.of(12, 0),
                        "<script>\"test\"</script>", "ACTIVE", "Intern", "Program", true));
            }
            @Override public int requestLeave(User actor, int enrollment, LocalDate from, LocalDate to, String reason) { called = true; return 6; }
            @Override public List<BulkResult> createSchedules(User actor, List<Integer> ids, LocalDate date, LocalTime from, LocalTime to, String note) {
                called = true; return List.of(new BulkResult(1, 5, true, null), new BulkResult(999, null, false, "NOT_FOUND"));
            }
        };
        servlet = new ScheduleLeaveServlet(service);
        HttpSession session = proxy(HttpSession.class, (p, m, a) -> switch (m.getName()) {
            case "getAttribute" -> attributes.get(a[0]);
            case "setAttribute" -> { attributes.put((String) a[0], a[1]); yield null; }
            default -> null;
        });
        request = proxy(HttpServletRequest.class, (p, m, a) -> switch (m.getName()) {
            case "getSession" -> session;
            case "getServletPath" -> route;
            case "getParameter" -> parameters.get(a[0]);
            default -> null;
        });
        response = proxy(HttpServletResponse.class, (p, m, a) -> switch (m.getName()) {
            case "getWriter" -> new PrintWriter(body);
            case "setStatus" -> { status = (int) a[0]; yield null; }
            default -> null;
        });
        parameters.put("from", "2026-10-01"); parameters.put("to", "2026-10-31");
    }
    @SuppressWarnings("unchecked") static <T> T proxy(Class<T> type, InvocationHandler handler) {
        return (T) Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type}, handler);
    }
    void login(String name) {
        User actor = new User(); actor.setUserId(10L); actor.setStatus(UserStatus.ACTIVE);
        Role role = new Role(); role.setRoleName(name); actor.setRole(role); attributes.put("currentUser", actor);
    }
    @Test void anonymousGets401AndNoData() throws Exception {
        servlet.doGet(request, response); assertEquals(401, status); assertFalse(called);
        assertTrue(body.toString().contains("UNAUTHENTICATED"));
    }
    @Test void roleMismatchGets403() throws Exception {
        login("INTERN"); route = "/api/hr/work-schedules";
        servlet.doGet(request, response); assertEquals(403, status); assertFalse(called);
    }
    @Test void getReturnsCsrfTimezoneAndApprovedLeaveMarkerWithSafeJson() throws Exception {
        login("INTERN"); servlet.doGet(request, response);
        assertEquals(200, status); assertTrue(called);
        String json = body.toString(); assertTrue(json.contains("csrfToken"));
        assertTrue(json.contains("Asia/Ho_Chi_Minh")); assertTrue(json.contains("\"approvedLeave\":true"));
        assertFalse(json.contains("<script>")); assertFalse(json.contains("password"));
    }
    @Test void missingCsrfCannotWrite() throws Exception {
        login("INTERN"); route = "/api/intern/leave-requests"; parameters.put("action", "create");
        servlet.doPost(request, response); assertEquals(403, status); assertFalse(called);
    }
    @Test void validCsrfAllowsInternLeaveAndReturns201() throws Exception {
        login("INTERN"); route = "/api/intern/leave-requests";
        parameters.put("csrfToken", CsrfToken.expose(request)); parameters.put("action", "create");
        parameters.put("enrollmentId", "1"); parameters.put("fromDate", "2026-10-12");
        parameters.put("toDate", "2026-10-12"); parameters.put("reason", "Reason");
        servlet.doPost(request, response); assertEquals(201, status); assertTrue(called);
        assertTrue(body.toString().contains("\"leaveId\":6"));
    }
    @Test void invalidDateAndMissingParametersReturn400() throws Exception {
        login("INTERN"); parameters.put("from", "invalid");
        servlet.doGet(request, response); assertEquals(400, status); assertFalse(called);
        parameters.remove("from"); servlet.doGet(request, response); assertEquals(400, status);
    }
    @Test void partialGroupSaveCannotReportCompleteSuccess() throws Exception {
        login("HR"); route = "/api/hr/work-schedules";
        parameters.put("csrfToken", CsrfToken.expose(request)); parameters.put("action", "bulk-create");
        parameters.put("enrollmentIds", "1,999"); parameters.put("workDate", "2026-10-12");
        parameters.put("startTime", "09:00"); parameters.put("endTime", "12:00");
        servlet.doPost(request, response); assertEquals(207, status);
        assertTrue(body.toString().contains("\"allSucceeded\":false"));
        assertTrue(body.toString().contains("\"failureCount\":1"));
    }
    @Test void internCannotMutateWorkScheduleEvenWithCsrf() throws Exception {
        login("INTERN"); parameters.put("csrfToken", CsrfToken.expose(request)); parameters.put("action", "cancel");
        servlet.doPost(request, response); assertEquals(403, status); assertFalse(called);
    }
    @Test void lookupReturnsAllChoicesWithoutRequiringDateFilters() throws Exception {
        servlet = new ScheduleLeaveServlet(new ScheduleLeaveService(), new EnrollmentLookupService() {
            @Override public List<ScheduleEnrollmentDAO.Choice> lookup(User actor, String email) {
                assertEquals("intern@example.com", email);
                return List.of(new ScheduleEnrollmentDAO.Choice(1, 10, "A", "ACTIVE", "ACTIVE", "Name", email),
                        new ScheduleEnrollmentDAO.Choice(2, 20, "B", "ACTIVE", "ACTIVE", "Name", email));
            }
        });
        login("HR"); route="/api/hr/work-schedules"; parameters.clear();
        parameters.put("action", "enrollments");parameters.put("email", "intern@example.com");
        servlet.doGet(request,response);assertEquals(200,status);
        assertTrue(body.toString().contains("\"enrollmentId\":1"));
        assertTrue(body.toString().contains("\"enrollmentId\":2"));
    }
    @Test void lookupDatabaseFailureReturns503NotEmptySuccess() throws Exception {
        servlet = new ScheduleLeaveServlet(new ScheduleLeaveService(), new EnrollmentLookupService() {
            @Override public List<ScheduleEnrollmentDAO.Choice> lookup(User actor, String email) throws SQLException {
                throw new SQLException("private-database-detail");
            }
        });
        login("HR");route="/api/hr/work-schedules";parameters.put("action","enrollments");
        servlet.doGet(request,response);assertEquals(503,status);
        assertTrue(body.toString().contains("DATABASE_ERROR"));
        assertFalse(body.toString().contains("private-database-detail"));
    }
    @Test void internCannotUseHrLookupRoute() throws Exception {
        login("INTERN");route="/api/hr/work-schedules";parameters.put("action","enrollments");
        servlet.doGet(request,response);assertEquals(403,status);
    }

    @Test void lookupUnknownEmailReturnsEmptyList() throws Exception {
        servlet = new ScheduleLeaveServlet(new ScheduleLeaveService(), new EnrollmentLookupService() {
            @Override public List<ScheduleEnrollmentDAO.Choice> lookup(User actor, String email) { return List.of(); }
        });
        login("HR");route="/api/hr/work-schedules";parameters.put("action","enrollments");
        servlet.doGet(request,response);assertEquals(200,status);
        assertTrue(body.toString().contains("\"data\":[]"));
    }
    @Test void internCannotLookupOtherEmailOnOwnRoute() throws Exception {
        login("INTERN");parameters.put("action","enrollments");parameters.put("email","other@example.com");
        servlet.doGet(request,response);assertEquals(403,status);
    }

}
