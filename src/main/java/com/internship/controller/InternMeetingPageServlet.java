package com.internship.controller;

import com.internship.model.User;
import com.internship.model.UserStatus;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import java.io.IOException;

/** BE1 page route for FE1's existing read-only meeting view. */
@WebServlet("/intern/meetings")
public class InternMeetingPageServlet extends HttpServlet {
    @Override protected void doGet(HttpServletRequest req, HttpServletResponse res)
            throws ServletException, IOException {
        HttpSession session = req.getSession(false);
        Object actor = session == null ? null : session.getAttribute("currentUser");
        if (!(actor instanceof User user)) {
            res.sendRedirect(req.getContextPath() + "/auth/login");
            return;
        }
        if (user.getStatus() != UserStatus.ACTIVE || user.getRole() == null
                || !"INTERN".equals(user.getRole().getRoleName())) {
            res.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }
        res.setHeader("Cache-Control", "no-store");
        req.getRequestDispatcher("/WEB-INF/views/intern/meetings.jsp").forward(req, res);
    }
}
