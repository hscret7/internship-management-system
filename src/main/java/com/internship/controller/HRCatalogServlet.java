package com.internship.controller;

import com.internship.model.User;
import com.internship.service.CatalogService;
import com.internship.util.CsrfToken;
import com.internship.util.UserRoleChecks;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.sql.Date;
import java.sql.SQLException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/** Minimal HR setup for departments, programs and mentor records. */
@WebServlet("/hr/catalog")
public class HRCatalogServlet extends HttpServlet {
    private static final Logger LOGGER = Logger.getLogger(HRCatalogServlet.class.getName());
    private CatalogService catalog;

    @Override
    public void init() {
        catalog = new CatalogService();
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        if (!isHR(request)) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }
        CsrfToken.expose(request);
        try {
            request.setAttribute("departments", catalog.getDepartments());
            request.setAttribute("programs", catalog.getPrograms());
            request.setAttribute("mentors", catalog.getMentors());
            request.setAttribute("mentorUsers", catalog.getEligibleMentorUsers());
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Unable to load HR catalog", e);
            request.setAttribute("errorMessage", "Không thể tải dữ liệu thiết lập.");
        }
        var session = request.getSession();
        for (String key : List.of("successMessage", "errorMessage")) {
            Object message = session.getAttribute(key);
            if (message != null) {
                request.setAttribute(key, message);
                session.removeAttribute(key);
            }
        }
        request.getRequestDispatcher("/WEB-INF/views/hr/catalog.jsp").forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.setCharacterEncoding("UTF-8");
        if (!isHR(request) || !CsrfToken.valid(request)) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }
        try {
            String action = request.getParameter("action");
            if ("create-department".equals(action)) {
                catalog.createDepartment(request.getParameter("name"),
                        request.getParameter("description"));
            } else if ("create-program".equals(action)) {
                catalog.createProgram(request.getParameter("name"),
                        positiveInt(request, "departmentId"), request.getParameter("description"),
                        dateParam(request, "startDate"),
                        dateParam(request, "endDate"),
                        nonnegativeInt(request, "maxInterns"));
            } else if ("activate-program".equals(action)) {
                catalog.activateProgram(positiveInt(request, "programId"));
            } else if ("create-mentor".equals(action)) {
                catalog.createMentor(positiveLong(request, "userId"),
                        positiveInt(request, "departmentId"),
                        request.getParameter("specialization"),
                        positiveInt(request, "maxMentees"));
            } else {
                throw new IllegalArgumentException("Hành động không hợp lệ.");
            }
            request.getSession().setAttribute("successMessage", "Đã lưu thay đổi thành công.");
        } catch (IllegalArgumentException | IllegalStateException e) {
            request.getSession().setAttribute("errorMessage", e.getMessage());
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Unable to update HR catalog", e);
            request.getSession().setAttribute("errorMessage", "Không thể lưu thay đổi. Kiểm tra dữ liệu trùng.");
        }
        response.sendRedirect(request.getContextPath() + "/hr/catalog");
    }

    private boolean isHR(HttpServletRequest request) {
        Object user = request.getSession(false) == null ? null
                : request.getSession(false).getAttribute("currentUser");
        return user instanceof User currentUser && UserRoleChecks.canManageHr(currentUser);
    }

    private int positiveInt(HttpServletRequest request, String name) {
        try {
            int value = Integer.parseInt(request.getParameter(name));
            if (value > 0) return value;
        } catch (NumberFormatException ignored) { }
        throw new IllegalArgumentException("Tham số không hợp lệ: " + name);
    }

    private int nonnegativeInt(HttpServletRequest request, String name) {
        try {
            int value = Integer.parseInt(request.getParameter(name));
            if (value >= 0) return value;
        } catch (NumberFormatException ignored) { }
        throw new IllegalArgumentException("Tham số không hợp lệ: " + name);
    }

    private long positiveLong(HttpServletRequest request, String name) {
        try {
            long value = Long.parseLong(request.getParameter(name));
            if (value > 0) return value;
        } catch (NumberFormatException ignored) { }
        throw new IllegalArgumentException("Tham số không hợp lệ: " + name);
    }

    private Date dateParam(HttpServletRequest request, String name) {
        String value = request.getParameter(name);
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Thiếu ngày: " + name);
        }
        try {
            return Date.valueOf(value);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Ngày không hợp lệ: " + name);
        }
    }
}
