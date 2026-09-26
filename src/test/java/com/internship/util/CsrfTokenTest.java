package com.internship.util;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class CsrfTokenTest {
    @Test
    void tokenIsSessionBoundAndRequired() {
        FakeRequest first = new FakeRequest();
        String token = CsrfToken.expose(first.request);
        assertFalse(token.isBlank());
        assertEquals(token, CsrfToken.expose(first.request));
        assertFalse(CsrfToken.valid(first.request));

        first.params.put("csrfToken", token);
        assertTrue(CsrfToken.valid(first.request));

        FakeRequest second = new FakeRequest();
        CsrfToken.expose(second.request);
        second.params.put("csrfToken", token);
        assertFalse(CsrfToken.valid(second.request));
    }

    private static final class FakeRequest {
        final Map<String, Object> sessionAttributes = new HashMap<>();
        final Map<String, Object> requestAttributes = new HashMap<>();
        final Map<String, String> params = new HashMap<>();
        final HttpSession session = (HttpSession) Proxy.newProxyInstance(
                HttpSession.class.getClassLoader(), new Class<?>[]{HttpSession.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "getAttribute" -> sessionAttributes.get(args[0]);
                    case "setAttribute" -> { sessionAttributes.put((String) args[0], args[1]); yield null; }
                    default -> null;
                });
        final HttpServletRequest request = (HttpServletRequest) Proxy.newProxyInstance(
                HttpServletRequest.class.getClassLoader(), new Class<?>[]{HttpServletRequest.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "getSession" -> session;
                    case "getParameter" -> params.get(args[0]);
                    case "setAttribute" -> { requestAttributes.put((String) args[0], args[1]); yield null; }
                    default -> null;
                });
    }
}
