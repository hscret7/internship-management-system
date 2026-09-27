package com.internship.controller;

import com.internship.model.Application;
import com.internship.model.User;
import com.internship.service.ApplicationService;
import com.internship.service.email.EmailException;
import com.internship.service.email.SmtpEmailService;
import com.internship.util.CsrfToken;
import com.internship.util.UserRoleChecks;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * HRApplicationServlet: HR xem danh sách đơn, xem chi tiết, duyệt/từ chối.
 * Route: /hr/applications
 */
@WebServlet("/hr/applications")
public class HRApplicationServlet extends HttpServlet {

    private static final Logger LOGGER = Logger.getLogger(HRApplicationServlet.class.getName());
    private ApplicationService applicationService;

    @Override
    public void init() throws ServletException {
        try {
            applicationService = new ApplicationService(new SmtpEmailService());
        } catch (Exception e) {
            LOGGER.log(Level.WARNING,
                    "EmailService initialization failed; email notifications disabled.", e);
            applicationService = new ApplicationService(
                    (recipient, subject, content) -> {
                        throw new EmailException("Email service unavailable", e);
                    });
        }
    }

    /**
     * GET /hr/applications
     * Xem danh sách đơn chờ duyệt hoặc chi tiết đơn.
     *
     * Query params:
     * - action=detail&id=<applicationId>  → chi tiết đơn
     * - action=all                        → tất cả đơn
     * - (mặc định)                        → đơn PENDING
     *
     * Request attributes set:
     * - applications: List<Application> (for list view)
     * - application: Application (for detail view)
     * - viewMode: "list" | "detail"
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        User currentUser = (User) request.getSession().getAttribute("currentUser");
        if (!UserRoleChecks.canManageHr(currentUser)) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }

        String action = request.getParameter("action");
        CsrfToken.expose(request);

        try {
            if ("detail".equals(action)) {
                handleDetail(request, response);
                return;
            }

            List<Application> applications;
            if ("all".equals(action)) {
                applications = applicationService.getAllApplications();
            } else {
                applications = applicationService.getPendingApplications();
            }
            request.setAttribute("applications", applications);
            request.setAttribute("viewMode", "list");
            request.setAttribute("filterAction", action != null ? action : "pending");

        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error loading applications", e);
            request.setAttribute("errorMessage", "Lỗi tải danh sách đơn ứng tuyển.");
        }

        // Chuyển session message sang request
        transferSessionMessages(request);

        request.getRequestDispatcher("/WEB-INF/views/hr/applications.jsp")
                .forward(request, response);
    }

    /**
     * POST /hr/applications
     * Duyệt hoặc từ chối đơn.
     *
     * Parameters:
     * - action: "approve" | "reject"
     * - applicationId: int
     * - rejectReason: String (bắt buộc khi reject)
     *
     * On success: redirect với successMessage
     * On failure: redirect với errorMessage
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.setCharacterEncoding("UTF-8");
        User currentUser = (User) request.getSession().getAttribute("currentUser");
        if (!UserRoleChecks.canManageHr(currentUser)) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }
        if (!CsrfToken.valid(request)) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Invalid form token");
            return;
        }

        String action = request.getParameter("action");
        String appIdStr = request.getParameter("applicationId");

        if (appIdStr == null || appIdStr.isBlank()) {
            request.getSession().setAttribute("errorMessage", "Thiếu mã đơn ứng tuyển.");
            response.sendRedirect(request.getContextPath() + "/hr/applications");
            return;
        }

        int applicationId;
        try {
            applicationId = Integer.parseInt(appIdStr.trim());
        } catch (NumberFormatException e) {
            request.getSession().setAttribute("errorMessage", "Mã đơn không hợp lệ.");
            response.sendRedirect(request.getContextPath() + "/hr/applications");
            return;
        }

        try {
            if ("approve".equals(action)) {
                applicationService.approveApplication(applicationId, currentUser.getUserId());
                request.getSession().setAttribute("successMessage",
                        "Đã duyệt đơn ứng tuyển #" + applicationId + " thành công.");

            } else if ("reject".equals(action)) {
                String rejectReason = request.getParameter("rejectReason");
                applicationService.rejectApplication(
                        applicationId, currentUser.getUserId(), rejectReason);
                request.getSession().setAttribute("successMessage",
                        "Đã từ chối đơn ứng tuyển #" + applicationId + ".");

            } else {
                request.getSession().setAttribute("errorMessage", "Hành động không hợp lệ.");
            }
        } catch (IllegalStateException | IllegalArgumentException e) {
            request.getSession().setAttribute("errorMessage", e.getMessage());
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error processing application", e);
            request.getSession().setAttribute("errorMessage",
                    "Lỗi hệ thống khi xử lý đơn. Vui lòng thử lại.");
        }

        response.sendRedirect(request.getContextPath() + "/hr/applications");
    }

    // --- Private helpers ---

    private void handleDetail(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String idStr = request.getParameter("id");
        if (idStr == null || idStr.isBlank()) {
            request.setAttribute("errorMessage", "Thiếu mã đơn ứng tuyển.");
            request.setAttribute("viewMode", "list");
            request.getRequestDispatcher("/WEB-INF/views/hr/applications.jsp")
                    .forward(request, response);
            return;
        }

        try {
            int applicationId = Integer.parseInt(idStr.trim());
            Application app = applicationService.getApplicationById(applicationId);
            if (app == null) {
                request.setAttribute("errorMessage", "Không tìm thấy đơn ứng tuyển.");
                request.setAttribute("viewMode", "list");
            } else {
                request.setAttribute("application", app);
                request.setAttribute("viewMode", "detail");
            }
        } catch (NumberFormatException e) {
            request.setAttribute("errorMessage", "Mã đơn không hợp lệ.");
            request.setAttribute("viewMode", "list");
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error loading application detail", e);
            request.setAttribute("errorMessage", "Lỗi tải chi tiết đơn ứng tuyển.");
            request.setAttribute("viewMode", "list");
        }

        request.getRequestDispatcher("/WEB-INF/views/hr/applications.jsp")
                .forward(request, response);
    }

    private void transferSessionMessages(HttpServletRequest request) {
        var session = request.getSession();
        String success = (String) session.getAttribute("successMessage");
        String error = (String) session.getAttribute("errorMessage");
        if (success != null) {
            request.setAttribute("successMessage", success);
            session.removeAttribute("successMessage");
        }
        if (error != null) {
            request.setAttribute("errorMessage", error);
            session.removeAttribute("errorMessage");
        }
    }
}
