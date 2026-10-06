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
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Bạn không có quyền duyệt tài liệu.");
            return;
        }

        if (!CsrfToken.valid(request)) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Invalid or missing CSRF token");
            return;
        }

        String documentIdStr = request.getParameter("documentId");
        String action = request.getParameter("action"); // 'approve' hoặc 'reject'
        String rejectReason = request.getParameter("rejectReason");

        if (documentIdStr == null || documentIdStr.trim().isEmpty()) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Thiếu mã tài liệu cần duyệt.");
            return;
        }

        int documentId;
        try {
            documentId = Integer.parseInt(documentIdStr.trim());
            if (documentId <= 0) {
                response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Mã tài liệu không hợp lệ.");
                return;
            }
        } catch (NumberFormatException e) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Mã tài liệu không hợp lệ.");
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
        } catch (IllegalStateException e) {
            if (isAjax) {
                writeJsonResponse(response, HttpServletResponse.SC_CONFLICT, false, e.getMessage(), null);
            } else {
                handleError(request, response, e.getMessage(), redirectUrl, isAjax);
            }
        } catch (IllegalArgumentException | IllegalAccessException e) {
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
        String safeUrl = InternProfileServlet.getSafeRedirectUrl(request, redirectUrl, null);
        if (safeUrl != null) {
            response.sendRedirect(safeUrl);
            return;
        }
        String referer = request.getHeader("Referer");
        if (referer != null && !referer.trim().isEmpty()) {
            try {
                java.net.URI refUri = java.net.URI.create(referer);
                String refPath = refUri.getPath();
                if (refPath != null && refPath.startsWith(request.getContextPath())) {
                    String query = refUri.getQuery();
                    response.sendRedirect(refPath + (query != null ? "?" + query : ""));
                    return;
                }
            } catch (Exception ignored) {}
        }
        response.sendRedirect(request.getContextPath() + "/hr/interns");
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
