package com.internship.util;

import jakarta.servlet.http.HttpServletRequest;

/** Authorize the path mapped by the container, not the undecoded request URI. */
public final class RequestPath {
    private RequestPath() { }

    public static String mapped(HttpServletRequest request) {
        String servlet = request.getServletPath();
        if (servlet != null && !servlet.isEmpty()) {
            return servlet + (request.getPathInfo() == null ? "" : request.getPathInfo());
        }
        // Supports root mappings and servlet test doubles. URI.getPath decodes once.
        String raw = request.getRequestURI().substring(request.getContextPath().length());
        try {
            return java.net.URI.create(raw).getPath().replaceAll(";[^/]*", "");
        } catch (IllegalArgumentException e) {
            return "/admin/invalid-path"; // Fail closed on malformed paths.
        }
    }
}
