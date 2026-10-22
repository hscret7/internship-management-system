package com.internship.controller;

import com.internship.model.Contract;
import com.internship.model.InternProfile;
import com.internship.model.User;
import com.internship.service.DocumentService;
import com.internship.service.InternProfileService;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.sql.SQLException;

/**
 * ContractServlet: Điều khiển xem hợp đồng và TTS xác nhận hợp đồng (US10).
 */
@WebServlet(name = "ContractServlet", urlPatterns = {"/contract/view", "/contract/confirm"})
public class ContractServlet extends HttpServlet {

    private DocumentService documentService;
    private InternProfileService profileService;

    @Override
    public void init() throws ServletException {
        this.documentService = new DocumentService();
        this.profileService = new InternProfileService();
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        HttpSession session = request.getSession(false);
        User currentUser = null;
        if (session != null) {
            currentUser = (User) session.getAttribute("currentUser");
            if (currentUser == null) {
                currentUser = (User) session.getAttribute("user");
            }
        }

        if (currentUser == null) {
            response.sendRedirect(request.getContextPath() + "/auth/login");
            return;
        }

        try {
            Contract contract = null;
            if (DocumentService.isIntern(currentUser)) {
                InternProfile profile = profileService.getProfileByUserId(currentUser.getUserId());
                if (profile != null) {
                    contract = documentService.getContractByProfileId(profile.getProfileId());
                }
            } else if (DocumentService.isHR(currentUser) || DocumentService.isAdmin(currentUser)) {
                String idStr = request.getParameter("id");
                if (idStr != null && !idStr.isEmpty()) {
                    contract = documentService.getContractById(Integer.parseInt(idStr));
                }
            }

            request.setAttribute("contract", contract);
            request.getRequestDispatcher("/intern/contract.jsp").forward(request, response);
        } catch (NumberFormatException e) {
            response.sendError(400, "Mã hợp đồng không hợp lệ.");
        } catch (SQLException e) {
            log("Lỗi CSDL khi xem hợp đồng", e);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Lỗi truy vấn cơ sở dữ liệu.");
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        HttpSession session = request.getSession(false);
        User currentUser = null;
        if (session != null) {
            currentUser = (User) session.getAttribute("currentUser");
            if (currentUser == null) {
                currentUser = (User) session.getAttribute("user");
            }
        }

        if (currentUser == null) {
            response.sendRedirect(request.getContextPath() + "/auth/login");
            return;
        }

        String path = request.getServletPath();
        if ("/contract/confirm".equals(path)) {
            String contractIdStr = request.getParameter("contractId");
            if (contractIdStr == null || contractIdStr.trim().isEmpty()) {
                session.setAttribute("flashError", "Thiếu mã hợp đồng xác nhận.");
                response.sendRedirect(request.getContextPath() + "/contract/view");
                return;
            }

            try {
                int contractId = Integer.parseInt(contractIdStr);
                String version = request.getParameter("fileVersion");
                if (version == null || !version.matches("[0-9a-f]{64}")) throw new IllegalArgumentException("Vui lòng tải lại trang hợp đồng trước khi xác nhận.");
                if (!documentService.confirmContract(contractId, currentUser, version)) throw new IllegalArgumentException("Hợp đồng đã thay đổi. Vui lòng tải lại.");
                session.setAttribute("flashSuccess", "Bạn đã xác nhận hợp đồng thực tập thành công trên hệ thống!");
            } catch (IllegalAccessException | IllegalArgumentException e) {
                session.setAttribute("flashError", e.getMessage());
            } catch (SQLException e) {
                log("Lỗi CSDL khi xác nhận hợp đồng", e);
                session.setAttribute("flashError", "Không thể xác nhận hợp đồng. Vui lòng thử lại.");
            }
            response.sendRedirect(request.getContextPath() + "/contract/view");
        }
    }
}
