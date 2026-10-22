package com.internship.controller;

import com.internship.dao.*;
import com.internship.model.*;
import com.internship.service.CatalogService;
import com.internship.service.EnrollmentService;
import com.internship.util.CsrfToken;
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.lang.reflect.Proxy;
import java.sql.Date;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.*;

class Be3ControllerTest {
    private static final class Http {
        final Map<String, Object> sessionValues = new HashMap<>();
        final Map<String, Object> attributes = new HashMap<>();
        final Map<String, String> parameters = new HashMap<>();
        final Map<String, String> headers = new HashMap<>();
        String forwarded;
        String redirected;
        int status = 200;
        final HttpSession session = proxy(HttpSession.class, (method, args) -> switch (method) {
            case "getAttribute" -> sessionValues.get(args[0]);
            case "setAttribute" -> { sessionValues.put((String) args[0], args[1]); yield null; }
            case "removeAttribute" -> { sessionValues.remove(args[0]); yield null; }
            default -> null;
        });
        final HttpServletRequest request = proxy(HttpServletRequest.class, (method, args) -> switch (method) {
            case "getSession" -> session;
            case "getParameter" -> parameters.get(args[0]);
            case "getAttribute" -> attributes.get(args[0]);
            case "setAttribute" -> { attributes.put((String) args[0], args[1]); yield null; }
            case "getContextPath" -> "/test";
            case "getServletPath" -> "/hr/catalog";
            case "getRequestDispatcher" -> {
                String path = (String) args[0];
                yield proxy(RequestDispatcher.class, (name, values) -> { forwarded = path; return null; });
            }
            default -> null;
        });
        final HttpServletResponse response = proxy(HttpServletResponse.class, (method, args) -> {
            if ("sendError".equals(method)) status = (Integer) args[0];
            if ("sendRedirect".equals(method)) redirected = (String) args[0];
            if ("setHeader".equals(method)) headers.put((String) args[0], (String) args[1]);
            return null;
        });

