package com.internship.controller;

import com.internship.model.InternProfile;
import com.internship.model.User;
import com.internship.service.DocumentService;
import com.internship.service.InternProfileService;
import com.internship.util.CsrfToken;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.MultipartConfig;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

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

        if (currentUser.getRole() == null && session.getAttribute("currentRole") != null) {
            com.internship.model.Role r = new com.internship.model.Role();
            r.setRoleName((String) session.getAttribute("currentRole"));
            currentUser.setRole(r);
        }

        String path = request.getServletPath();

        if (!CsrfToken.valid(request)) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Invalid or missing CSRF token");
            return;
        }

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
        } catch (IllegalArgumentException | IllegalStateException e) {
            session.setAttribute("flashError", e.getMessage());
            redirectBack(request, response, path);
        } catch (SQLException e) {
            log("Lỗi CSDL khi upload file", e);
            session.setAttribute("flashError", "Lỗi CSDL: " + e.getMessage());
            redirectBack(request, response, path);
        } catch (Exception e) {
            log("Lỗi hệ thống khi upload file", e);
            session.setAttribute("flashError", "Lỗi khi xử lý tệp tải lên: " + e.getMessage());
            redirectBack(request, response, path);
        }
    }

    /**
     * Upload CV hoặc Đơn xin thực tập (US04 - BE2).
     */
    private void handleUploadDocument(HttpServletRequest request, HttpServletResponse response, 
                                      User currentUser, String uploadDir) 
            throws SQLException, IOException, ServletException {
        if (!DocumentService.isIntern(currentUser)) {
            request.getSession().setAttribute("flashError", "Chỉ thực tập sinh mới có quyền tải lên tài liệu cá nhân.");
            redirectBack(request, response, "/document/upload");
            return;
        }

        InternProfile profile = profileService.getProfileByUserId(currentUser.getUserId());
        if (profile == null) {
            throw new IllegalArgumentException("Vui lòng tạo hồ sơ cá nhân trước khi tải lên tài liệu!");
        }

        String docType = request.getParameter("documentType");
        if (docType == null || docType.trim().isEmpty()) {
            docType = "CV";
        } else {
            docType = docType.trim().toUpperCase();
        }

        if (!DocumentService.ALLOWED_DOCUMENT_TYPES.contains(docType)) {
            throw new IllegalArgumentException("Loại tài liệu không hợp lệ: " + docType + ". Chỉ chấp nhận: " + String.join(", ", DocumentService.ALLOWED_DOCUMENT_TYPES));
        }

        Part filePart = null;
        try {
            filePart = request.getPart("file");
        } catch (Exception ignored) {}
        if (filePart == null || filePart.getSize() == 0) {
            try {
                filePart = request.getPart("documentFile");
            } catch (Exception ignored) {}
        }

        documentService.saveDocument(profile.getProfileId(), docType, filePart, uploadDir);

        // Tự động kết nối với BE3: Khi TTS tải lên CV, nếu chưa có Application thì tự động khởi tạo đơn PENDING
        // để HR có thể thấy và xét duyệt ngay tại danh sách /hr/applications
        if ("CV".equalsIgnoreCase(docType)) {
            try {
                com.internship.dao.ApplicationDAO appDAO = new com.internship.dao.ApplicationDAO();
                List<com.internship.model.Application> apps = appDAO.getApplicationsByInternId(profile.getProfileId());
                if (apps == null || apps.isEmpty()) {
                    appDAO.createApplication(profile.getProfileId());
                }
            } catch (Exception e) {
                log("Không thể tự động khởi tạo đơn ứng tuyển sau khi tải CV", e);
            }
        }

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
            request.getSession().setAttribute("flashError", "Chỉ HR/Admin mới có quyền tải lên hợp đồng.");
            redirectBack(request, response, "/contract/upload");
            return;
        }

        String applicationIdStr = request.getParameter("applicationId");
        if (applicationIdStr == null || applicationIdStr.trim().isEmpty()) {
            applicationIdStr = request.getParameter("id");
        }

        String profileIdStr = request.getParameter("profileId");
        int profileId = 0;
        if (profileIdStr != null && !profileIdStr.trim().isEmpty()) {
            try {
                profileId = Integer.parseInt(profileIdStr.trim());
            } catch (NumberFormatException ignored) {}
        }
        if (profileId <= 0) {
            String redirectUrlParam = request.getParameter("redirectUrl");
            if (redirectUrlParam != null && redirectUrlParam.contains("id=")) {
                try {
                    String part = redirectUrlParam.substring(redirectUrlParam.indexOf("id=") + 3);
                    int amp = part.indexOf('&');
                    if (amp != -1) part = part.substring(0, amp);
                    profileId = Integer.parseInt(part.trim());
                } catch (Exception ignored) {}
            }
        }

        int applicationId = 0;
        if (applicationIdStr != null && !applicationIdStr.trim().isEmpty()) {
            try {
                applicationId = Integer.parseInt(applicationIdStr.trim());
            } catch (NumberFormatException ignored) {}
        }

        // Tự động tìm applicationId nếu chưa có nhưng có profileId
        if (applicationId <= 0 && profileId > 0) {
            try {
                com.internship.dao.ApplicationDAO appDAO = new com.internship.dao.ApplicationDAO();
                List<com.internship.model.Application> apps = appDAO.getApplicationsByInternId(profileId);
                if (apps != null && !apps.isEmpty()) {
                    applicationId = apps.stream()
                            .filter(a -> "APPROVED".equalsIgnoreCase(a.getStatus()))
                            .map(com.internship.model.Application::getApplicationId)
                            .findFirst()
                            .orElse(apps.get(0).getApplicationId());
                }
            } catch (Exception ignored) {}
        }

        // Fallback: Nếu vẫn chưa có applicationId, gán applicationId = profileId
        if (applicationId <= 0 && profileId > 0) {
            applicationId = profileId;
        }

        if (applicationId <= 0) {
            throw new IllegalArgumentException("Thiếu thông tin đơn ứng tuyển (applicationId). Vui lòng kiểm tra lại ID ứng viên.");
        }

        Part filePart = null;
        try {
            filePart = request.getPart("contractFile");
        } catch (Exception ignored) {}
        if (filePart == null || filePart.getSize() == 0) {
            try {
                filePart = request.getPart("file");
            } catch (Exception ignored) {}
        }

        documentService.saveContract(applicationId, currentUser.getUserId(), filePart, uploadDir);

        request.getSession().setAttribute("flashSuccess", "Tải lên hợp đồng thành công!");
        String redirectUrl = request.getParameter("redirectUrl");
        String safeRedirect = InternProfileServlet.getSafeRedirectUrl(request, redirectUrl, request.getContextPath() + "/hr/interns");
        response.sendRedirect(safeRedirect);
    }

    private void redirectBack(HttpServletRequest request, HttpServletResponse response, String path) throws IOException {
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
        response.sendRedirect(request.getContextPath() + ("/contract/upload".equals(path) ? "/hr/interns" : "/intern/profile"));
    }
}
