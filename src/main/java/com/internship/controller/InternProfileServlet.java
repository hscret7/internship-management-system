package com.internship.controller;

import com.internship.model.Contract;
import com.internship.model.Document;
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
import java.sql.Date;
import java.sql.SQLException;
import java.util.List;

/**
 * InternProfileServlet: Điều khiển tạo, xem, cập nhật và tìm kiếm hồ sơ thực tập sinh (BE2).
 * Hỗ trợ URL:
 * - /intern/profile: Xem và sửa hồ sơ của TTS đăng nhập.
 * - /hr/interns, /hr/interns/search: HR/Admin tìm kiếm, lọc danh sách hồ sơ (US03).
 * - /hr/intern-detail: HR xem chi tiết hồ sơ thực tập sinh.
 * - /hr/interns/edit: HR/Admin cập nhật thông tin hồ sơ TTS theo whitelist (US02).
 */
@WebServlet(name = "InternProfileServlet", urlPatterns = {"/intern/profile", "/hr/interns", "/hr/interns/search", "/hr/intern-detail", "/hr/interns/edit"})
public class InternProfileServlet extends HttpServlet {

    private InternProfileService profileService;
    private DocumentService documentService;

    @Override
    public void init() throws ServletException {
        this.profileService = new InternProfileService();
        this.documentService = new DocumentService();
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) 
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

        CsrfToken.expose(request);

        String path = request.getServletPath();

        try {
            switch (path) {
                case "/intern/profile":
                    handleViewOwnProfile(request, response, currentUser);
                    break;
                case "/hr/interns":
                case "/hr/interns/search":
                    handleListAndSearchProfiles(request, response, currentUser);
                    break;
                case "/hr/intern-detail":
                    handleViewProfileDetail(request, response, currentUser);
                    break;
                default:
                    response.sendError(HttpServletResponse.SC_NOT_FOUND);
                    break;
            }
        } catch (SQLException e) {
            log("Lỗi CSDL trong InternProfileServlet", e);
            request.setAttribute("errorMessage", "Lỗi cơ sở dữ liệu: " + e.getMessage());
            request.getRequestDispatcher("/error/500.jsp").forward(request, response);
        }
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

