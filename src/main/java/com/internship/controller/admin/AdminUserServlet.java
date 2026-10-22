package com.internship.controller.admin;

import java.io.IOException;
import com.internship.service.AuditLogService;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.internship.dao.RoleDAO;
import com.internship.dao.UserDAO;
import com.internship.service.UserService;
import com.internship.service.email.EmailException;
import com.internship.service.email.EmailService;
import com.internship.service.email.SmtpEmailService;
import com.internship.util.CsrfToken;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@WebServlet("/admin/users")
public class AdminUserServlet extends HttpServlet {

    private final AuditLogService auditLog;

    private static final Logger LOGGER =
            Logger.getLogger(AdminUserServlet.class.getName());

    private UserService userService;

    public AdminUserServlet() {
        this(null, new AuditLogService());
    }

    public AdminUserServlet(UserService userService) {
        this(userService, new AuditLogService());
    }

    public AdminUserServlet(UserService userService, AuditLogService auditLog) {
        this.userService = userService;
        this.auditLog = java.util.Objects.requireNonNull(auditLog);
    }

    @Override
    public void init() {
        if (userService != null) {
            return;
        }

        EmailService emailService;
        try {
            emailService = new SmtpEmailService();
        } catch (RuntimeException e) {
            LOGGER.log(
                    Level.WARNING,
                    "EmailService initialization failed; account emails disabled.",
                    e
            );
            emailService = (recipient, subject, content) -> {
                throw new EmailException("Email service unavailable", e);
            };
        }

        UserDAO userDAO = new UserDAO();
        RoleDAO roleDAO = new RoleDAO();
        userService = new UserService(
                userDAO,
                roleDAO,
                emailService
        );
    }

    @Override
    protected void doPost(
            HttpServletRequest request,
            HttpServletResponse response
    ) throws IOException {

        request.setCharacterEncoding("UTF-8");
        response.setContentType("application/json;charset=UTF-8");

        var actorSession = request.getSession(false);
        if (actorSession == null
                || !(actorSession.getAttribute("currentUser") instanceof com.internship.model.User adminActor)
                || adminActor.getStatus() != com.internship.model.UserStatus.ACTIVE
                || adminActor.getRole() == null || !"ADMIN".equals(adminActor.getRole().getRoleName())) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            writeError(response, "Admin access required.");
            return;
        }

        if (!CsrfToken.valid(request)) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            writeError(
                    response,
                    "Invalid CSRF token."
            );
            return;
        }

        String username = request.getParameter("username");
        String email = request.getParameter("email");
        String fullName = request.getParameter("fullName");
        String role = request.getParameter("role");

        try {
            UserService.CreateUserResult result =
                    userService.createUser(
                            username,
                            email,
                            fullName,
                            role
                    );

            var session = request.getSession(false);
            Long actorId = session != null && session.getAttribute("currentUser") instanceof com.internship.model.User actor
                    ? actor.getUserId() : null;
            auditLog.recordBestEffort(actorId, "ACCOUNT_CREATE", "USER", result.user().getUserId(), "SUCCESS");

            response.setStatus(HttpServletResponse.SC_CREATED);

            if (result.emailSent()) {
                response.getWriter().printf(
                        """
                        {
                          "success": true,
                          "userId": %d,
                          "emailSent": true
                        }
                        """,
                        result.user().getUserId()
                );
            } else {
                response.getWriter().printf(
                        """
                        {
                          "success": true,
                          "userId": %d,
                          "emailSent": false,
                          "temporaryPassword": "%s"
                        }
                        """,
                        result.user().getUserId(),
                        escapeJson(result.temporaryPassword())
                );
            }

        } catch (IllegalArgumentException e) {

            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);

            writeError(
                    response,
                    e.getMessage()
            );

        } catch (IllegalStateException e) {

            response.setStatus(
                    HttpServletResponse.SC_INTERNAL_SERVER_ERROR
            );

            writeError(
                    response,
                    "Unable to create user."
            );
        }
    }

    private void writeError(
            HttpServletResponse response,
            String message
    ) throws IOException {

        response.getWriter().printf(
                """
                {
                  "success": false,
                  "message": "%s"
                }
                """,
                escapeJson(message)
        );
    }

    private String escapeJson(String value) {

        if (value == null) {
            return "";
        }

        return value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r");
    }
}
