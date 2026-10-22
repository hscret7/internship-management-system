package com.internship.controller;

import com.internship.model.*;
import com.internship.dao.InternshipProgramDAO;
import com.internship.dao.MentorDAO;
import com.internship.service.EnrollmentService;
import com.internship.util.CsrfToken;
import com.internship.util.UserRoleChecks;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * HREnrollmentServlet: HR ghi danh, phân mentor, đổi mentor.
 * Route: /hr/enrollments
 */
@WebServlet("/hr/enrollments")
public class HREnrollmentServlet extends HttpServlet {

    private static final Logger LOGGER = Logger.getLogger(HREnrollmentServlet.class.getName());
    private EnrollmentService enrollmentService;
    private InternshipProgramDAO programDAO;
    private MentorDAO mentorDAO;
    private final com.internship.service.ApprovedApplicationLookupService applicationLookup = new com.internship.service.ApprovedApplicationLookupService();

    public HREnrollmentServlet() { }

    HREnrollmentServlet(EnrollmentService service, InternshipProgramDAO programs, MentorDAO mentors) {
        enrollmentService = service;
        programDAO = programs;
        mentorDAO = mentors;
    }

    @Override
    public void init() throws ServletException {
        if (enrollmentService == null) enrollmentService = new EnrollmentService();
        if (programDAO == null) programDAO = new InternshipProgramDAO();
        if (mentorDAO == null) mentorDAO = new MentorDAO();
    }

