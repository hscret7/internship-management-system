package com.internship.controller.admin;

import com.internship.dao.RoleAssignmentDAO;
import com.internship.dao.UserDAO;
import com.internship.model.User;
import com.internship.model.UserStatus;
import com.internship.util.CsrfToken;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

@WebServlet("/admin/roles")
public class AdminRoleServlet extends HttpServlet {
    private final UserDAO users;
    private final RoleAssignmentDAO assignments;
    public AdminRoleServlet() { this(new UserDAO(), new RoleAssignmentDAO()); }
    public AdminRoleServlet(UserDAO users, RoleAssignmentDAO assignments) {
        this.users = users; this.assignments = assignments;
    }
    private User admin(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        Object value = session == null ? null : session.getAttribute("currentUser");
        return value instanceof User u && u.getStatus() == UserStatus.ACTIVE
                && u.getRole() != null && "ADMIN".equals(u.getRole().getRoleName()) ? u : null;
    }
    @Override protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        if (admin(request) == null) { response.sendError(403); return; }
        try { request.setAttribute("users", users.findAll()); }
        catch (SQLException e) { logFailure(e); response.sendError(503); return; }
        request.setAttribute("roleNames", List.of("ADMIN", "HR", "MENTOR", "INTERN"));
        CsrfToken.expose(request);
        HttpSession session = request.getSession(false);
        for (String key : List.of("successMessage", "errorMessage")) {
            request.setAttribute(key, session.getAttribute("role." + key));
            session.removeAttribute("role." + key);
        }
        request.getRequestDispatcher("/WEB-INF/views/admin/roles.jsp").forward(request, response);
    }
    @Override protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        request.setCharacterEncoding("UTF-8");
        User actor = admin(request);
        if (actor == null || !CsrfToken.valid(request)) { response.sendError(403); return; }
        try {
            long id = Long.parseLong(request.getParameter("userId"));
            boolean changed = assignments.change(actor.getUserId(), id,
                    request.getParameter("expectedRole"), request.getParameter("role"));
            request.getSession(false).setAttribute("role.successMessage", changed
                    ? "Đã cập nhật vai trò. Người dùng cần đăng nhập lại để tiếp tục."
                    : "Vai trò không thay đổi.");
        } catch (SecurityException e) { response.sendError(403); return; }
        catch (IllegalArgumentException e) {
            request.getSession(false).setAttribute("role.errorMessage",
                    e instanceof NumberFormatException ? "Mã tài khoản không hợp lệ." : e.getMessage());
        } catch (SQLException e) {
            logFailure(e); response.sendError(503); return;
        }
        response.sendRedirect(request.getContextPath() + "/admin/roles");
    }
    private void logFailure(Exception e) {
        Logger.getLogger(getClass().getName()).log(Level.SEVERE, "Role management failed", e);
    }
}