        Http(String role) {
            if (role != null) {
                User user = new User(); user.setUserId(4_000_000_000L);
                Role r = new Role(); r.setRoleName(role); user.setRole(r); user.setStatus(UserStatus.ACTIVE);
                sessionValues.put("currentUser", user);
            }
        }
        void token() { parameters.put("csrfToken", CsrfToken.expose(request)); }
    }
    @FunctionalInterface private interface Invocation { Object call(String method, Object[] args) throws Throwable; }
    private static <T> T proxy(Class<T> type, Invocation call) {
        return type.cast(Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type},
                (object, method, args) -> call.call(method.getName(), args)));
    }

    private CatalogService catalog() {
        return new CatalogService() {
            @Override public List<Department> getDepartments() { return List.of(); }
            @Override public List<InternshipProgram> getPrograms(Integer id) { return List.of(); }
            @Override public List<Mentor> getMentors(Integer id) { return List.of(); }
            @Override public List<User> getEligibleMentorUsers() { return List.of(); }
        };
    }

    @Test void catalogRequiresHrOrAdminRole() throws Exception {
        for (String role : Arrays.asList(null, "INTERN", "MENTOR")) {
            Http http = new Http(role); new HRCatalogServlet(catalog()).doGet(http.request, http.response);
            assertEquals(403, http.status); assertNull(http.forwarded);
        }
        for (String role : List.of("HR", "ADMIN")) {
            Http http = new Http(role); new HRCatalogServlet(catalog()).doGet(http.request, http.response);
            assertEquals(200, http.status); assertEquals("/WEB-INF/views/hr/catalog.jsp", http.forwarded);
            assertNotNull(http.attributes.get("csrfToken"));
        }
    }

    @Test void catalogPostRejectsMissingAndInvalidToken() throws Exception {
        for (String token : List.of("", "forged")) {
            Http http = new Http("HR"); CsrfToken.expose(http.request);
            http.parameters.put("csrfToken", token);
            new HRCatalogServlet(catalog()).doPost(http.request, http.response);
            assertEquals(403, http.status); assertNull(http.redirected);
        }
    }

    @Test void programUpdateIsAvailableWithSessionToken() throws Exception {
        boolean[] updated = {false};
        CatalogService stub = new CatalogService() {
            @Override public void updateProgram(int id, String name, int dep, String description,
                                                 Date start, Date end, int max) {
                assertEquals(2, id); assertEquals("Updated", name); assertEquals(1, dep);
                assertEquals(start, end); updated[0] = true;
            }
        };
        Http http = new Http("HR"); http.token();
        http.parameters.putAll(Map.of("action", "update-program", "programId", "2", "name", "Updated",
                "departmentId", "1", "startDate", "2026-10-01", "endDate", "2026-10-01", "maxInterns", "5"));
        new HRCatalogServlet(stub).doPost(http.request, http.response);
        assertTrue(updated[0]); assertEquals("/test/hr/catalog", http.redirected);
    }

    @Test void rejectsImpossibleOrNonCanonicalCalendarDates() {
        Http http = new Http("HR");
        for (String date : List.of("2026-02-30", "2026-02-29", "2026-2-1", "0999-01-01", "2026-13-01")) {
            http.parameters.put("startDate", date);
            assertThrows(IllegalArgumentException.class, () -> HRCatalogServlet.dateParam(http.request, "startDate"));
        }
        http.parameters.put("startDate", "2028-02-29");
        assertEquals(Date.valueOf("2028-02-29"), HRCatalogServlet.dateParam(http.request, "startDate"));
    }

    // US14 protects the BE3 -> FE1 data contract without prescribing JSP markup.
    @Test void internOverviewExposesDisplayDataForSessionUserNotClientIds() throws Exception {
        AtomicLong requestedUser = new AtomicLong();
        InternProfileDAO profiles = new InternProfileDAO() {
            @Override public InternProfile getProfileByUserId(long id) {
                requestedUser.set(id); InternProfile profile = new InternProfile(); profile.setProfileId(8); return profile;
            }
        };
        ProgramEnrollment enrollment = sampleEnrollment();
        MentorAssignment mentor = new MentorAssignment();
        mentor.setEnrollmentId(12); mentor.setMentorId(3); mentor.setMentorName("Nguyễn Văn An");
        mentor.setAssignedAt(Timestamp.valueOf("2026-10-02 09:00:00"));
        EnrollmentService service = new EnrollmentService() {
            @Override public ProgramEnrollment getActiveEnrollmentByIntern(int id) { assertEquals(8, id); return enrollment; }
            @Override public MentorAssignment getCurrentMentor(int id) { assertEquals(12, id); return mentor; }
        };
        Http http = new Http("INTERN"); http.parameters.put("userId", "99"); http.parameters.put("enrollmentId", "999");
        http.parameters.put("profileId", "888");
        new InternEnrollmentServlet(service, profiles).doGet(http.request, http.response);
        assertEquals(4_000_000_000L, requestedUser.get());
        assertSame(enrollment, http.attributes.get("enrollment"));
        assertSame(mentor, http.attributes.get("currentMentor"));
        ProgramEnrollment displayed = (ProgramEnrollment) http.attributes.get("enrollment");
        assertAll("FE1 enrollment bean properties",
                () -> assertEquals("Thực tập <Engineering>", displayed.getProgramName()),
                () -> assertEquals("R&D", displayed.getDepartmentName()),
                () -> assertEquals(Date.valueOf("2026-10-01"), displayed.getStartDate()),
                () -> assertEquals(Date.valueOf("2026-12-31"), displayed.getEndDate()),
                () -> assertEquals("ACTIVE", displayed.getStatus()),
                () -> assertEquals(Timestamp.valueOf("2026-10-01 08:30:00"), displayed.getEnrolledAt()),
                () -> assertEquals(3, displayed.getCurrentMentorId()),
                () -> assertEquals("Nguyễn Văn An", displayed.getCurrentMentorName()));
        MentorAssignment displayedMentor = (MentorAssignment) http.attributes.get("currentMentor");
        assertEquals("Nguyễn Văn An", displayedMentor.getMentorName());
        assertEquals(Timestamp.valueOf("2026-10-02 09:00:00"), displayedMentor.getAssignedAt());
        assertNull(http.attributes.get("errorMessage"));
        assertInternOverviewResponse(http);
    }

    @Test void internOverviewKeepsEnrollmentWhenNoMentorIsAssigned() throws Exception {
        ProgramEnrollment enrollment = sampleEnrollment();
        enrollment.setCurrentMentorId(null); enrollment.setCurrentMentorName(null);
        Http http = new Http("INTERN");
        new InternEnrollmentServlet(new EnrollmentService() {
            @Override public ProgramEnrollment getActiveEnrollmentByIntern(int id) { assertEquals(8, id); return enrollment; }
            @Override public MentorAssignment getCurrentMentor(int id) { assertEquals(12, id); return null; }
        }, internProfile()).doGet(http.request, http.response);
        assertSame(enrollment, http.attributes.get("enrollment"));
        assertNull(http.attributes.get("currentMentor"));
        assertNull(http.attributes.get("errorMessage"));
        assertInternOverviewResponse(http);
    }

    @Test void internOverviewWithoutProfileIsAnEmptyStateNotAnError() throws Exception {
        Http http = new Http("INTERN");
        new InternEnrollmentServlet(new EnrollmentService() {
            @Override public ProgramEnrollment getActiveEnrollmentByIntern(int id) {
                fail("Do not load enrollment without an intern profile"); return null;
            }
        }, new InternProfileDAO() {
            @Override public InternProfile getProfileByUserId(long id) { return null; }
        }).doGet(http.request, http.response);
        assertNull(http.attributes.get("enrollment"));
        assertNull(http.attributes.get("currentMentor"));
        assertNull(http.attributes.get("errorMessage"));
        assertInternOverviewResponse(http);
    }

    @Test void internOverviewWithoutActiveEnrollmentIsAnEmptyStateNotAnError() throws Exception {
        Http http = new Http("INTERN");
        new InternEnrollmentServlet(new EnrollmentService() {
            @Override public ProgramEnrollment getActiveEnrollmentByIntern(int id) { assertEquals(8, id); return null; }
            @Override public MentorAssignment getCurrentMentor(int id) {
                fail("Do not load mentor without an enrollment"); return null;
            }
        }, internProfile()).doGet(http.request, http.response);
        assertNull(http.attributes.get("enrollment"));
        assertNull(http.attributes.get("currentMentor"));
        assertNull(http.attributes.get("errorMessage"));
        assertInternOverviewResponse(http);
    }

    @ParameterizedTest
    @ValueSource(strings = {"profile", "enrollment", "mentor"})
    void internOverviewDatabaseFailuresExposeAnErrorNotPartialOrEmptySuccess(String stage) throws Exception {
        SQLException failure = new SQLException("private database detail: simulated " + stage + " failure");
        Http http = new Http("INTERN");
        new InternEnrollmentServlet(new EnrollmentService() {
            @Override public ProgramEnrollment getActiveEnrollmentByIntern(int id) throws SQLException {
                assertNotEquals("profile", stage, "Do not continue after a profile lookup failure");
                assertEquals(8, id);
                if ("enrollment".equals(stage)) throw failure;
                return sampleEnrollment();
            }
            @Override public MentorAssignment getCurrentMentor(int id) throws SQLException {
                assertEquals("mentor", stage, "Do not continue after an earlier lookup failure");
                assertEquals(12, id);
                throw failure;
            }
        }, new InternProfileDAO() {
            @Override public InternProfile getProfileByUserId(long id) throws SQLException {
                if ("profile".equals(stage)) throw failure;
                InternProfile profile = new InternProfile(); profile.setProfileId(8); return profile;
            }
        }).doGet(http.request, http.response);
        String error = (String) http.attributes.get("errorMessage");
        assertNotNull(error);
        assertFalse(error.isBlank());
        assertFalse(error.contains(failure.getMessage()), "Database details must not leak into the view");
        assertNull(http.attributes.get("enrollment"), "Discard partial enrollment data when loading fails");
        assertNull(http.attributes.get("currentMentor"));
        assertInternOverviewResponse(http);
    }

    @Test void internOverviewRejectsUnauthenticatedAndOtherRolesBeforeDataAccess() throws Exception {
        for (String role : Arrays.asList(null, "HR", "ADMIN", "MENTOR")) {
            Http http = new Http(role);
            new InternEnrollmentServlet(new EnrollmentService(), new InternProfileDAO() {
                @Override public InternProfile getProfileByUserId(long id) {
                    fail("Forbidden requests must not load private intern data"); return null;
                }
            }).doGet(http.request, http.response);
            assertEquals(403, http.status, "Role: " + role);
            assertNull(http.forwarded);
            assertNull(http.attributes.get("enrollment"));
            assertNull(http.attributes.get("currentMentor"));
        }
    }

    private static void assertInternOverviewResponse(Http http) {
        assertEquals(200, http.status);
        assertEquals("/WEB-INF/views/intern/enrollment.jsp", http.forwarded);
        assertEquals("no-store", http.headers.get("Cache-Control"));
    }

    private static InternProfileDAO internProfile() {
        return new InternProfileDAO() {
            @Override public InternProfile getProfileByUserId(long id) {
                assertEquals(4_000_000_000L, id);
                InternProfile profile = new InternProfile(); profile.setProfileId(8); return profile;
            }
        };
    }

    private static ProgramEnrollment sampleEnrollment() {
        ProgramEnrollment enrollment = new ProgramEnrollment();
        enrollment.setEnrollmentId(12); enrollment.setInternProfileId(8);
        enrollment.setProgramName("Thực tập <Engineering>"); enrollment.setDepartmentName("R&D");
        enrollment.setStartDate(Date.valueOf("2026-10-01")); enrollment.setEndDate(Date.valueOf("2026-12-31"));
        enrollment.setStatus("ACTIVE"); enrollment.setEnrolledAt(Timestamp.valueOf("2026-10-01 08:30:00"));
        enrollment.setCurrentMentorId(3); enrollment.setCurrentMentorName("Nguyễn Văn An");
        return enrollment;
    }

    @Test void mentorFormOnlyOffersSameDepartmentWithSpace() throws Exception {
        ProgramEnrollment enrollment = new ProgramEnrollment(); enrollment.setProgramId(2); enrollment.setStatus("ACTIVE");
        InternshipProgram program = new InternshipProgram(); program.setDepartmentId(1);
        Mentor valid = mentor(1, 1, 0); Mentor full = mentor(2, 1, 2); Mentor other = mentor(3, 2, 0);
        HREnrollmentServlet servlet = new HREnrollmentServlet(new EnrollmentService() {
            @Override public ProgramEnrollment getEnrollmentById(int id) { return enrollment; }
        }, new InternshipProgramDAO() {
            @Override public InternshipProgram getProgramById(int id) { return program; }
        }, new MentorDAO() {
            @Override public List<Mentor> getActiveMentors() { return List.of(valid, full, other); }
        });
        Http http = new Http("HR"); http.parameters.putAll(Map.of("action", "assign-form", "enrollmentId", "1"));
        servlet.doGet(http.request, http.response); assertEquals(List.of(valid), http.attributes.get("mentors"));
        enrollment.setStatus("COMPLETED");
        new HREnrollmentServlet(new EnrollmentService() {
            @Override public ProgramEnrollment getEnrollmentById(int id) { return enrollment; }
            @Override public List<ProgramEnrollment> getAllEnrollments() { return List.of(enrollment); }
        }, new InternshipProgramDAO(), new MentorDAO()).doGet(http.request, http.response);
        assertNotNull(http.attributes.get("errorMessage"));
    }

    private Mentor mentor(int id, int department, int count) {
        Mentor m = new Mentor(); m.setMentorId(id); m.setDepartmentId(department);
        m.setMaxMentees(2); m.setCurrentMenteeCount(count); return m;
    }

    @Test void partialEnrollmentSuccessRedirectsToExistingEnrollment() throws Exception {
        Http http = new Http("HR"); http.token();
        http.parameters.putAll(Map.of("action", "enroll", "applicationId", "1", "programId", "2", "mentorId", "3"));
        HREnrollmentServlet servlet = new HREnrollmentServlet(new EnrollmentService() {
            @Override public EnrollmentResult enrollInternWithMentor(int app, int program, Integer mentor, long hr) {
                assertEquals(4_000_000_000L, hr); assertEquals(3, mentor);
                return new EnrollmentResult(42, null, "Đã ghi danh nhưng mentor hết chỗ.");
            }
        }, new InternshipProgramDAO(), new MentorDAO());
        servlet.doPost(http.request, http.response);
        assertEquals("/test/hr/enrollments?action=detail&id=42", http.redirected);
        assertNotNull(http.sessionValues.get("successMessage")); assertNotNull(http.sessionValues.get("errorMessage"));
    }
}
