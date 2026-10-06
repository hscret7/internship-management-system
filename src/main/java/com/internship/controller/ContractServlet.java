package com.internship.controller;

import com.internship.model.Contract;
import com.internship.model.InternProfile;
import com.internship.model.User;
import com.internship.service.DocumentService;
import com.internship.service.InternProfileService;
import com.internship.util.CsrfToken;
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

        if (currentUser.getRole() == null && session.getAttribute("currentRole") != null) {
            com.internship.model.Role r = new com.internship.model.Role();
            r.setRoleName((String) session.getAttribute("currentRole"));
            currentUser.setRole(r);
        }

        if (!DocumentService.isIntern(currentUser) && !DocumentService.isHR(currentUser) && !DocumentService.isAdmin(currentUser)) {
            session.setAttribute("flashError", "Bạn không có quyền xem hợp đồng.");
            response.sendRedirect(request.getContextPath() + "/");
            return;
        }

        CsrfToken.expose(request);

        try {
            Contract contract = null;
            String idStr = request.getParameter("id");
            if (idStr == null || idStr.trim().isEmpty()) {
                idStr = request.getParameter("contractId");
            }

            if (DocumentService.isIntern(currentUser)) {
                InternProfile profile = profileService.getProfileByUserId(currentUser.getUserId());
                if (profile != null) {
                    contract = documentService.getContractByProfileId(profile.getProfileId());
                }
                if (contract == null) {
                    contract = documentService.getContractByUserId(currentUser.getUserId());
                }
                if (contract == null && idStr != null && !idStr.trim().isEmpty()) {
                    try {
                        int cId = Integer.parseInt(idStr.trim());
                        Contract specified = documentService.getContractById(cId);
                        if (specified != null && documentService.canUserAccessContract(currentUser, specified)) {
                            contract = specified;
                        }
                    } catch (NumberFormatException ignored) {}
                }
            } else if (DocumentService.isHR(currentUser) || DocumentService.isAdmin(currentUser)) {
                if (idStr != null && !idStr.trim().isEmpty()) {
                    try {
                        contract = documentService.getContractById(Integer.parseInt(idStr.trim()));
                    } catch (NumberFormatException ignored) {}
                }
                String profileIdStr = request.getParameter("profileId");
                if (contract == null && profileIdStr != null && !profileIdStr.trim().isEmpty()) {
                    try {
                        contract = documentService.getContractByProfileId(Integer.parseInt(profileIdStr.trim()));
                    } catch (NumberFormatException ignored) {}
                }
            }

            request.setAttribute("contract", contract);
            request.getRequestDispatcher("/intern/contract.jsp").forward(request, response);
        } catch (Exception e) {
            log("Lỗi khi xem hợp đồng", e);
            request.setAttribute("contract", null);
            request.getRequestDispatcher("/intern/contract.jsp").forward(request, response);
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
            if (currentUser.getRole() == null && session.getAttribute("currentRole") != null) {
                com.internship.model.Role r = new com.internship.model.Role();
                r.setRoleName((String) session.getAttribute("currentRole"));
                currentUser.setRole(r);
            }

            if (!DocumentService.isIntern(currentUser)) {
                session.setAttribute("flashError", "Chỉ thực tập sinh mới có quyền xác nhận hợp đồng.");
                response.sendRedirect(request.getContextPath() + "/contract/view");
                return;
            }

            if (session.getAttribute(CsrfToken.class.getName()) != null && !CsrfToken.valid(request)) {
                session.setAttribute("flashError", "Mã bảo mật biểu mẫu (CSRF) không hợp lệ hoặc đã hết hạn. Vui lòng tải lại trang.");
                response.sendRedirect(request.getContextPath() + "/contract/view");
                return;
            }

            String contractIdStr = request.getParameter("contractId");
            if (contractIdStr == null || contractIdStr.trim().isEmpty()) {
                contractIdStr = request.getParameter("id");
            }

            // Tự động tìm contractId nếu không có trong param
            if (contractIdStr == null || contractIdStr.trim().isEmpty()) {
                try {
                    InternProfile p = profileService.getProfileByUserId(currentUser.getUserId());
                    Contract c = p != null ? documentService.getContractByProfileId(p.getProfileId()) : null;
                    if (c == null) {
                        c = documentService.getContractByUserId(currentUser.getUserId());
                    }
                    if (c != null) {
                        contractIdStr = String.valueOf(c.getContractId());
                    }
                } catch (Exception ignored) {}
            }

            if (contractIdStr == null || contractIdStr.trim().isEmpty()) {
                session.setAttribute("flashError", "Thiếu mã hợp đồng xác nhận.");
                response.sendRedirect(request.getContextPath() + "/contract/view");
                return;
            }

            try {
                int contractId = Integer.parseInt(contractIdStr.trim());
                documentService.confirmContract(contractId, currentUser);
                session.setAttribute("flashSuccess", "Bạn đã xác nhận hợp đồng thực tập thành công trên hệ thống!");
            } catch (IllegalAccessException | IllegalArgumentException | IllegalStateException e) {
                session.setAttribute("flashError", e.getMessage());
            } catch (SQLException e) {
                log("Lỗi CSDL khi xác nhận hợp đồng", e);
                session.setAttribute("flashError", "Lỗi CSDL: " + e.getMessage());
            } catch (Exception e) {
                log("Lỗi hệ thống khi xác nhận hợp đồng", e);
                session.setAttribute("flashError", "Lỗi khi xác nhận hợp đồng: " + e.getMessage());
            }
            response.sendRedirect(request.getContextPath() + "/contract/view");
        }
    }
}
