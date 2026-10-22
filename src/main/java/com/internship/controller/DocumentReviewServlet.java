package com.internship.controller;

import com.internship.model.User;
import com.internship.service.DocumentService;
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

        if (!DocumentService.isHR(currentUser) && !DocumentService.isAdmin(currentUser)) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Bạn không có quyền duyệt tài liệu.");
            return;
        }

        String documentIdStr = request.getParameter("documentId");
        String action = request.getParameter("action"); // 'approve' hoặc 'reject'
        String rejectReason = request.getParameter("rejectReason");
        String redirectUrl = request.getParameter("redirectUrl");

        boolean isAjax = "XMLHttpRequest".equalsIgnoreCase(request.getHeader("X-Requested-With"))
                || (request.getHeader("Accept") != null && request.getHeader("Accept").contains("application/json"));

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
                if (!documentService.approveDocument(documentId, currentUser)) throw new IllegalStateException("Tài liệu đã được xử lý. Vui lòng tải lại trang.");
                String msg = "Đã phê duyệt tài liệu #" + documentId + " thành công!";
                if (isAjax) {
                    writeJsonResponse(response, HttpServletResponse.SC_OK, true, msg, "APPROVED");
                } else {
                    session.setAttribute("flashSuccess", msg);
                    doRedirect(request, response, redirectUrl);
                }
            } else if ("reject".equalsIgnoreCase(action)) {
                if (!documentService.rejectDocument(documentId, currentUser, rejectReason)) throw new IllegalStateException("Tài liệu đã được xử lý. Vui lòng tải lại trang.");
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
            if (isAjax) writeJsonResponse(response, 409, false, e.getMessage(), null);
            else { session.setAttribute("flashError", e.getMessage()); doRedirect(request, response, redirectUrl); }
        } catch (IllegalArgumentException | IllegalAccessException e) {
            handleError(request, response, e.getMessage(), redirectUrl, isAjax);
        } catch (SQLException e) {
            log("Lỗi CSDL khi duyệt tài liệu", e);
            if (isAjax) writeJsonResponse(response, 503, false, "Không thể lưu quyết định. Vui lòng thử lại.", null);
            else handleError(request, response, "Không thể lưu quyết định. Vui lòng thử lại.", redirectUrl, false);
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
        if (redirectUrl != null && redirectUrl.startsWith(request.getContextPath() + "/hr/") && !redirectUrl.contains("\r") && !redirectUrl.contains("\n")) {
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
