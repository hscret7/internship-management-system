package com.internship.controller;

import com.internship.model.User;
import com.internship.service.DocumentService;
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
 * DocumentReviewServlet: Điều khiển HR phê duyệt hoặc từ chối tài liệu của TTS (US05 - BE2).
 * Hỗ trợ URL:
 * - /document/review
 * - /hr/document/review
 * - /hr/documents/review
 */
@WebServlet(name = "DocumentReviewServlet", urlPatterns = {"/document/review", "/hr/document/review", "/hr/documents/review"})
public class DocumentReviewServlet extends HttpServlet {

    private DocumentService documentService;

    @Override
    public void init() throws ServletException {
        this.documentService = new DocumentService();
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        request.setCharacterEncoding("UTF-8");
        response.setCharacterEncoding("UTF-8");

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

        String redirectUrl = request.getParameter("redirectUrl");
        boolean isAjax = "XMLHttpRequest".equalsIgnoreCase(request.getHeader("X-Requested-With"))
                || (request.getHeader("Accept") != null && request.getHeader("Accept").contains("application/json"));

        if (!DocumentService.isHR(currentUser) && !DocumentService.isAdmin(currentUser)) {
            handleError(request, response, "Bạn không có quyền duyệt tài liệu.", redirectUrl, isAjax);
            return;
        }

        if (session.getAttribute(CsrfToken.class.getName()) != null && !CsrfToken.valid(request)) {
            handleError(request, response, "Mã bảo mật biểu mẫu (CSRF) không hợp lệ hoặc đã hết hạn.", redirectUrl, isAjax);
            return;
        }

        String documentIdStr = request.getParameter("documentId");
        String action = request.getParameter("action"); // 'approve' hoặc 'reject'
        String rejectReason = request.getParameter("rejectReason");

        if (documentIdStr == null || documentIdStr.trim().isEmpty()) {
            handleError(request, response, "Thiếu mã tài liệu cần duyệt.", redirectUrl, isAjax);
            return;
        }

        int documentId;
        try {
            documentId = Integer.parseInt(documentIdStr.trim());
        } catch (NumberFormatException e) {
            handleError(request, response, "Mã tài liệu không hợp lệ.", redirectUrl, isAjax);
            return;
        }

        try {
            if ("approve".equalsIgnoreCase(action)) {
                documentService.approveDocument(documentId, currentUser);
                String msg = "Đã phê duyệt tài liệu #" + documentId + " thành công!";
                if (isAjax) {
                    writeJsonResponse(response, HttpServletResponse.SC_OK, true, msg, "APPROVED");
                } else {
                    session.setAttribute("flashSuccess", msg);
                    doRedirect(request, response, redirectUrl);
                }
            } else if ("reject".equalsIgnoreCase(action)) {
                documentService.rejectDocument(documentId, currentUser, rejectReason);
                String msg = "Đã từ chối tài liệu #" + documentId + " thành công.";
                if (isAjax) {
                    writeJsonResponse(response, HttpServletResponse.SC_OK, true, msg, "REJECTED");
                } else {
                    session.setAttribute("flashSuccess", msg);
                    doRedirect(request, response, redirectUrl);
                }
            } else {
                handleError(request, response, "Hành động duyệt tài liệu không hợp lệ (chỉ chấp nhận 'approve' hoặc 'reject').", redirectUrl, isAjax);
            }
        } catch (IllegalArgumentException | IllegalAccessException | IllegalStateException e) {
            handleError(request, response, e.getMessage(), redirectUrl, isAjax);
        } catch (SQLException e) {
            log("Lỗi CSDL khi duyệt tài liệu", e);
            handleError(request, response, "Lỗi cơ sở dữ liệu: " + e.getMessage(), redirectUrl, isAjax);
        } catch (Exception e) {
            log("Lỗi hệ thống khi duyệt tài liệu", e);
            handleError(request, response, "Lỗi xử lý duyệt tài liệu: " + e.getMessage(), redirectUrl, isAjax);
        }
    }

    private void handleError(HttpServletRequest request, HttpServletResponse response, 
                             String errorMessage, String redirectUrl, boolean isAjax) throws IOException {
        if (isAjax) {
            writeJsonResponse(response, HttpServletResponse.SC_BAD_REQUEST, false, errorMessage, null);
        } else {
            request.getSession().setAttribute("flashError", errorMessage);
            doRedirect(request, response, redirectUrl);
        }
    }

    private void doRedirect(HttpServletRequest request, HttpServletResponse response, String redirectUrl) throws IOException {
        if (redirectUrl != null && !redirectUrl.trim().isEmpty()) {
            response.sendRedirect(redirectUrl);
        } else {
            String referer = request.getHeader("Referer");
            if (referer != null && !referer.trim().isEmpty()) {
                response.sendRedirect(referer);
            } else {
                response.sendRedirect(request.getContextPath() + "/hr/interns");
            }
        }
    }

    private void writeJsonResponse(HttpServletResponse response, int statusCode, boolean success, 
                                   String message, String newStatus) throws IOException {
        response.setStatus(statusCode);
        response.setContentType("application/json;charset=UTF-8");
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        sb.append("\"success\":").append(success).append(",");
        sb.append("\"message\":\"").append(escapeJson(message)).append("\"");
        if (newStatus != null) {
            sb.append(",\"status\":\"").append(escapeJson(newStatus)).append("\"");
        }
        sb.append("}");
        response.getWriter().write(sb.toString());
    }

    private String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\b", "\\b")
                .replace("\f", "\\f")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
}
