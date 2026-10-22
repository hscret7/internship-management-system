package com.internship.controller;

import com.internship.dao.ProgramStatisticsDAO;
import com.internship.model.*;
import com.internship.service.ProgramLifecycleService;
import com.internship.util.*;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.sql.SQLException;
import java.time.LocalDate;

@WebServlet({"/api/hr/program-lifecycle", "/api/hr/enrollment-lifecycle", "/api/hr/program-statistics"})
public class ProgramLifecycleServlet extends HttpServlet {
    private final ProgramLifecycleService service = new ProgramLifecycleService();
    @Override protected void doGet(HttpServletRequest req, HttpServletResponse res) throws IOException { handle(req, res, false); }
    @Override protected void doPost(HttpServletRequest req, HttpServletResponse res) throws IOException { req.setCharacterEncoding("UTF-8"); handle(req, res, true); }
    private void handle(HttpServletRequest req, HttpServletResponse res, boolean write) throws IOException {
        res.setCharacterEncoding("UTF-8"); res.setContentType("application/json"); res.setHeader("Cache-Control", "no-store");
        try {
            var session = req.getSession(false);
            Object value = session == null ? null : session.getAttribute("currentUser");
            if (!(value instanceof User actor)) { error(res, 401, "Vui lòng đăng nhập."); return; }
            if (actor.getStatus() != UserStatus.ACTIVE || !UserRoleChecks.canManageHr(actor)) { error(res, 403, "Chỉ HR/Admin được thao tác."); return; }
            Object data;
            if (write) {
                String content = req.getContentType();
                if (content == null || !content.toLowerCase(java.util.Locale.ROOT).startsWith("application/x-www-form-urlencoded")) { error(res, 415, "Yêu cầu form-urlencoded."); return; }
                if (!CsrfToken.valid(req)) { error(res, 403, "CSRF không hợp lệ."); return; }
                String target = req.getParameter("status"), reason = req.getParameter("reason");
                if (req.getServletPath().endsWith("program-lifecycle")) data = Be4Json.fields("closedEnrollments", service.closeProgram(actor.getUserId(), id(req, "programId"), target, reason));
                else if (req.getServletPath().endsWith("enrollment-lifecycle")) { service.closeEnrollment(actor.getUserId(), id(req, "enrollmentId"), target, reason); data = null; }
                else { error(res, 405, "Thống kê chỉ hỗ trợ GET."); return; }
            } else {
                if (!req.getServletPath().endsWith("program-statistics")) {
                    // Fetch a CSRF token without requiring a mutation or a database connection.
                    data = null;
                } else {
                    var filter = new ProgramStatisticsDAO.Filter(optionalId(req, "programId"), optional(req, "status"), date(req, "cohortFrom"), date(req, "cohortTo"));
                    try (var c = DBContext.getConnection()) {
                        var stats = new ProgramStatisticsDAO().summarize(c, filter);
                        data = Be4Json.fields("interns", stats.interns(), "enrollments", stats.enrollments(), "active", stats.active(), "completed", stats.completed(), "cancelled", stats.cancelled(),
                                "completionRate", stats.completionRate(), "formula", "COMPLETED / all enrollments in the same filter * 100",
                                "universities", stats.universities().stream().map(g -> Be4Json.fields("name", g.name(), "interns", g.interns(), "enrollments", g.enrollments())).toList(),
                                "majors", stats.majors().stream().map(g -> Be4Json.fields("name", g.name(), "interns", g.interns(), "enrollments", g.enrollments())).toList());
                    }
                }
            }
            res.getWriter().write(Be4Json.stringify(Be4Json.fields("success", true, "data", data, "csrfToken", CsrfToken.expose(req))));
        } catch (SecurityException e) { error(res, 403, e.getMessage()); }
          catch (IllegalArgumentException | java.time.format.DateTimeParseException e) { error(res, 400, e.getMessage()); }
          catch (IllegalStateException e) { error(res, 409, e.getMessage()); }
          catch (SQLException e) { getServletContext().log("BE3 lifecycle/statistics failed", e); error(res, 503, "Không thể xử lý dữ liệu."); }
    }
    private static void error(HttpServletResponse res, int status, String message) throws IOException { res.setStatus(status); res.getWriter().write(Be4Json.stringify(Be4Json.fields("success", false, "error", message))); }
    private static String optional(HttpServletRequest r, String k) { String v = r.getParameter(k); return v == null || v.isBlank() ? null : v.strip(); }
    private static Integer optionalId(HttpServletRequest r, String k) { String v = optional(r, k); return v == null ? null : Integer.valueOf(v); }
    private static int id(HttpServletRequest r, String k) { Integer v = optionalId(r, k); if (v == null) throw new IllegalArgumentException("Thiếu " + k); return v; }
    private static LocalDate date(HttpServletRequest r, String k) { String v = optional(r, k); return v == null ? null : LocalDate.parse(v); }
}
