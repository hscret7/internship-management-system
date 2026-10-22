package com.internship.controller;

import com.internship.dao.*;
import com.internship.model.*;
import com.internship.util.*;
import jakarta.servlet.*;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.sql.SQLException;

@WebServlet({"/intern/schedule", "/intern/leave-requests"})
public class InternScheduleLeavePageServlet extends HttpServlet {
    @Override protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        var session = req.getSession(false);
        Object attribute = session == null ? null : session.getAttribute("currentUser");
        if (!(attribute instanceof User actor) || !UserRoleChecks.isIntern(actor) || actor.getStatus() != UserStatus.ACTIVE) {
            resp.sendError(403); return;
        }
        try {
            InternProfile profile = new InternProfileDAO().getProfileByUserId(actor.getUserId());
            req.setAttribute("enrollment", profile == null ? null : new ProgramEnrollmentDAO().getActiveEnrollmentByIntern(profile.getProfileId()));
        } catch (SQLException e) {
            log("Cannot load intern schedule enrollment", e);
            resp.sendError(503); return;
        }
        resp.setHeader("Cache-Control", "no-store");
        CsrfToken.expose(req);
        req.getRequestDispatcher("/WEB-INF/views/intern/" + (req.getServletPath().endsWith("schedule") ? "schedule.jsp" : "leave-requests.jsp")).forward(req, resp);
    }
}
