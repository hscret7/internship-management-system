package com.internship.controller;

import com.internship.model.ProgramEnrollment;
import com.internship.model.MentorAssignment;
import com.internship.model.User;
import com.internship.dao.InternProfileDAO;
import com.internship.model.InternProfile;
import com.internship.service.EnrollmentService;
import com.internship.util.UserRoleChecks;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * InternEnrollmentServlet: TTS xem enrollment hiện tại và mentor.
 * Route: /intern/enrollment
 */
@WebServlet("/intern/enrollment")
public class InternEnrollmentServlet extends HttpServlet {

    private static final Logger LOGGER = Logger.getLogger(InternEnrollmentServlet.class.getName());
    private EnrollmentService enrollmentService;
    private InternProfileDAO internProfileDAO;

    public InternEnrollmentServlet() { }

    InternEnrollmentServlet(EnrollmentService service, InternProfileDAO profiles) {
        enrollmentService = service;
        internProfileDAO = profiles;
    }

    @Override
    public void init() throws ServletException {
        if (enrollmentService == null) enrollmentService = new EnrollmentService();
        if (internProfileDAO == null) internProfileDAO = new InternProfileDAO();
    }

    /**
     * GET /intern/enrollment
     * Xem enrollment ACTIVE hiện tại và mentor hiện hành.
     *
     * Request attributes set:
     * - enrollment: ProgramEnrollment (or null)
     * - currentMentor: MentorAssignment (or null)
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        User currentUser = (User) request.getSession().getAttribute("currentUser");
        if (!UserRoleChecks.isIntern(currentUser)) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }
        response.setHeader("Cache-Control", "no-store");

        try {
            InternProfile profile = internProfileDAO.getProfileByUserId(currentUser.getUserId());
            if (profile != null) {
                ProgramEnrollment enrollment = enrollmentService
                        .getActiveEnrollmentByIntern(profile.getProfileId());
                if (enrollment != null) {
                    request.setAttribute("enrollment", enrollment);
                    MentorAssignment mentor = enrollmentService
                            .getCurrentMentor(enrollment.getEnrollmentId());
                    request.setAttribute("currentMentor", mentor);
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error loading intern enrollment", e);
            request.setAttribute("errorMessage", "Lỗi tải thông tin ghi danh.");
            request.setAttribute("enrollment", null);
            request.setAttribute("currentMentor", null);
        }

        request.getRequestDispatcher("/WEB-INF/views/intern/enrollment.jsp")
                .forward(request, response);
    }
}
