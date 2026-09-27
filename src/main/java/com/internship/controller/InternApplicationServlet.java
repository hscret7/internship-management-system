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
 * InternApplicationServlet: TTS nộp đơn và xem đơn của mình.
 * Route: /intern/applications
 */
@WebServlet("/intern/applications")
public class InternApplicationServlet extends HttpServlet {

    private static final Logger LOGGER = Logger.getLogger(InternApplicationServlet.class.getName());
    private ApplicationService applicationService;

    @Override
    public void init() throws ServletException {
        try {
            applicationService = new ApplicationService(new SmtpEmailService());
        } catch (Exception e) {
            // Nếu cấu hình email thiếu, vẫn khởi tạo service nhưng log cảnh báo
            LOGGER.log(Level.WARNING,
                    "EmailService initialization failed; email notifications disabled.", e);
            applicationService = new ApplicationService(
                    (recipient, subject, content) -> {
                        throw new EmailException("Email service unavailable", e);
                    });
        }
    }

    /**
     * GET /intern/applications
     * Xem danh sách đơn của TTS hiện tại.
     *
     * Request attributes set:
     * - applications: List<Application>
     * - successMessage / errorMessage: String (optional)
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        User currentUser = (User) request.getSession().getAttribute("currentUser");
        if (!UserRoleChecks.isIntern(currentUser)) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }

        try {
            List<Application> applications = applicationService.getMyApplications(
                    currentUser.getUserId());
            request.setAttribute("applications", applications);
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error loading applications", e);
            request.setAttribute("errorMessage", "Lỗi tải danh sách đơn ứng tuyển.");
        }

        var session = request.getSession();
        for (String key : List.of("successMessage", "errorMessage")) {
            Object message = session.getAttribute(key);
            if (message != null) {
                request.setAttribute(key, message);
                session.removeAttribute(key);
            }
        }
        CsrfToken.expose(request);

        request.getRequestDispatcher("/WEB-INF/views/intern/applications.jsp")
                .forward(request, response);
    }

    /**
     * POST /intern/applications
     * Nộp đơn ứng tuyển mới.
     *
     * Parameters: (none – lấy userId từ session)
     *
     * On success: redirect với successMessage
     * On failure: redirect với errorMessage
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.setCharacterEncoding("UTF-8");
        User currentUser = (User) request.getSession().getAttribute("currentUser");
        if (!UserRoleChecks.isIntern(currentUser)) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }
        if (!CsrfToken.valid(request)) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Invalid form token");
            return;
        }

        try {
            int appId = applicationService.submitApplication(currentUser.getUserId());
            request.getSession().setAttribute("successMessage",
                    "Đã nộp đơn ứng tuyển thành công (mã đơn: " + appId + ").");
        } catch (IllegalStateException e) {
            request.getSession().setAttribute("errorMessage", e.getMessage());
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error submitting application", e);
            request.getSession().setAttribute("errorMessage",
                    "Lỗi hệ thống khi nộp đơn. Vui lòng thử lại.");
        }

        response.sendRedirect(request.getContextPath() + "/intern/applications");
    }
}
