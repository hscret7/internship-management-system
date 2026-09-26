package com.internship.util;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

/** Session-bound token for BE3's state-changing HTML forms. */
public final class CsrfToken {
    private static final String KEY = CsrfToken.class.getName();
    private static final SecureRandom RANDOM = new SecureRandom();

    private CsrfToken() { }

    public static String expose(HttpServletRequest request) {
        HttpSession session = request.getSession();
        String token = (String) session.getAttribute(KEY);
        if (token == null) {
            byte[] random = new byte[32];
            RANDOM.nextBytes(random);
            token = Base64.getUrlEncoder().withoutPadding().encodeToString(random);
            session.setAttribute(KEY, token);
        }
        request.setAttribute("csrfToken", token);
        return token;
    }

    public static boolean valid(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session == null) return false;
        Object expected = session.getAttribute(KEY);
        String actual = request.getParameter("csrfToken");
        return expected instanceof String token && actual != null
                && MessageDigest.isEqual(token.getBytes(StandardCharsets.UTF_8),
                                         actual.getBytes(StandardCharsets.UTF_8));
    }
}
