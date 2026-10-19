package com.internship.controller;

import com.internship.model.User;
import com.internship.service.SupportRequestService;
import com.internship.util.UserRoleChecks;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.sql.SQLException;

@WebServlet({"/intern/support", "/hr/support"})
public class SupportRequestServlet extends HttpServlet {

    private SupportRequestService supportRequestService;

    public SupportRequestServlet() {}

    SupportRequestServlet(SupportRequestService supportRequestService) {
        this.supportRequestService = supportRequestService;
    }

    @Override
    public void init() {
        if (supportRequestService == null) {
            supportRequestService = new SupportRequestService();
        }
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        HttpSession session = req.getSession();
        User currentUser = (User) session.getAttribute("currentUser");

        String path = req.getServletPath();
        if (req.getParameter("error") != null) req.setAttribute("error", req.getParameter("error"));

        try {
            if ("/intern/support".equals(path)) {
                if (!UserRoleChecks.isIntern(currentUser)) {
                    resp.sendError(HttpServletResponse.SC_FORBIDDEN, "Access denied");
                    return;
                }
                req.setAttribute("requests", supportRequestService.getMyRequests(currentUser));
                req.getRequestDispatcher("/WEB-INF/views/intern/support.jsp").forward(req, resp);

            } else if ("/hr/support".equals(path)) {
                if (!UserRoleChecks.canManageHr(currentUser)) {
                    resp.sendError(HttpServletResponse.SC_FORBIDDEN, "Access denied");
                    return;
                }
                req.setAttribute("requests", supportRequestService.getAllRequests(currentUser));
                req.getRequestDispatcher("/WEB-INF/views/hr/support.jsp").forward(req, resp);
            }
        } catch (IllegalArgumentException e) {
            resp.sendError(400,e.getMessage());
        } catch (SQLException e) {
            log("Support request lookup failed",e);resp.sendError(503,"Không thể tải yêu cầu hỗ trợ lúc này.");
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        req.setCharacterEncoding("UTF-8");
        if (!com.internship.util.CsrfToken.valid(req)) {
            resp.sendError(HttpServletResponse.SC_FORBIDDEN, "Invalid CSRF Token");
            return;
        }

        HttpSession session = req.getSession();
        User currentUser = (User) session.getAttribute("currentUser");

        String path = req.getServletPath();

        try {
            if ("/intern/support".equals(path)) {
                if (!UserRoleChecks.isIntern(currentUser)) {
                    resp.sendError(HttpServletResponse.SC_FORBIDDEN, "Access denied");
                    return;
                }

                String requestType = req.getParameter("requestType");
                String title = req.getParameter("title");
                String content = req.getParameter("content");

                supportRequestService.createRequest(currentUser, requestType, title, content);
                resp.sendRedirect(req.getContextPath() + "/intern/support?success=true");

            } else if ("/hr/support".equals(path)) {
                if (!UserRoleChecks.canManageHr(currentUser)) {
                    resp.sendError(HttpServletResponse.SC_FORBIDDEN, "Access denied");
                    return;
                }

                int requestId = 0;
                try {
                    requestId = Integer.parseInt(req.getParameter("requestId"));
                } catch (NumberFormatException e) {
                    resp.sendRedirect(req.getContextPath() + "/hr/support?error=" + java.net.URLEncoder.encode("Mã yêu cầu không hợp lệ", "UTF-8"));
                    return;
                }

                String status = req.getParameter("status");
                String responseText = req.getParameter("response");

                supportRequestService.updateResponse(currentUser, requestId, status, responseText);
                resp.sendRedirect(req.getContextPath() + "/hr/support?success=true");
            }
        } catch (SQLException e) {
            log("Support update database error", e);
            resp.sendRedirect(req.getContextPath() + path + "?error=" + java.net.URLEncoder.encode("Không thể lưu yêu cầu. Vui lòng thử lại.", "UTF-8"));
        } catch (IllegalArgumentException e) {
            resp.sendRedirect(req.getContextPath() + path + "?error=" + java.net.URLEncoder.encode(e.getMessage()!=null?e.getMessage():"Dữ liệu không hợp lệ.", "UTF-8"));
        }
    }
}
