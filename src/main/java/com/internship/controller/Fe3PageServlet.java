package com.internship.controller;

import com.internship.model.User;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;

/** FE3 pages are server-rendered shells; data/actions come from the BE4 JSON API. */
@WebServlet({
        "/mentor/dashboard", "/mentor/tasks", "/mentor/reports", "/mentor/evaluation",
        "/intern/tasks", "/intern/tasks/detail", "/intern/reports", "/intern/evaluation",
        "/hr/evaluations", "/hr/dashboard"
})
public class Fe3PageServlet extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        HttpSession session = request.getSession(false);
        if (session == null || !(session.getAttribute("currentUser") instanceof User user)) {
            response.sendRedirect(request.getContextPath() + "/auth/login");
            return;
        }
        String path = request.getServletPath();
        String role = user.getRole() == null ? null : user.getRole().getRoleName();
        if ((path.startsWith("/mentor/") && !"MENTOR".equals(role))
                || (path.startsWith("/intern/") && !"INTERN".equals(role))
                || (path.startsWith("/hr/") && !("HR".equals(role) || "ADMIN".equals(role)))) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }
        String view = switch (path) {
            case "/mentor/dashboard" -> "/WEB-INF/views/mentor/dashboard.jsp";
            case "/mentor/tasks" -> "/WEB-INF/views/mentor/tasks.jsp";
            case "/mentor/reports" -> "/WEB-INF/views/mentor/reports.jsp";
            case "/mentor/evaluation" -> "/WEB-INF/views/mentor/evaluation.jsp";
            case "/intern/tasks" -> "/WEB-INF/views/intern/task-list.jsp";
            case "/intern/tasks/detail" -> "/WEB-INF/views/intern/task-detail.jsp";
            case "/intern/reports" -> "/WEB-INF/views/intern/weekly-report.jsp";
            case "/intern/evaluation" -> "/WEB-INF/views/intern/evaluation.jsp";
            case "/hr/evaluations" -> "/WEB-INF/views/hr/evaluation-result.jsp";
            case "/hr/dashboard" -> "/WEB-INF/views/hr/dashboard-overview.jsp";
            default -> null;
        };
        if (view == null) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        response.setHeader("Cache-Control", "no-store");
        request.getRequestDispatcher(view).forward(request, response);
    }
}
