package com.internship.controller;

import com.internship.model.Allowance;
import com.internship.model.Role;
import com.internship.model.SupportRequest;
import com.internship.model.User;
import com.internship.model.UserStatus;
import com.internship.service.AllowanceService;
import com.internship.service.AttendanceService;
import com.internship.service.AttendanceService.AttendanceReportRow;
import com.internship.service.SupportRequestService;
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class Be2ServletTest {

    private Map<String, Object> sessionAttributes;
    private Map<String, Object> requestAttributes;
    private Map<String, String> requestParameters;
    private String forwardedPath;
    private int responseStatus;
    private StringWriter responseBody;

    private HttpSession sessionProxy;
    private HttpServletRequest requestProxy;
    private HttpServletResponse responseProxy;
    private RequestDispatcher dispatcherProxy;

    private String servletPath;

    @BeforeEach
    void setUp() {
        sessionAttributes = new HashMap<>();
        requestAttributes = new HashMap<>();
        requestParameters = new HashMap<>();
        forwardedPath = null;
        responseStatus = 200;
        responseBody = new StringWriter();

        dispatcherProxy = proxy(RequestDispatcher.class, (p, m, a) -> null);

        sessionProxy = proxy(HttpSession.class, (p, m, a) -> switch (m.getName()) {
            case "getAttribute" -> sessionAttributes.get(a[0]);
            case "setAttribute" -> { sessionAttributes.put((String) a[0], a[1]); yield null; }
            default -> null;
        });

        requestProxy = proxy(HttpServletRequest.class, (p, m, a) -> switch (m.getName()) {
            case "getSession" -> sessionProxy;
            case "getServletPath" -> servletPath;
            case "getParameter" -> requestParameters.get(a[0]);
            case "getAttribute" -> requestAttributes.get(a[0]);
            case "setAttribute" -> { requestAttributes.put((String) a[0], a[1]); yield null; }
            case "getRequestDispatcher" -> {
                forwardedPath = (String) a[0];
                yield dispatcherProxy;
            }
            case "getContextPath" -> "";
            case "getHeader" -> null;
            default -> null;
        });

        responseProxy = proxy(HttpServletResponse.class, (p, m, a) -> switch (m.getName()) {
            case "setStatus" -> { responseStatus = (int) a[0]; yield null; }
            case "sendError" -> { responseStatus = (int) a[0]; yield null; }
            case "getWriter" -> new PrintWriter(responseBody);
            case "setContentType", "setCharacterEncoding", "setHeader" -> null;
            case "sendRedirect" -> null;
            default -> null;
        });
    }

    @SuppressWarnings("unchecked")
    private static <T> T proxy(Class<T> type, InvocationHandler handler) {
        return (T) Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type}, handler);
    }

    private User createTestUser(long id, String roleName) {
        User u = new User();
        u.setUserId(id);
        u.setStatus(UserStatus.ACTIVE);
        Role r = new Role();
        r.setRoleName(roleName);
        u.setRole(r);
        return u;
    }

    // ==========================================
    // US21 / US22 AttendanceServlet tests
    // ==========================================

    @Test
    @DisplayName("Intern attendance doGet rejects non-intern with 403")
    void testInternAttendanceGetForbiddenForNonIntern() throws Exception {
        sessionAttributes.put("currentUser", createTestUser(1L, "HR"));
        servletPath = "/intern/attendance";

        AttendanceServlet servlet = new AttendanceServlet(new StubAttendanceService());
        servlet.doGet(requestProxy, responseProxy);

        assertEquals(403, responseStatus);
        assertNull(forwardedPath);
    }

    @Test
    @DisplayName("Intern attendance doGet populates report, from, to, enrollmentId and forwards to intern view")
    void testInternAttendanceGetSuccess() throws Exception {
        sessionAttributes.put("currentUser", createTestUser(2L, "INTERN"));
        servletPath = "/intern/attendance";
        requestParameters.put("enrollmentId", "15");
        requestParameters.put("from", "2026-10-01");
        requestParameters.put("to", "2026-10-12");

        StubAttendanceService stubService = new StubAttendanceService();
        AttendanceServlet servlet = new AttendanceServlet(stubService,
                new com.internship.service.EnrollmentLookupService() {
                    @Override public List<com.internship.dao.ScheduleEnrollmentDAO.Choice> lookup(User actor, String email) {
                        assertEquals(2L, actor.getUserId());
                        assertNull(email);
                        return List.of(new com.internship.dao.ScheduleEnrollmentDAO.Choice(
                                15, 1, "Program", "ACTIVE", "ACTIVE", "Intern", "intern@example.com"));
                    }
                });
        servlet.doGet(requestProxy, responseProxy);

        assertEquals(200, responseStatus);
        assertEquals("/WEB-INF/views/intern/attendance.jsp", forwardedPath);
        assertNotNull(requestAttributes.get("report"));
        assertEquals(LocalDate.of(2026, 10, 1), requestAttributes.get("from"));
        assertEquals(LocalDate.of(2026, 10, 12), requestAttributes.get("to"));
        assertEquals(15, requestAttributes.get("enrollmentId"));
    }

    @Test
    @DisplayName("HR attendance doGet filters by status, populates attributes and forwards to HR view")
    void testHrAttendanceGetSuccess() throws Exception {
        sessionAttributes.put("currentUser", createTestUser(3L, "HR"));
        servletPath = "/hr/attendance";
        requestParameters.put("enrollmentId", "10");
        requestParameters.put("status", "PRESENT");

        StubAttendanceService stubService = new StubAttendanceService();
        AttendanceServlet servlet = new AttendanceServlet(stubService);
        servlet.doGet(requestProxy, responseProxy);

        assertEquals(200, responseStatus);
        assertEquals("/WEB-INF/views/hr/attendance.jsp", forwardedPath);
        assertNotNull(requestAttributes.get("report"));
        assertNotNull(requestAttributes.get("attendanceJson"));
        assertEquals(10, requestAttributes.get("enrollmentId"));
        assertEquals("PRESENT", requestAttributes.get("status"));
    }

    @Test
    @DisplayName("Attendance doPost rejects missing or invalid CSRF token with 403")
    void testAttendancePostRejectsInvalidCsrf() throws Exception {
        sessionAttributes.put("currentUser", createTestUser(2L, "INTERN"));
        servletPath = "/intern/attendance";
        // no csrf token in request parameters or header

        AttendanceServlet servlet = new AttendanceServlet(new StubAttendanceService());
        servlet.doPost(requestProxy, responseProxy);

        assertEquals(403, responseStatus);
    }

    // ==========================================
    // US26 AllowanceServlet tests & Allowance Model
    // ==========================================

    @Test
    @DisplayName("Allowance model supports internName getter and setter")
    void testAllowanceInternNameProperty() {
        Allowance allowance = new Allowance();
        allowance.setAllowanceId(1);
        allowance.setInternName("Nguyễn Văn A");
        allowance.setAmount(BigDecimal.valueOf(1500000));
        allowance.setStatus("PENDING");

        assertEquals("Nguyễn Văn A", allowance.getInternName());
        assertEquals(1, allowance.getAllowanceId());
        assertEquals("PENDING", allowance.getStatus());
        assertEquals(BigDecimal.valueOf(1500000), allowance.getAmount());
    }

    @Test
    @DisplayName("Intern allowances doGet forwards to intern/allowances.jsp")
    void testInternAllowancesGet() throws Exception {
        sessionAttributes.put("currentUser", createTestUser(2L, "INTERN"));
        servletPath = "/intern/allowances";

        StubAllowanceService stubService = new StubAllowanceService();
        AllowanceServlet servlet = new AllowanceServlet(stubService);
        servlet.doGet(requestProxy, responseProxy);

        assertEquals(200, responseStatus);
        assertEquals("/WEB-INF/views/intern/allowances.jsp", forwardedPath);
        assertNotNull(requestAttributes.get("allowances"));
    }

    @Test
    @DisplayName("HR allowances doGet forwards to hr/allowances.jsp")
    void testHrAllowancesGet() throws Exception {
        sessionAttributes.put("currentUser", createTestUser(1L, "HR"));
        servletPath = "/hr/allowances";

        StubAllowanceService stubService = new StubAllowanceService();
        AllowanceServlet servlet = new AllowanceServlet(stubService);
        servlet.doGet(requestProxy, responseProxy);

        assertEquals(200, responseStatus);
        assertEquals("/WEB-INF/views/hr/allowances.jsp", forwardedPath);
        assertNotNull(requestAttributes.get("allowances"));
    }

    // ==========================================
    // US27 SupportRequestServlet tests
    // ==========================================

    @Test
    @DisplayName("Intern support doGet forwards to intern/support.jsp")
    void testInternSupportGet() throws Exception {
        sessionAttributes.put("currentUser", createTestUser(2L, "INTERN"));
        servletPath = "/intern/support";

        StubSupportRequestService stubService = new StubSupportRequestService();
        SupportRequestServlet servlet = new SupportRequestServlet(stubService);
        servlet.doGet(requestProxy, responseProxy);

        assertEquals(200, responseStatus);
        assertEquals("/WEB-INF/views/intern/support.jsp", forwardedPath);
        assertNotNull(requestAttributes.get("requests"));
    }

    @Test
    @DisplayName("Intern support doGet rejects non-intern with 403")
    void testInternSupportForbiddenForNonIntern() throws Exception {
        sessionAttributes.put("currentUser", createTestUser(1L, "HR"));
        servletPath = "/intern/support";

        StubSupportRequestService stubService = new StubSupportRequestService();
        SupportRequestServlet servlet = new SupportRequestServlet(stubService);
        servlet.doGet(requestProxy, responseProxy);

        assertEquals(403, responseStatus);
    }

    @Test
    @DisplayName("HR support doGet forwards to hr/support.jsp")
    void testHrSupportGet() throws Exception {
        sessionAttributes.put("currentUser", createTestUser(1L, "HR"));
        servletPath = "/hr/support";

        StubSupportRequestService stubService = new StubSupportRequestService();
        SupportRequestServlet servlet = new SupportRequestServlet(stubService);
        servlet.doGet(requestProxy, responseProxy);

        assertEquals(200, responseStatus);
        assertEquals("/WEB-INF/views/hr/support.jsp", forwardedPath);
        assertNotNull(requestAttributes.get("requests"));
    }

    @Test
    @DisplayName("SupportRequest doPost rejects invalid CSRF token with 403")
    void testSupportRequestPostRejectsInvalidCsrf() throws Exception {
        sessionAttributes.put("currentUser", createTestUser(2L, "INTERN"));
        servletPath = "/intern/support";

        SupportRequestServlet servlet = new SupportRequestServlet(new StubSupportRequestService());
        servlet.doPost(requestProxy, responseProxy);

        assertEquals(403, responseStatus);
    }

    // ==========================================
    // Stubs for test execution
    // ==========================================

    private static class StubAttendanceService extends AttendanceService {
        @Override
        public List<AttendanceReportRow> getReport(User actor, Integer enrollmentId, Integer programId, LocalDate fromDate, LocalDate toDate) throws SQLException {
            return getReport(actor, enrollmentId, programId, fromDate, toDate, null);
        }

        @Override
        public List<AttendanceReportRow> getReport(User actor, Integer enrollmentId, Integer programId, LocalDate fromDate, LocalDate toDate, String statusFilter) throws SQLException {
            AttendanceReportRow row = new AttendanceReportRow();
            row.enrollmentId = enrollmentId != null ? enrollmentId : 1;
            row.internName = "Test Intern";
            row.programName = "Java Test";
            row.workDate = LocalDate.now();
            row.status = statusFilter != null ? statusFilter : "PRESENT";
            row.workedHours = 8.0;
            return List.of(row);
        }
    }

    private static class StubAllowanceService extends AllowanceService {
        @Override
        public List<Allowance> getAllowances(User user, Integer enrollmentId) throws SQLException {
            Allowance a = new Allowance();
            a.setAllowanceId(1);
            a.setEnrollmentId(enrollmentId != null ? enrollmentId : 10);
            a.setAllowanceType("MEAL");
            a.setPeriod("2026-10");
            a.setAmount(BigDecimal.valueOf(500000));
            a.setStatus("PAID");
            a.setInternName("Test Intern");
            return List.of(a);
        }
    }

    private static class StubSupportRequestService extends SupportRequestService {
        @Override
        public List<SupportRequest> getMyRequests(User actor) throws SQLException {
            SupportRequest r = new SupportRequest();
            r.setRequestId(1);
            r.setTitle("Help with IDE");
            r.setRequestType("IT_SUPPORT");
            r.setStatus("RESOLVED");
            return List.of(r);
        }

        @Override
        public List<SupportRequest> getAllRequests(User actor) throws SQLException {
            SupportRequest r = new SupportRequest();
            r.setRequestId(1);
            r.setTitle("General inquiry");
            r.setRequestType("HR_INQUIRY");
            r.setStatus("PENDING");
            return List.of(r);
        }
    }
}