        if (!CsrfToken.valid(request)) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Invalid form token");
            return;
        }

        String path = request.getServletPath();
        String action = request.getParameter("action");
        if (action == null) {
            action = "save";
        }

        try {
            if ("update_status".equals(action)) {
                // HR cập nhật trạng thái hồ sơ (ACTIVE / INACTIVE)
                handleUpdateStatus(request, response, currentUser);
            } else if ("/hr/interns/edit".equals(path) || "hr_edit".equals(action) || "edit_profile".equals(action)) {
                // US02: HR/Admin chỉnh sửa hồ sơ TTS theo whitelist
                handleHrEditProfile(request, response, currentUser);
            } else {
                // TTS tạo hoặc cập nhật hồ sơ cá nhân
                handleSaveProfile(request, response, currentUser);
            }
        } catch (SQLException e) {
            log("Lỗi CSDL khi lưu hồ sơ", e);
            request.setAttribute("errorMessage", "Lỗi lưu CSDL: " + e.getMessage());
            doGet(request, response);
        }
    }

    /**
     * Xem hồ sơ cá nhân của TTS đăng nhập.
     */
    private void handleViewOwnProfile(HttpServletRequest request, HttpServletResponse response, User currentUser)
            throws SQLException, ServletException, IOException {
        if (!DocumentService.isIntern(currentUser)) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Chỉ thực tập sinh mới có quyền xem hồ sơ cá nhân.");
            return;
        }
        InternProfile profile = profileService.getProfileByUserId(currentUser.getUserId());
        List<Document> documents = null;
        com.internship.model.Application application = null;

        if (profile != null) {
            documents = documentService.getDocumentsByProfileId(profile.getProfileId());
            try {
                com.internship.dao.ApplicationDAO appDAO = new com.internship.dao.ApplicationDAO();
                List<com.internship.model.Application> apps = appDAO.getApplicationsByInternId(profile.getProfileId());
                if (apps != null && !apps.isEmpty()) {
                    application = apps.get(0);
                }
            } catch (Exception ignored) {}
        } else {
            // TTS mới đăng ký chưa có hồ sơ: tự động khởi tạo dữ liệu mặc định từ currentUser
            // để không bắt TTS gõ lại Họ tên và Email khi đăng nhập lần đầu
            profile = new InternProfile();
            profile.setUserId(currentUser.getUserId());
            profile.setFullName(currentUser.getFullName());
            profile.setEmail(currentUser.getEmail());
            profile.setStatus("ACTIVE");
        }

        request.setAttribute("profile", profile);
        request.setAttribute("documents", documents);
        request.setAttribute("application", application);
        request.getRequestDispatcher("/intern/profile.jsp").forward(request, response);
    }

    /**
     * HR/Admin xem danh sách và tìm kiếm theo tên, trường, ngành (Yêu cầu BE2 - US03).
     */
    private void handleListAndSearchProfiles(HttpServletRequest request, HttpServletResponse response, User currentUser)
            throws SQLException, ServletException, IOException {
        if (!DocumentService.isHR(currentUser) && !DocumentService.isAdmin(currentUser)) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Bạn không có quyền truy cập chức năng này.");
            return;
        }

        String name = request.getParameter("name");
        String keyword = request.getParameter("keyword");
        if ((name == null || name.trim().isEmpty()) && keyword != null && !keyword.trim().isEmpty()) {
            name = keyword;
        }
        String university = request.getParameter("university");
        String major = request.getParameter("major");
        String status = request.getParameter("status");

        List<InternProfile> list = profileService.searchProfiles(name, university, major, status);

        request.setAttribute("profiles", list);
        request.setAttribute("searchName", name);
        request.setAttribute("keyword", name);
        request.setAttribute("searchUniversity", university);
        request.setAttribute("searchMajor", major);
        request.setAttribute("searchStatus", status);

        request.getRequestDispatcher("/hr/intern-list.jsp").forward(request, response);
    }

    /**
     * HR xem chi tiết một hồ sơ thực tập sinh kèm danh sách tài liệu và hợp đồng (US02, US05, US09).
     */
    private void handleViewProfileDetail(HttpServletRequest request, HttpServletResponse response, User currentUser)
            throws SQLException, ServletException, IOException {
        if (!DocumentService.isHR(currentUser) && !DocumentService.isAdmin(currentUser)) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Bạn không có quyền xem chi tiết hồ sơ.");
            return;
        }

        String idStr = request.getParameter("id");
        if (idStr == null || idStr.isEmpty()) {
            response.sendRedirect(request.getContextPath() + "/hr/interns");
            return;
        }

        int profileId = Integer.parseInt(idStr);
        InternProfile profile = profileService.getProfileById(profileId);
        if (profile == null) {
            request.setAttribute("errorMessage", "Không tìm thấy hồ sơ ID: " + profileId);
            request.getRequestDispatcher("/error/404.jsp").forward(request, response);
            return;
        }

        List<Document> documents = documentService.getDocumentsByProfileId(profileId);
        Contract contract = documentService.getContractByProfileId(profileId);
        com.internship.model.Application application = null;
        try {
            com.internship.dao.ApplicationDAO appDAO = new com.internship.dao.ApplicationDAO();
            List<com.internship.model.Application> apps = appDAO.getApplicationsByInternId(profileId);
            if (apps != null && !apps.isEmpty()) {
                application = apps.stream()
                        .filter(a -> "APPROVED".equalsIgnoreCase(a.getStatus()))
                        .findFirst()
                        .orElse(apps.get(0));
            }
        } catch (Exception ignored) {}

        request.setAttribute("profile", profile);
        request.setAttribute("documents", documents);
        request.setAttribute("contract", contract);
        request.setAttribute("application", application);
        request.getRequestDispatcher("/hr/intern-detail.jsp").forward(request, response);
    }

    /**
     * US02: HR/Admin chỉnh sửa hồ sơ TTS theo whitelist bảo mật.
     * Quy ước Sprint 2:
     * - Whitelist: fullName, dob, gender, phone, email, university, major, gpa, skills.
     * - Không nhận userId, role, trạng thái Application từ form.
     * - profileId phải tồn tại.
     */
    private void handleHrEditProfile(HttpServletRequest request, HttpServletResponse response, User currentUser)
            throws SQLException, IOException {
        if (!DocumentService.isHR(currentUser) && !DocumentService.isAdmin(currentUser)) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Bạn không có quyền chỉnh sửa hồ sơ TTS.");
            return;
        }

        String profileIdStr = request.getParameter("profileId");
        if (profileIdStr == null || profileIdStr.trim().isEmpty()) {
            request.getSession().setAttribute("flashError", "Thiếu mã hồ sơ (profileId).");
            response.sendRedirect(request.getContextPath() + "/hr/interns");
            return;
        }

        int profileId;
        try {
            profileId = Integer.parseInt(profileIdStr.trim());
        } catch (NumberFormatException e) {
            request.getSession().setAttribute("flashError", "Mã hồ sơ không hợp lệ.");
            response.sendRedirect(request.getContextPath() + "/hr/interns");
            return;
        }

        String fullName = request.getParameter("fullName");
        String dobStr = request.getParameter("dob");
        String gender = request.getParameter("gender");
        String phone = request.getParameter("phone");
        String email = request.getParameter("email");
        String university = request.getParameter("university");
        String major = request.getParameter("major");
        String gpaStr = request.getParameter("gpa");
        String skills = request.getParameter("skills");

        InternProfile input = new InternProfile();
        input.setProfileId(profileId);
        input.setFullName(fullName != null ? fullName.trim() : null);
        input.setGender(gender != null ? gender.trim() : null);
        input.setPhone(phone != null ? phone.trim() : null);
        input.setEmail(email != null ? email.trim() : null);
        input.setUniversity(university != null ? university.trim() : null);
        input.setMajor(major != null ? major.trim() : null);
        input.setSkills(skills);

        if (dobStr != null && !dobStr.trim().isEmpty()) {
            try {
                input.setDob(Date.valueOf(dobStr.trim()));
            } catch (IllegalArgumentException ignored) {
            }
        }

        if (gpaStr != null && !gpaStr.trim().isEmpty()) {
            try {
                input.setGpa(Double.parseDouble(gpaStr.trim()));
            } catch (NumberFormatException ignored) {
            }
        }

        try {
            profileService.updateProfileByHr(input, currentUser);
            request.getSession().setAttribute("flashSuccess", "Cập nhật hồ sơ thực tập sinh thành công!");
            String redirectUrl = request.getParameter("redirectUrl");
            if (redirectUrl != null && !redirectUrl.trim().isEmpty()) {
                response.sendRedirect(redirectUrl);
            } else {
                response.sendRedirect(request.getContextPath() + "/hr/intern-detail?id=" + profileId);
            }
        } catch (IllegalArgumentException | IllegalAccessException e) {
            request.getSession().setAttribute("flashError", e.getMessage());
            response.sendRedirect(request.getContextPath() + "/hr/intern-detail?id=" + profileId);
        }
    }

    /**
     * Lưu (Tạo mới hoặc Cập nhật) hồ sơ thực tập sinh (chỉ INTERN sở hữu mới có quyền).
     */
    private void handleSaveProfile(HttpServletRequest request, HttpServletResponse response, User currentUser)
            throws SQLException, ServletException, IOException {
        if (!DocumentService.isIntern(currentUser)) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Chỉ thực tập sinh mới có quyền tạo hoặc chỉnh sửa hồ sơ cá nhân.");
            return;
        }

        String profileIdStr = request.getParameter("profileId");
        String fullName = request.getParameter("fullName");
        String dobStr = request.getParameter("dob");
        String gender = request.getParameter("gender");
        String phone = request.getParameter("phone");
        String email = request.getParameter("email");
        String university = request.getParameter("university");
        String major = request.getParameter("major");
        String gpaStr = request.getParameter("gpa");
        String skills = request.getParameter("skills");

        InternProfile profile = new InternProfile();
        profile.setUserId(currentUser.getUserId());
        profile.setFullName(fullName != null ? fullName.trim() : null);
        profile.setGender(gender);
        profile.setPhone(phone != null ? phone.trim() : null);
        profile.setEmail(email != null ? email.trim() : null);
        profile.setUniversity(university != null ? university.trim() : null);
        profile.setMajor(major != null ? major.trim() : null);
        profile.setSkills(skills);
        profile.setStatus("ACTIVE");

        if (dobStr != null && !dobStr.trim().isEmpty()) {
            try {
                profile.setDob(Date.valueOf(dobStr.trim()));
            } catch (IllegalArgumentException ignored) {
            }
        }

        if (gpaStr != null && !gpaStr.trim().isEmpty()) {
            try {
                profile.setGpa(Double.parseDouble(gpaStr.trim()));
            } catch (NumberFormatException ignored) {
            }
        }

        try {
            InternProfile ownProfile = profileService.getProfileByUserId(currentUser.getUserId());
            if (profileIdStr != null && !profileIdStr.trim().isEmpty() && !"0".equals(profileIdStr.trim())) {
                int profileId = Integer.parseInt(profileIdStr.trim());
                if (ownProfile == null || ownProfile.getProfileId() != profileId) {
                    response.sendError(HttpServletResponse.SC_FORBIDDEN, "Bạn không có quyền chỉnh sửa hồ sơ của người khác.");
                    return;
                }
                profile.setProfileId(profileId);
                profileService.updateProfile(profile, currentUser);
                request.getSession().setAttribute("flashSuccess", "Cập nhật hồ sơ thành công!");
            } else {
                if (ownProfile != null) {
                    profile.setProfileId(ownProfile.getProfileId());
                    profileService.updateProfile(profile, currentUser);
                    request.getSession().setAttribute("flashSuccess", "Cập nhật hồ sơ thành công!");
                } else {
                    profileService.createProfile(profile, currentUser);
                    request.getSession().setAttribute("flashSuccess", "Tạo hồ sơ thực tập sinh thành công!");
                }
            }
            response.sendRedirect(request.getContextPath() + "/intern/profile");
        } catch (IllegalArgumentException | IllegalAccessException e) {
            request.setAttribute("errorMessage", e.getMessage());
            request.setAttribute("profile", profile);
            try {
                InternProfile ownProfile = profileService.getProfileByUserId(currentUser.getUserId());
                if (ownProfile != null) {
                    request.setAttribute("documents", documentService.getDocumentsByProfileId(ownProfile.getProfileId()));
                }
            } catch (Exception ignored) {}
            request.getRequestDispatcher("/intern/profile.jsp").forward(request, response);
        }
    }

    /**
     * HR cập nhật trạng thái hồ sơ.
     */
    private void handleUpdateStatus(HttpServletRequest request, HttpServletResponse response, User currentUser)
            throws SQLException, IOException {
        if (!DocumentService.isHR(currentUser) && !DocumentService.isAdmin(currentUser)) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }

        int profileId = Integer.parseInt(request.getParameter("profileId"));
        String status = request.getParameter("status"); // ACTIVE hoặc INACTIVE
        profileService.updateStatus(profileId, status);

        response.sendRedirect(request.getContextPath() + "/hr/intern-detail?id=" + profileId);
    }
}
