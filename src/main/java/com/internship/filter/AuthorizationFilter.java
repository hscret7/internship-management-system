package com.internship.filter;

import com.internship.model.User;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;

@WebFilter("/*")
public class AuthorizationFilter implements Filter {

    @Override
    public void doFilter(
            ServletRequest servletRequest,
            ServletResponse servletResponse,
            FilterChain chain
    ) throws IOException, ServletException {

        HttpServletRequest request =
                (HttpServletRequest) servletRequest;

        HttpServletResponse response =
                (HttpServletResponse) servletResponse;

        String contextPath = request.getContextPath();
        String path = com.internship.util.RequestPath.mapped(request);

        if (path.equals("/uploads") || path.startsWith("/uploads/")) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        /*
         * Authorization is only required for role-specific areas.
         * AuthenticationFilter is responsible for checking login/session.
         */
        String requiredArea = getRequiredArea(path);

        if (requiredArea == null) {
            chain.doFilter(request, response);
            return;
        }

        HttpSession session = request.getSession(false);

        if (session == null) {
            response.sendRedirect(contextPath + "/auth/login");
            return;
        }

        Object userAttribute = session.getAttribute("currentUser");

        if (!(userAttribute instanceof User currentUser)
                || currentUser.getRole() == null
                || currentUser.getRole().getRoleName() == null) {

            response.sendRedirect(contextPath + "/auth/login");
            return;
        }

        String role = currentUser.getRole().getRoleName();

        if (!hasAccess(role, requiredArea)) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }

        chain.doFilter(request, response);
    }

    private String getRequiredArea(String path) {

        if (path.equals("/admin") || path.startsWith("/admin/")) {
            return "ADMIN";
        }

        if (path.equals("/hr") || path.startsWith("/hr/")) {
            return "HR";
        }

        if (path.equals("/mentor") || path.startsWith("/mentor/")) {
            return "MENTOR";
        }

        if (path.equals("/intern") || path.startsWith("/intern/")) {
            return "INTERN";
        }

        return null;
    }

    private boolean hasAccess(String role, String requiredArea) {

        if ("ADMIN".equals(role)) {
            return true;
        }

        return role.equals(requiredArea);
    }
}