    /**
     * GET /hr/enrollments
     *
     * Query params:
     * - action=detail&id=<enrollmentId>     → chi tiết enrollment + mentor history
     * - action=enroll-form&applicationId=<> → form chọn chương trình
     * - action=assign-form&enrollmentId=<>  → form chọn mentor
     * - action=change-form&enrollmentId=<>  → form đổi mentor
     * - (mặc định)                          → danh sách enrollment
     *
     * Request attributes set:
     * - enrollments: List<ProgramEnrollment> (for list)
     * - enrollment: ProgramEnrollment (for detail)
     * - mentorHistory: List<MentorAssignment> (for detail)
     * - programs: List<InternshipProgram> (for enroll-form)
     * - mentors: List<Mentor> (for assign-form/change-form)
     * - viewMode: String
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        User currentUser = (User) request.getSession().getAttribute("currentUser");
        if (!UserRoleChecks.canManageHr(currentUser)) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }

        String action = request.getParameter("action");
        if ("application-options".equals(action)) {
            writeApplicationChoices(request, response, currentUser);
            return;
        }
        CsrfToken.expose(request);

        try {
            if ("detail".equals(action)) {
                handleDetail(request);
            } else if ("enroll-form".equals(action)) {
                handleEnrollForm(request);
            } else if ("assign-form".equals(action) || "change-form".equals(action)) {
                handleMentorForm(request, action);
            } else {
                loadList(request);
            }
        } catch (IllegalArgumentException e) {
            request.setAttribute("errorMessage", e.getMessage());
            request.setAttribute("viewMode", "list");
            try {
                request.setAttribute("enrollments", enrollmentService.getAllEnrollments());
            } catch (SQLException loadError) {
                LOGGER.log(Level.SEVERE, "Error loading enrollment list", loadError);
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error loading enrollment data", e);
            request.setAttribute("errorMessage", "Lỗi tải dữ liệu ghi danh.");
            request.setAttribute("viewMode", "list");
        }

        transferSessionMessages(request);
        request.getRequestDispatcher("/WEB-INF/views/hr/enrollments.jsp")
                .forward(request, response);
    }

    /**
     * POST /hr/enrollments
     *
     * Parameters:
     * - action: "enroll" | "assign-mentor" | "change-mentor"
     *
     * For "enroll":
     * - applicationId: int
     * - programId: int
     *
     * For "assign-mentor":
     * - enrollmentId: int
     * - mentorId: int
     *
     * For "change-mentor":
     * - enrollmentId: int
     * - newMentorId: int
     * - endReason: String
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.setCharacterEncoding("UTF-8");
        User currentUser = (User) request.getSession().getAttribute("currentUser");
        if (!UserRoleChecks.canManageHr(currentUser)) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }
        if (!CsrfToken.valid(request)) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Invalid form token");
            return;
        }

        String action = request.getParameter("action");
        long hrUserId = currentUser.getUserId();
        String redirect = request.getContextPath() + "/hr/enrollments";

        try {
            switch (action != null ? action : "") {
                case "enroll" -> redirect += "?action=detail&id=" + handleEnroll(request, hrUserId);
                case "assign-mentor" -> handleAssignMentor(request, hrUserId);
                case "change-mentor" -> handleChangeMentor(request, hrUserId);
                default -> request.getSession().setAttribute("errorMessage",
                        "Hành động không hợp lệ.");
            }
        } catch (com.internship.service.ScheduleLeaveException e) {
            response.sendError(e.status(), e.getMessage());
            return;
        } catch (IllegalStateException | IllegalArgumentException e) {
            request.getSession().setAttribute("errorMessage", e.getMessage());
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error processing enrollment action: " + action, e);
            request.getSession().setAttribute("errorMessage",
                    "Lỗi hệ thống khi xử lý. Vui lòng thử lại.");
        }

        response.sendRedirect(redirect);
    }

    // Choices never create enrollments; POST keeps all existing approval/duplicate/capacity checks.
    private void writeApplicationChoices(HttpServletRequest request, HttpServletResponse response, User actor)
            throws IOException {
        response.setContentType("application/json;charset=UTF-8");
        response.setHeader("Cache-Control", "no-store");
        try {
            String raw = request.getParameter("applicationId");
            Integer id = raw == null || raw.isBlank() ? null : parseIntParam(request, "applicationId");
            var choices = applicationLookup.find(actor, request.getParameter("email"), id).stream()
                    .map(c -> Be4Json.fields("applicationId", c.applicationId(), "internName", c.internName(), "email", c.email())).toList();
            response.getWriter().write(Be4Json.stringify(Be4Json.fields("success", true, "data", choices)));
        } catch (com.internship.service.ScheduleLeaveException e) {
            response.setStatus(e.status());
            response.getWriter().write(Be4Json.stringify(Be4Json.fields("success", false, "error", e.getMessage())));
        } catch (IllegalArgumentException e) {
            response.setStatus(400);
            response.getWriter().write(Be4Json.stringify(Be4Json.fields("success", false, "error", e.getMessage())));
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Approved application lookup failed", e);
            response.setStatus(503);
            response.getWriter().write(Be4Json.stringify(Be4Json.fields("success", false, "error", "Không thể tìm đơn lúc này. Vui lòng thử lại.")));
        }
    }

    // --- Action handlers ---

    private int handleEnroll(HttpServletRequest request, long hrUserId) throws SQLException {
        int applicationId = parseIntParam(request, "applicationId");
        int programId = parseIntParam(request, "programId");
        String email = request.getParameter("applicationEmail");
        if (email != null && !email.isBlank()) {
            User actor = (User) request.getSession().getAttribute("currentUser");
            boolean matches = applicationLookup.find(actor, email, null).stream()
                    .anyMatch(c -> c.applicationId() == applicationId);
            if (!matches) throw new IllegalArgumentException("Đơn đã chọn không khớp email hoặc không còn đủ điều kiện ghi danh.");
        }

        String mentorValue = request.getParameter("mentorId");
        Integer mentorId = mentorValue == null || mentorValue.isBlank() ? null
                : parseIntParam(request, "mentorId");
        var result = enrollmentService.enrollInternWithMentor(applicationId, programId, mentorId, hrUserId);
        request.getSession().setAttribute("successMessage",
                "Đã ghi danh thành công (mã enrollment: " + result.enrollmentId() + ")."
                        + (result.assignmentId() != null ? " Đã phân mentor." : " Chưa phân mentor."));
        if (result.mentorWarning() != null) {
            request.getSession().setAttribute("errorMessage", result.mentorWarning());
        }
        return result.enrollmentId();
    }

    private void handleAssignMentor(HttpServletRequest request, long hrUserId) throws SQLException {
        int enrollmentId = parseIntParam(request, "enrollmentId");
        int mentorId = parseIntParam(request, "mentorId");

        int assignmentId = enrollmentService.assignMentor(enrollmentId, mentorId, hrUserId);
        request.getSession().setAttribute("successMessage",
                "Đã phân mentor thành công (mã assignment: " + assignmentId + ").");
    }

    private void handleChangeMentor(HttpServletRequest request, long hrUserId) throws SQLException {
        int enrollmentId = parseIntParam(request, "enrollmentId");
        int newMentorId = parseIntParam(request, "newMentorId");
        String endReason = request.getParameter("endReason");

        int assignmentId = enrollmentService.changeMentor(
                enrollmentId, newMentorId, endReason, hrUserId);
        request.getSession().setAttribute("successMessage",
                "Đã đổi mentor thành công (mã assignment mới: " + assignmentId + ").");
    }

    // --- View helpers ---

    private void handleDetail(HttpServletRequest request) throws SQLException {
        int enrollmentId = parseIntParam(request, "id");
        ProgramEnrollment enrollment = enrollmentService.getEnrollmentById(enrollmentId);
        if (enrollment == null) {
            request.setAttribute("errorMessage", "Không tìm thấy enrollment.");
            loadList(request);
            return;
        }
        List<MentorAssignment> history = enrollmentService.getMentorHistory(enrollmentId);
        request.setAttribute("enrollment", enrollment);
        request.setAttribute("mentorHistory", history);
        request.setAttribute("viewMode", "detail");
    }

    private void handleEnrollForm(HttpServletRequest request) throws SQLException {
        String appIdStr = request.getParameter("applicationId");
        if (appIdStr != null) {
            request.setAttribute("applicationId", parseIntParam(request, "applicationId"));
        }
        List<InternshipProgram> programs = programDAO.getActivePrograms();
        request.setAttribute("programs", programs);
        request.setAttribute("mentors", mentorDAO.getActiveMentors().stream()
                .filter(m -> !m.isAtCapacity()).toList());
        request.setAttribute("viewMode", "enroll-form");
    }

    private void handleMentorForm(HttpServletRequest request, String action) throws SQLException {
        int enrollmentId = parseIntParam(request, "enrollmentId");
        ProgramEnrollment enrollment = enrollmentService.getEnrollmentById(enrollmentId);
        if (enrollment == null) {
            request.setAttribute("errorMessage", "Không tìm thấy enrollment.");
            loadList(request);
            return;
        }
        if (!"ACTIVE".equals(enrollment.getStatus())) {
            throw new IllegalArgumentException("Chỉ được phân hoặc đổi mentor cho ghi danh ACTIVE.");
        }
        if ("assign-form".equals(action) && enrollment.getCurrentMentorId() != null) {
            throw new IllegalArgumentException("Đã có mentor; hãy dùng chức năng đổi mentor.");
        }
        if ("change-form".equals(action) && enrollment.getCurrentMentorId() == null) {
            throw new IllegalArgumentException("Chưa có mentor; hãy dùng chức năng phân mentor.");
        }
        InternshipProgram program = programDAO.getProgramById(enrollment.getProgramId());
        if (program == null) throw new IllegalArgumentException("Chương trình không tồn tại.");
        List<Mentor> mentors = mentorDAO.getActiveMentors().stream()
                .filter(m -> m.getDepartmentId() != null
                        && m.getDepartmentId() == program.getDepartmentId()
                        && !m.isAtCapacity()
                        && !java.util.Objects.equals(m.getMentorId(), enrollment.getCurrentMentorId()))
                .toList();
        request.setAttribute("enrollment", enrollment);
        request.setAttribute("mentors", mentors);
        request.setAttribute("viewMode", action);
    }

    // --- Utility ---

    private void loadList(HttpServletRequest request) throws SQLException {
        request.setAttribute("enrollments", enrollmentService.getAllEnrollments());
        request.setAttribute("viewMode", "list");
    }

    private int parseIntParam(HttpServletRequest request, String paramName) {
        String value = request.getParameter(paramName);
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Thiếu tham số: " + paramName);
        }
        try {
            int id = Integer.parseInt(value.trim());
            if (id <= 0) throw new NumberFormatException();
            return id;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Tham số không hợp lệ: " + paramName);
        }
    }

    private void transferSessionMessages(HttpServletRequest request) {
        var session = request.getSession();
        String success = (String) session.getAttribute("successMessage");
        String error = (String) session.getAttribute("errorMessage");
        if (success != null) {
            request.setAttribute("successMessage", success);
            session.removeAttribute("successMessage");
        }
        if (error != null) {
            request.setAttribute("errorMessage", error);
            session.removeAttribute("errorMessage");
        }
    }
}
