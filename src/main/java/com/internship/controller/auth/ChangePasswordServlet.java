package com.internship.controller.auth;

import com.internship.dao.RoleDAO;
import com.internship.dao.UserDAO;
import com.internship.model.User;
import com.internship.service.UserService;
import com.internship.util.CsrfToken;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Objects;

@WebServlet("/auth/change-password")
public class ChangePasswordServlet extends HttpServlet {
    private final UserService users;
    public ChangePasswordServlet() { this(new UserService(new UserDAO(), new RoleDAO())); }
    ChangePasswordServlet(UserService users) { this.users = Objects.requireNonNull(users); }

    @Override protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.setHeader("Cache-Control", "no-store");
        if (actor(request) == null) {
            response.sendRedirect(request.getContextPath() + "/auth/login");
            return;
        }
        CsrfToken.expose(request);
        request.getRequestDispatcher("/WEB-INF/views/auth/change-password.jsp").forward(request, response);
    }

    @Override protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.setCharacterEncoding("UTF-8");
        response.setHeader("Cache-Control", "no-store");
        User user = actor(request);
        if (user == null) { response.sendError(HttpServletResponse.SC_UNAUTHORIZED); return; }
        if (!CsrfToken.valid(request)) { response.sendError(HttpServletResponse.SC_FORBIDDEN); return; }
        try {
            users.changePassword(user.getUserId(), request.getParameter("currentPassword"),
                    request.getParameter("newPassword"), request.getParameter("confirmPassword"));
            request.getSession(false).invalidate();
            request.getSession(true).setAttribute("flashSuccess", "Đổi mật khẩu thành công. Vui lòng đăng nhập lại.");
            response.sendRedirect(request.getContextPath() + "/auth/login");
        } catch (IllegalArgumentException e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            request.setAttribute("errorMessage", e.getMessage());
            doGet(request, response);
        } catch (SQLException e) {
            response.setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
            request.setAttribute("errorMessage", "Chưa thể đổi mật khẩu. Vui lòng thử lại sau.");
            doGet(request, response);
        }
    }

    private User actor(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        return session != null && session.getAttribute("currentUser") instanceof User user ? user : null;
    }
}
