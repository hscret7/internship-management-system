package com.internship.controller;

import com.internship.model.InternProfile;
import com.internship.model.User;
import com.internship.service.DocumentService;
import com.internship.service.InternProfileService;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.MultipartConfig;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;

/**
 * DocumentUploadServlet: Xử lý tải lên CV, đơn xin thực tập và hợp đồng (BE2).
 * Kiểm tra định dạng (PDF/Word) và giới hạn dung lượng (tối đa 5MB).
 */
@WebServlet(name = "DocumentUploadServlet", urlPatterns = {"/document/upload", "/contract/upload"})
@MultipartConfig(
        fileSizeThreshold = 1024 * 1024 * 1, // 1 MB
        maxFileSize = 1024 * 1024 * 10,      // 10 MB
        maxRequestSize = 1024 * 1024 * 15    // 15 MB
)
public class DocumentUploadServlet extends HttpServlet {

    private DocumentService documentService;
    private InternProfileService profileService;

    @Override
    public void init() throws ServletException {
        this.documentService = new DocumentService();
        this.profileService = new InternProfileService();
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

        String path = request.getServletPath();
        String uploadDir = getServletContext().getRealPath("/uploads");
        if (uploadDir == null) {
            uploadDir = System.getProperty("user.home") + File.separator + "internship_uploads";
        }

        try {
            if ("/contract/upload".equals(path)) {
                // US9: HR Upload hợp đồng
                handleUploadContract(request, response, currentUser, uploadDir);
            } else {
                // US04: TTS Upload CV / Đơn xin thực tập
                handleUploadDocument(request, response, currentUser, uploadDir);
            }
        } catch (IllegalArgumentException e) {
            session.setAttribute("flashError", e.getMessage());
            redirectBack(request, response, path);
        } catch (SQLException e) {
            log("Lỗi CSDL khi upload file", e);
            session.setAttribute("flashError", "Lỗi CSDL: " + e.getMessage());
            redirectBack(request, response, path);
        }
    }

    /**
     * Upload CV hoặc Đơn xin thực tập (US04 - BE2).
     */
    private void handleUploadDocument(HttpServletRequest request, HttpServletResponse response, 
                                     User currentUser, String uploadDir) 
            throws SQLException, IOException, ServletException {
        InternProfile profile = profileService.getProfileByUserId(currentUser.getUserId());
        if (profile == null) {
            throw new IllegalArgumentException("Vui lòng tạo hồ sơ cá nhân trước khi tải lên tài liệu!");
        }

        String docType = request.getParameter("documentType");
        if (docType == null || docType.trim().isEmpty()) {
            docType = "CV";
        }

        Part filePart = request.getPart("file");
        documentService.saveDocument(profile.getProfileId(), docType, filePart, uploadDir);

        request.getSession().setAttribute("flashSuccess", "Tải lên " + docType + " thành công!");
        response.sendRedirect(request.getContextPath() + "/intern/profile");
    }

    /**
     * Upload Hợp đồng thực tập (US9 - HR upload).
     */
    private void handleUploadContract(HttpServletRequest request, HttpServletResponse response, 
                                     User currentUser, String uploadDir) 
            throws SQLException, IOException, ServletException {
        if (!DocumentService.isHR(currentUser) && !DocumentService.isAdmin(currentUser)) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Chỉ HR/Admin mới có quyền tải lên hợp đồng.");
            return;
        }

        String applicationIdStr = request.getParameter("applicationId");
        if (applicationIdStr == null || applicationIdStr.trim().isEmpty()) {
            throw new IllegalArgumentException("Thiếu thông tin đơn ứng tuyển (applicationId).");
        }

        int applicationId = Integer.parseInt(applicationIdStr);
        Part filePart = request.getPart("contractFile");

        documentService.saveContract(applicationId, currentUser.getUserId(), filePart, uploadDir);

        request.getSession().setAttribute("flashSuccess", "Tải lên hợp đồng thành công!");
        String redirectUrl = request.getParameter("redirectUrl");
        if (redirectUrl != null && !redirectUrl.isEmpty()) {
            response.sendRedirect(redirectUrl);
        } else {
            response.sendRedirect(request.getContextPath() + "/hr/interns");
        }
    }

    private void redirectBack(HttpServletRequest request, HttpServletResponse response, String path) throws IOException {
        String referer = request.getHeader("Referer");
        if (referer != null && !referer.isEmpty()) {
            response.sendRedirect(referer);
        } else {
            response.sendRedirect(request.getContextPath() + ("/contract/upload".equals(path) ? "/hr/interns" : "/intern/profile"));
        }
    }
}
