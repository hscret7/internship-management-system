package com.internship.controller;

import com.internship.model.Document;
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
import java.sql.Date;
import java.sql.SQLException;
import java.util.List;

/**
 * InternProfileServlet: Điều khiển tạo, xem, cập nhật và tìm kiếm hồ sơ thực tập sinh (BE2).
 * Hỗ trợ URL:
 * - /intern/profile: Xem và sửa hồ sơ của TTS đăng nhập.
 * - /hr/interns: HR/Admin tìm kiếm, lọc danh sách hồ sơ.
 * - /hr/intern-detail: HR xem chi tiết hồ sơ thực tập sinh.
 */
@WebServlet(name = "InternProfileServlet", urlPatterns = {"/intern/profile", "/hr/interns", "/hr/intern-detail"})
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

        String path = request.getServletPath();

        try {
            switch (path) {
                case "/intern/profile":
                    handleViewOwnProfile(request, response, currentUser);
                    break;
                case "/hr/interns":
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

        String action = request.getParameter("action");
        if (action == null) {
            action = "save";
        }

        try {
            if ("update_status".equals(action)) {
                // HR cập nhật trạng thái hồ sơ (ACTIVE / INACTIVE)
                handleUpdateStatus(request, response, currentUser);
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
        InternProfile profile = profileService.getProfileByUserId(currentUser.getUserId());
        List<Document> documents = null;

        if (profile != null) {
            documents = documentService.getDocumentsByProfileId(profile.getProfileId());
        }

        request.setAttribute("profile", profile);
        request.setAttribute("documents", documents);
        request.getRequestDispatcher("/intern/profile.jsp").forward(request, response);
    }

    /**
     * HR/Admin xem danh sách và tìm kiếm theo tên, trường, ngành (Yêu cầu BE2).
     */
    private void handleListAndSearchProfiles(HttpServletRequest request, HttpServletResponse response, User currentUser)
            throws SQLException, ServletException, IOException {
        if (!DocumentService.isHR(currentUser) && !DocumentService.isAdmin(currentUser)) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Bạn không có quyền truy cập chức năng này.");
            return;
        }

        String name = request.getParameter("name");
        String university = request.getParameter("university");
        String major = request.getParameter("major");

        List<InternProfile> list = profileService.searchProfiles(name, university, major);

        request.setAttribute("profiles", list);
        request.setAttribute("searchName", name);
        request.setAttribute("searchUniversity", university);
        request.setAttribute("searchMajor", major);

        request.getRequestDispatcher("/hr/intern-list.jsp").forward(request, response);
    }

    /**
     * HR xem chi tiết một hồ sơ thực tập sinh kèm danh sách tài liệu.
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
        request.setAttribute("profile", profile);
        request.setAttribute("documents", documents);
        request.getRequestDispatcher("/hr/intern-detail.jsp").forward(request, response);
    }

    /**
     * Lưu (Tạo mới hoặc Cập nhật) hồ sơ thực tập sinh.
     */
    private void handleSaveProfile(HttpServletRequest request, HttpServletResponse response, User currentUser)
            throws SQLException, ServletException, IOException {
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
            if (profileIdStr != null && !profileIdStr.trim().isEmpty()) {
                // Cập nhật
                profile.setProfileId(Integer.parseInt(profileIdStr));
                profileService.updateProfile(profile);
                request.getSession().setAttribute("flashSuccess", "Cập nhật hồ sơ thành công!");
            } else {
                // Tạo mới
                profileService.createProfile(profile);
                request.getSession().setAttribute("flashSuccess", "Tạo hồ sơ thực tập sinh thành công!");
            }
            response.sendRedirect(request.getContextPath() + "/intern/profile");
        } catch (IllegalArgumentException e) {
            request.setAttribute("errorMessage", e.getMessage());
            request.setAttribute("profile", profile);
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
