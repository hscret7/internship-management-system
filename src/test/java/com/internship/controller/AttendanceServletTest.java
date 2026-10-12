package com.internship.controller;

import com.internship.model.Allowance;
import com.internship.model.Role;
import com.internship.model.User;
import com.internship.service.AttendanceService;
import com.internship.service.AttendanceService.AttendanceReportRow;
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class AttendanceServletTest {

    private Map<String, String> params = new HashMap<>();
    private Map<String, Object> sessionAttributes = new HashMap<>();
    private Map<String, Object> requestAttributes = new HashMap<>();
    private Map<String, String> headers = new HashMap<>();
    private int status = 200;
    private String contentType;
    private String forwardedPath;
    private StringWriter responseWriter = new StringWriter();

    private HttpServletRequest request;
    private HttpServletResponse response;
    private AttendanceServlet servlet;
    private MockAttendanceService mockService;

    static class MockAttendanceService extends AttendanceService {
        User lastActor;
        Integer lastEnrollmentId;
        Integer lastProgramId;
        LocalDate lastFromDate;
        LocalDate lastToDate;
        String lastStatusFilter;
        List<AttendanceReportRow> returnedRows = new ArrayList<>();

        @Override
        public List<AttendanceReportRow> getReport(User actor, Integer enrollmentId, Integer programId,
                                                   LocalDate fromDate, LocalDate toDate, String statusFilter) throws SQLException {
            this.lastActor = actor;
            this.lastEnrollmentId = enrollmentId;
            this.lastProgramId = programId;
            this.lastFromDate = fromDate;
            this.lastToDate = toDate;
            this.lastStatusFilter = statusFilter;
            return returnedRows;
        }

        @Override
        public List<AttendanceReportRow> getReport(User actor, Integer enrollmentId, Integer programId,
                                                   LocalDate fromDate, LocalDate toDate) throws SQLException {
            return getReport(actor, enrollmentId, programId, fromDate, toDate, null);
        }
    }

    @BeforeEach
    void setUp() throws Exception {
        params.clear();
        sessionAttributes.clear();
        requestAttributes.clear();
        headers.clear();
        status = 200;
        contentType = null;
        forwardedPath = null;
        responseWriter = new StringWriter();

        var session = proxy(HttpSession.class, (p, m, a) -> {
            if ("getAttribute".equals(m.getName())) return sessionAttributes.get(a[0]);
            if ("setAttribute".equals(m.getName())) { sessionAttributes.put((String) a[0], a[1]); return null; }
            return null;
        });

        var dispatcher = proxy(RequestDispatcher.class, (p, m, a) -> {
            if ("forward".equals(m.getName())) return null;
            return null;
        });

        request = proxy(HttpServletRequest.class, (p, m, a) -> switch (m.getName()) {
            case "getSession" -> session;
            case "getServletPath" -> params.getOrDefault("_servletPath", "/hr/attendance");
            case "getParameter" -> params.get(a[0]);
            case "getHeader" -> headers.get(a[0]);
            case "setAttribute" -> { requestAttributes.put((String) a[0], a[1]); yield null; }
            case "getAttribute" -> requestAttributes.get(a[0]);
            case "getRequestDispatcher" -> { forwardedPath = (String) a[0]; yield dispatcher; }
            default -> null;
        });

        response = proxy(HttpServletResponse.class, (p, m, a) -> switch (m.getName()) {
            case "getWriter" -> new PrintWriter(responseWriter);
            case "setStatus" -> { status = (int) a[0]; yield null; }
            case "sendError" -> { status = (int) a[0]; yield null; }
            case "setContentType" -> { contentType = (String) a[0]; yield null; }
            default -> null;
        });

        servlet = new AttendanceServlet();
        mockService = new MockAttendanceService();

        Field serviceField = AttendanceServlet.class.getDeclaredField("attendanceService");
        serviceField.setAccessible(true);
        serviceField.set(servlet, mockService);
    }

    private void login(String roleName) {
        User u = new User();
        u.setUserId(1L);
        u.setFullName("Test User");
        Role r = new Role();
        r.setRoleName(roleName);
        u.setRole(r);
        sessionAttributes.put("currentUser", u);
    }

    @Test
    @DisplayName("HR request with format=json returns JSON array payload")
    void testHrAttendanceJsonFormat() throws Exception {
        login("HR");
        params.put("_servletPath", "/hr/attendance");
        params.put("format", "json");
        params.put("from", "2026-10-01");
        params.put("to", "2026-10-10");
        params.put("status", "PRESENT");

        mockService.returnedRows.add(new AttendanceReportRow(
                12, "Nguyen Van A", "Java Sprint 3",
                LocalDate.of(2026, 10, 5), LocalTime.of(8, 0), LocalTime.of(17, 0),
                LocalDateTime.of(2026, 10, 5, 8, 2), LocalDateTime.of(2026, 10, 5, 17, 1),
                false, "PRESENT", 8.0
        ));

        servlet.doGet(request, response);

        assertEquals(200, status);
        assertTrue(contentType.contains("application/json"));
        String json = responseWriter.toString();
        assertTrue(json.contains("\"enrollmentId\":12"));
        assertTrue(json.contains("\"internName\":\"Nguyen Van A\""));
        assertTrue(json.contains("\"status\":\"PRESENT\""));
        assertEquals("PRESENT", mockService.lastStatusFilter);
        assertNull(forwardedPath, "Direct JSON mode should not forward to JSP");
    }

    @Test
    @DisplayName("HR request with Accept: application/json returns direct JSON")
    void testHrAttendanceAcceptJsonHeader() throws Exception {
        login("HR");
        params.put("_servletPath", "/hr/attendance");
        headers.put("Accept", "application/json, text/javascript");

        servlet.doGet(request, response);

        assertEquals(200, status);
        assertTrue(contentType.contains("application/json"));
        assertEquals("[]", responseWriter.toString().trim());
    }

    @Test
    @DisplayName("HR HTML request forwards to WEB-INF/views/hr/attendance.jsp with report & attendanceJson")
    void testHrAttendanceHtmlForward() throws Exception {
        login("HR");
        params.put("_servletPath", "/hr/attendance");
        params.put("from", "2026-10-01");
        params.put("to", "2026-10-10");
        params.put("enrollmentId", "5");

        servlet.doGet(request, response);

        assertEquals("/WEB-INF/views/hr/attendance.jsp", forwardedPath);
        assertNotNull(requestAttributes.get("report"));
        assertNotNull(requestAttributes.get("attendanceJson"));
        assertEquals(5, requestAttributes.get("enrollmentId"));
        assertEquals(LocalDate.of(2026, 10, 1), requestAttributes.get("from"));
        assertEquals(LocalDate.of(2026, 10, 10), requestAttributes.get("to"));
    }

    @Test
    @DisplayName("Non-HR user accessing /hr/attendance receives 403")
    void testHrAttendanceForbidden() throws Exception {
        login("INTERN");
        params.put("_servletPath", "/hr/attendance");

        servlet.doGet(request, response);

        assertEquals(403, status);
        assertNull(forwardedPath);
    }

    @Test
    @DisplayName("Intern accessing /intern/attendance forwards to WEB-INF/views/intern/attendance.jsp")
    void testInternAttendanceHtmlForward() throws Exception {
        login("INTERN");
        params.put("_servletPath", "/intern/attendance");
        params.put("enrollmentId", "3");

        servlet.doGet(request, response);

        assertEquals("/WEB-INF/views/intern/attendance.jsp", forwardedPath);
        assertNotNull(requestAttributes.get("report"));
        assertEquals(3, requestAttributes.get("enrollmentId"));
    }

    @Test
    @DisplayName("Allowance model internName getter and setter work properly")
    void testAllowanceInternNameProperty() {
        Allowance a = new Allowance();
        a.setInternName("Tran Van B");
        assertEquals("Tran Van B", a.getInternName());
    }

    @SuppressWarnings("unchecked")
    private static <T> T proxy(Class<T> type, InvocationHandler handler) {
        return (T) Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type}, handler);
    }
}
