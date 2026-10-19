package com.internship.controller;

import com.internship.model.Allowance;
import com.internship.model.User;
import com.internship.service.AllowanceService;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.List;

@WebServlet({"/intern/allowances", "/hr/allowances"})
public class AllowanceServlet extends HttpServlet {

    private AllowanceService allowanceService;

    public AllowanceServlet() {}

    AllowanceServlet(AllowanceService allowanceService) {
        this.allowanceService = allowanceService;
    }

    @Override
    public void init() {
        if (allowanceService == null) {
            allowanceService = new AllowanceService();
        }
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        HttpSession session = req.getSession();
        User currentUser = (User) session.getAttribute("currentUser");

        String path = req.getServletPath();

        try {
            String enrollParam = req.getParameter("enrollmentId");
            Integer enrollmentId = null;
            try {
                enrollmentId = (enrollParam != null && !enrollParam.trim().isEmpty()) ? Integer.parseInt(enrollParam) : null;
            } catch (NumberFormatException e) { }

            List<Allowance> list = allowanceService.getAllowances(currentUser, enrollmentId);
            req.setAttribute("allowances", list);

            if ("/intern/allowances".equals(path)) {
                req.getRequestDispatcher("/WEB-INF/views/intern/allowances.jsp").forward(req, resp);
            } else {
                req.getRequestDispatcher("/WEB-INF/views/hr/allowances.jsp").forward(req, resp);
            }
        } catch (SQLException | IllegalArgumentException e) {
            log("Allowance lookup failed",e);
            resp.sendError(e instanceof SQLException?503:400,"Không thể tải phụ cấp. Kiểm tra bộ lọc hoặc thử lại sau.");
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        if (!com.internship.util.CsrfToken.valid(req)) {
            resp.sendError(HttpServletResponse.SC_FORBIDDEN, "Invalid CSRF Token");
            return;
        }

        HttpSession session = req.getSession();
        User currentUser = (User) session.getAttribute("currentUser");

        String path = req.getServletPath();
        String action = req.getParameter("action");

        try {
            if ("/hr/allowances".equals(path)) {
                if ("create".equals(action)) {
                    Allowance a = new Allowance();
                    try {
                        a.setEnrollmentId(Integer.parseInt(req.getParameter("enrollmentId")));
                        a.setAmount(new BigDecimal(req.getParameter("amount")));
                    } catch (Exception e) {
                        resp.sendRedirect(req.getContextPath() + "/hr/allowances?error=" + java.net.URLEncoder.encode("Dữ liệu nhập không hợp lệ", "UTF-8"));
                        return;
                    }
                    a.setAllowanceType(req.getParameter("allowanceType"));
                    a.setPeriod(req.getParameter("period"));

                    allowanceService.createAllowance(currentUser, a);
                    resp.sendRedirect(req.getContextPath() + "/hr/allowances?success=true");
                } else if ("update_status".equals(action)) {
                    int id = 0;
                    try {
                        id = Integer.parseInt(req.getParameter("allowanceId"));
                    } catch (NumberFormatException e) {
                        resp.sendRedirect(req.getContextPath() + "/hr/allowances?error=" + java.net.URLEncoder.encode("Mã phụ cấp không hợp lệ", "UTF-8"));
                        return;
                    }
                    String status = req.getParameter("status");
                    allowanceService.updateStatus(currentUser, id, status);
                    resp.sendRedirect(req.getContextPath() + "/hr/allowances?success=true");
                } else {
                    resp.sendRedirect(req.getContextPath() + "/hr/allowances?error=" + java.net.URLEncoder.encode("Hành động không hợp lệ", "UTF-8"));
                }
            } else {
                resp.sendError(HttpServletResponse.SC_FORBIDDEN, "Not allowed.");
            }
        } catch (Exception e) {
            resp.sendRedirect(req.getContextPath() + "/hr/allowances?error=" + java.net.URLEncoder.encode(e instanceof SQLException ? "Không thể cập nhật phụ cấp lúc này." : "Dữ liệu phụ cấp không hợp lệ.", "UTF-8"));
        }
    }
}
