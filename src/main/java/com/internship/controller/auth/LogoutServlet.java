package com.internship.controller.auth;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import com.internship.service.AuditLogService;
import com.internship.util.CsrfToken;
import java.util.Objects;

@WebServlet("/auth/logout")
public class LogoutServlet extends HttpServlet {

    private final AuditLogService auditLog;

    public LogoutServlet() { this(new AuditLogService()); }

    LogoutServlet(AuditLogService auditLog) {
        this.auditLog = Objects.requireNonNull(auditLog);
    }

    @Override
    protected void doPost(
            HttpServletRequest request,
            HttpServletResponse response
    ) throws IOException {

        HttpSession session =
                request.getSession(false);

        response.setHeader("Cache-Control", "no-store");
        if (session != null && !CsrfToken.valid(request)) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }

        if (session != null) {
            if (session.getAttribute("currentUser") instanceof com.internship.model.User user) {
                auditLog.recordBestEffort(user.getUserId(), "LOGOUT", "USER", user.getUserId(), "SUCCESS");
            }
            session.invalidate();
        }

        response.sendRedirect(
                request.getContextPath() + "/auth/login"
        );
    }
}
