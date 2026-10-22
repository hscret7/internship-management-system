package com.internship.controller;

import com.internship.model.Role;
import com.internship.model.User;
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class Fe3PageServletTest {
    @Test void hrDashboardMappingCannotDisappearAgain() {
        WebServlet routes = Fe3PageServlet.class.getAnnotation(WebServlet.class);
        assertNotNull(routes);
        assertTrue(Arrays.asList(routes.value()).contains("/hr/dashboard"));
    }

    @Test void hrCanOpenDashboard() throws Exception {
        Capture result = run("/hr/dashboard", user("HR"));
        assertEquals("/WEB-INF/views/hr/dashboard-overview.jsp", result.view);
        assertTrue(result.forwarded);
        assertEquals("no-store", result.headers.get("Cache-Control"));
        assertEquals(0, result.error);
    }

    @Test void adminCanOpenDashboard() throws Exception {
        assertTrue(run("/hr/dashboard", user("ADMIN")).forwarded);
    }

    @Test void mentorAndInternCannotOpenHrDashboard() throws Exception {
        for (String role : new String[]{"MENTOR", "INTERN"}) {
            Capture result = run("/hr/dashboard", user(role));
            assertEquals(403, result.error);
            assertFalse(result.forwarded);
        }
    }

    @Test void anonymousUserIsSentToLogin() throws Exception {
        Capture result = run("/hr/dashboard", null);
        assertEquals("/preview/auth/login", result.redirect);
        assertFalse(result.forwarded);
    }

    @Test void invalidSessionObjectIsNotTrusted() throws Exception {
        assertEquals("/preview/auth/login", run("/hr/dashboard", "not a User").redirect);
    }

    @Test void userWithoutRoleCannotAccessHrPage() throws Exception {
        assertEquals(403, run("/hr/dashboard", new User()).error);
    }

    @Test void existingFe3PagesStillForwardToTheirViews() throws Exception {
        String[][] pages = {{"/hr/evaluations", "HR", "hr/evaluation-result.jsp"},
                {"/mentor/dashboard", "MENTOR", "mentor/dashboard.jsp"},
                {"/mentor/tasks", "MENTOR", "mentor/tasks.jsp"},
                {"/mentor/reports", "MENTOR", "mentor/reports.jsp"},
                {"/mentor/evaluation", "MENTOR", "mentor/evaluation.jsp"},
                {"/intern/tasks", "INTERN", "intern/task-list.jsp"},
                {"/intern/tasks/detail", "INTERN", "intern/task-detail.jsp"},
                {"/intern/reports", "INTERN", "intern/weekly-report.jsp"},
                {"/intern/evaluation", "INTERN", "intern/evaluation.jsp"}};
        for (String[] page : pages) {
            Capture result = run(page[0], user(page[1]));
            assertEquals("/WEB-INF/views/" + page[2], result.view);
            assertTrue(result.forwarded);
        }
    }

    @Test void unknownHrRouteIsNotFound() throws Exception {
        assertEquals(404, run("/hr/does-not-exist", user("HR")).error);
    }

    @Test void restoredHrViewsKeepUtf8AndSharedLayout() throws Exception {
        for (String name : new String[]{"applications.jsp", "catalog.jsp", "enrollments.jsp", "dashboard-overview.jsp"}) {
            String source = Files.readString(Path.of("src/main/webapp/WEB-INF/views/hr", name), StandardCharsets.UTF_8);
            assertTrue(source.contains("contentType=\"text/html; charset=UTF-8\""), name);
            assertTrue(source.contains("pageEncoding=\"UTF-8\""), name);
            assertTrue(source.contains("/WEB-INF/views/common/header.jsp"), name);
            assertTrue(source.contains("/WEB-INF/views/common/footer.jsp"), name);
            assertFalse(source.contains("<<<<<<<"), name);
        }
    }

    @Test void evaluatedInternMetricMustNotTreatNamesAsUniqueIdentity() throws Exception {
        String source = Files.readString(Path.of("src/main/webapp/assets/js/fe3.js"), StandardCharsets.UTF_8);
        assertTrue(source.contains("return entry.internProfileId;"));
        assertTrue(source.contains("new Set(profileIds).size"));
        assertTrue(source.contains("Number.isSafeInteger(id) && id > 0"));
        assertFalse(source.contains("return entry.internName;"));
    }

    @Test void bootstrapTableColorsMustFollowTheSharedTheme() throws Exception {
        String source = Files.readString(Path.of("src/main/webapp/assets/css/fe3.css"), StandardCharsets.UTF_8);
        for (String state : new String[]{"color", "hover-color", "striped-color", "active-color"}) {
            assertTrue(source.contains("--bs-table-" + state + ": var(--text);"), state);
        }
    }

    @Test void sprint3PagesKeepAuthenticatedRoleScope() throws Exception {
        assertEquals("/WEB-INF/views/hr/end-term-report.jsp",run("/hr/end-term-report",user("HR")).view);
        assertTrue(run("/hr/end-term-report",user("ADMIN")).forwarded);
        assertEquals(403,run("/hr/end-term-report",user("MENTOR")).error);
        assertEquals("/WEB-INF/views/mentor/meetings.jsp",run("/mentor/meetings",user("MENTOR")).view);
        assertEquals(403,run("/mentor/meetings",user("INTERN")).error);
        assertEquals("/preview/auth/login",run("/mentor/meetings",null).redirect);
        for(String path:new String[]{"/hr/end-term-report","/mentor/meetings"})
            assertTrue(Arrays.asList(Fe3PageServlet.class.getAnnotation(WebServlet.class).value()).contains(path));
    }

    @Test void sprint3FormsAndPagesDeclareUtf8AndExplicitCriteriaScale() throws Exception {
        for(String path:new String[]{"hr/end-term-report.jsp","mentor/meetings.jsp","mentor/evaluation.jsp","intern/weekly-report.jsp"}) {
            String source=Files.readString(Path.of("src/main/webapp/WEB-INF/views",path),StandardCharsets.UTF_8);
            assertTrue(source.contains("pageEncoding=\"UTF-8\""),path);
            assertTrue(source.contains("/WEB-INF/views/common/header.jsp"),path);
            assertFalse(source.contains("<<<<<<<"),path);
        }
        String report=Files.readString(Path.of("src/main/webapp/WEB-INF/views/intern/weekly-report.jsp"));
        assertTrue(report.contains("id=\"reportDetail\" class=\"fe3-report-content\""),
                "Submitted report sections must retain line breaks instead of collapsing into one paragraph");
        String styles=Files.readString(Path.of("src/main/webapp/assets/css/fe3.css"));
        assertTrue(styles.contains(".fe3-report-content { white-space: pre-wrap; overflow-wrap: anywhere; }"));
        for(String name:new String[]{"results","difficulties","nextPlan"})assertTrue(report.contains("name=\""+name+"\""));
        String assessment=Files.readString(Path.of("src/main/webapp/WEB-INF/views/mentor/evaluation.jsp"));
        assertTrue(assessment.contains("name=\"skillsScore\""));assertTrue(assessment.contains("name=\"attitudeScore\""));
        String script=Files.readString(Path.of("src/main/webapp/assets/js/fe3.js"));
        assertEquals(2,script.split("Người phản hồi #",-1).length-1,"Mentor and Intern both see the actual historical reviewer");
        assertTrue(script.contains("Tài khoản #"));
    }

    private static User user(String name) {
        Role role = new Role(); role.setRoleName(name);
        User user = new User(); user.setRole(role); user.setUserId(1L);
        return user;
    }

    private static Capture run(String path, Object actor) throws Exception {
        Capture capture = new Capture();
        HttpSession session = actor == null ? null : proxy(HttpSession.class, (method, args) ->
                "getAttribute".equals(method) && "currentUser".equals(args[0]) ? actor : null);
        HttpServletRequest request = proxy(HttpServletRequest.class, (method, args) -> switch (method) {
            case "getSession" -> session;
            case "getServletPath" -> path;
            case "getContextPath" -> "/preview";
            case "getRequestDispatcher" -> {
                capture.view = (String) args[0];
                yield proxy(RequestDispatcher.class, (action, parameters) -> {
                    if ("forward".equals(action)) capture.forwarded = true;
                    return null;
                });
            }
            default -> null;
        });
        HttpServletResponse response = proxy(HttpServletResponse.class, (method, args) -> {
            switch (method) {
                case "sendRedirect" -> capture.redirect = (String) args[0];
                case "sendError" -> capture.error = (Integer) args[0];
                case "setHeader" -> capture.headers.put((String) args[0], (String) args[1]);
                default -> { return null; }
            }
            return null;
        });
        new Fe3PageServlet().doGet(request, response);
        return capture;
    }

    interface Calls { Object invoke(String name, Object[] args); }
    private static <T> T proxy(Class<T> type, Calls calls) {
        return type.cast(Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type}, (p, method, args) ->
                calls.invoke(method.getName(), args == null ? new Object[0] : args)));
    }
    static class Capture {
        String view;
        String redirect;
        int error;
        boolean forwarded;
        final Map<String, String> headers = new HashMap<>();
    }
}
