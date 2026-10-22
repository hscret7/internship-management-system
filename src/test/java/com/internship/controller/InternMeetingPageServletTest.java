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

class InternMeetingPageServletTest {
    @Test void activeInternCanOpenExistingView() throws Exception {
        var c=run("/intern/meetings",user("INTERN"));
        assertEquals("/WEB-INF/views/intern/meetings.jsp",c.view);assertTrue(c.forwarded);
        assertEquals("no-store",c.headers.get("Cache-Control"));
    }
    @Test void anonymousAndMalformedSessionsRedirect() throws Exception {
        assertEquals("/preview/auth/login",run("/intern/meetings",null).redirect);
        assertEquals("/preview/auth/login",run("/intern/meetings","bad").redirect);
    }
    @Test void otherRolesAndMissingRoleCannotOpenInternView() throws Exception {
        for(String role:new String[]{"ADMIN","HR","MENTOR"}) {
            var c=run("/intern/meetings",user(role));assertEquals(403,c.error);assertFalse(c.forwarded);
        }
        assertEquals(403,run("/intern/meetings",new User()).error);
    }
    @Test void inactiveInternCannotOpenView() throws Exception {
        User u=user("INTERN");u.setStatus(com.internship.model.UserStatus.LOCKED);
        assertEquals(403,run("/intern/meetings",u).error);
    }
    @Test void mappedPathDoesNotConflictWithJsonApi() {
        assertArrayEquals(new String[]{"/intern/meetings"},InternMeetingPageServlet.class.getAnnotation(WebServlet.class).value());
    }

    private static User user(String name) {
        Role role = new Role(); role.setRoleName(name);
        User user = new User(); user.setStatus(com.internship.model.UserStatus.ACTIVE); user.setRole(role); user.setUserId(1L);
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
        new InternMeetingPageServlet().doGet(request, response);
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
