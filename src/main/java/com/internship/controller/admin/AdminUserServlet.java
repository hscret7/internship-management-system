package com.internship.controller.admin;

import com.internship.dao.RoleDAO;
import com.internship.dao.UserDAO;
import com.internship.service.UserService;
import com.internship.service.email.SmtpEmailService;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

@WebServlet("/admin/users")
public class AdminUserServlet extends HttpServlet {

    private UserService userService;

    @Override
    public void init() throws ServletException {
        try {
            userService = new UserService(
                    new UserDAO(),
                    new RoleDAO(),
                    new SmtpEmailService()
            );
        } catch (RuntimeException e) {
            throw new ServletException(
                    "Unable to initialize AdminUserServlet.",
                    e
            );
        }
    }

    @Override
    protected void doPost(
            HttpServletRequest request,
            HttpServletResponse response
    ) throws IOException {

        request.setCharacterEncoding("UTF-8");
        response.setContentType("application/json;charset=UTF-8");

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

            response.setStatus(HttpServletResponse.SC_CREATED);

            response.getWriter().printf(
                    """
                    {
                      "success": true,
                      "userId": %d,
                      "emailSent": %s
                    }
                    """,
                    result.user().getUserId(),
                    result.emailSent()
            );

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