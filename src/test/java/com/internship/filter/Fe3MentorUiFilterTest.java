package com.internship.filter;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

class Fe3MentorUiFilterTest {
    private static final String CONTEXT = "/internship-management-system";

    @Test void createMentorReturnsToMentorDirectory() throws Exception {
        assertEquals(CONTEXT + "/hr/mentors", redirect("/hr/mentors", Map.of("action", "create-mentor"), CONTEXT + "/hr/catalog"));
    }

    @Test void mentorRedirectAlreadyCorrectIsKept() throws Exception {
        assertEquals(CONTEXT + "/hr/mentors", redirect("/hr/mentors", Map.of(), CONTEXT + "/hr/mentors"));
    }

    @Test void assignedMentorReturnsToEnrollmentHistory() throws Exception {
        assertEquals(CONTEXT + "/hr/enrollments?ui=mentor&action=detail&id=17", redirect("/hr/enrollments",
                Map.of("ui", "mentor", "action", "assign-mentor", "enrollmentId", "17"), CONTEXT + "/hr/enrollments"));
    }

    @Test void changedMentorKeepsUiEvenWhenBackendAddsQuery() throws Exception {
        assertEquals(CONTEXT + "/hr/enrollments?ui=mentor&action=detail&id=8", redirect("/hr/enrollments",
                Map.of("ui", "mentor", "action", "change-mentor", "enrollmentId", "8"), CONTEXT + "/hr/enrollments?status=ACTIVE"));
    }

    @Test void malformedIdsCannotBePlacedInRedirect() throws Exception {
        for (String id : new String[]{"0", "-1", "2147483648", "4&other=1", "abc", ""}) {
            assertEquals(CONTEXT + "/hr/enrollments?ui=mentor", redirect("/hr/enrollments",
                    Map.of("ui", "mentor", "action", "assign-mentor", "enrollmentId", id), CONTEXT + "/hr/enrollments"));
        }
        assertNull(Fe3MentorUiFilter.positiveId(null));
    }

    @Test void authenticationRedirectIsNeverRewritten() throws Exception {
        assertEquals(CONTEXT + "/auth/login", redirect("/hr/mentors", Map.of(), CONTEXT + "/auth/login"));
        assertEquals(CONTEXT + "/auth/login", redirect("/hr/enrollments",
                Map.of("ui", "mentor", "action", "assign-mentor", "enrollmentId", "3"), CONTEXT + "/auth/login"));
    }

    @Test void externalAndUnrelatedLocationsArePreserved() throws Exception {
        assertEquals("https://example.invalid/hr/catalog", redirect("/hr/mentors", Map.of(), "https://example.invalid/hr/catalog"));
        assertEquals(CONTEXT + "/hr/applications", redirect("/hr/mentors", Map.of(), CONTEXT + "/hr/applications"));
    }

    @Test void fe2EnrollmentPostsAreUntouched() throws Exception {
        assertEquals(CONTEXT + "/hr/enrollments", redirect("/hr/enrollments",
                Map.of("action", "assign-mentor", "enrollmentId", "9"), CONTEXT + "/hr/enrollments"));
        assertEquals(CONTEXT + "/hr/enrollments", redirect("/hr/enrollments",
                Map.of("ui", "mentor", "action", "enroll", "enrollmentId", "9"), CONTEXT + "/hr/enrollments"));
    }

    @Test void unknownPathsDoNotChangeHeadersOrRedirects() throws Exception {
        Capture output = new Capture();
        HttpServletResponse response = output.response();
        new Fe3MentorUiFilter().doFilter(request("POST", "/hr/catalog", Map.of()), response,
                (request, passed) -> { assertSame(response, passed); ((HttpServletResponse) passed).sendRedirect(CONTEXT + "/hr/catalog"); });
        assertTrue(output.headers.isEmpty());
        assertEquals(CONTEXT + "/hr/catalog", output.redirect);
    }

    @Test void getPassesOriginalObjectsThroughAndDisablesCaching() throws Exception {
        Capture output = new Capture();
        HttpServletRequest request = request("GET", "/hr/mentors", Map.of());
        HttpServletResponse response = output.response();
        AtomicBoolean called = new AtomicBoolean();
        new Fe3MentorUiFilter().doFilter(request, response, (passedRequest, passedResponse) -> {
            called.set(true); assertSame(request, passedRequest); assertSame(response, passedResponse);
        });
        assertTrue(called.get());
        assertEquals("no-store", output.headers.get("Cache-Control"));
    }

    @Test void originalAuthorizationAndCsrfErrorsPassThrough() throws Exception {
        Capture output = new Capture();
        new Fe3MentorUiFilter().doFilter(request("POST", "/hr/mentors", Map.of()), output.response(),
                (request, response) -> ((HttpServletResponse) response).sendError(403));
        assertEquals(403, output.errorStatus);
        assertNull(output.redirect);
    }

    @Test void assignmentsGetDisablesCachingOnlyWhenOptedIn() throws Exception {
        Capture output = new Capture();
        new Fe3MentorUiFilter().doFilter(request("GET", "/hr/enrollments", Map.of("ui", "mentor")), output.response(), (r, s) -> {});
        assertEquals("no-store", output.headers.get("Cache-Control"));
        Capture original = new Capture();
        new Fe3MentorUiFilter().doFilter(request("GET", "/hr/enrollments", Map.of()), original.response(), (r, s) -> {});
        assertTrue(original.headers.isEmpty());
    }

    private String redirect(String path, Map<String, String> parameters, String original) throws Exception {
        Capture output = new Capture();
        new Fe3MentorUiFilter().doFilter(request("POST", path, parameters), output.response(),
                (request, response) -> ((HttpServletResponse) response).sendRedirect(original));
        return output.redirect;
    }

    private static HttpServletRequest request(String method, String path, Map<String, String> parameters) {
        return (HttpServletRequest) Proxy.newProxyInstance(HttpServletRequest.class.getClassLoader(), new Class<?>[]{HttpServletRequest.class},
                (proxy, called, args) -> switch (called.getName()) {
                    case "getMethod" -> method;
                    case "getServletPath" -> path;
                    case "getContextPath" -> CONTEXT;
                    case "getParameter" -> parameters.get((String) args[0]);
                    default -> fallback(called.getReturnType());
                });
    }

    private static Object fallback(Class<?> type) {
        if (type == boolean.class) return false;
        if (type == int.class) return 0;
        if (type == long.class) return 0L;
        return null;
    }

    private static class Capture {
        final Map<String, String> headers = new HashMap<>();
        String redirect;
        int errorStatus;
        HttpServletResponse response() {
            return (HttpServletResponse) Proxy.newProxyInstance(HttpServletResponse.class.getClassLoader(), new Class<?>[]{HttpServletResponse.class},
                    (proxy, called, args) -> {
                        switch (called.getName()) {
                            case "setHeader" -> headers.put((String) args[0], (String) args[1]);
                            case "sendRedirect" -> redirect = (String) args[0];
                            case "sendError" -> errorStatus = (Integer) args[0];
                            default -> { return fallback(called.getReturnType()); }
                        }
                        return null;
                    });
        }
    }
}
