package com.internship.controller;

import com.internship.model.User;
import com.internship.service.AttendanceService;
import com.internship.service.EnrollmentLookupService;
import com.internship.service.ScheduleLeaveException;
import com.internship.service.AttendanceService.AttendanceReportRow;
import com.internship.util.UserRoleChecks;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import java.sql.SQLException;

@WebServlet({"/intern/attendance", "/hr/attendance"})
public class AttendanceServlet extends HttpServlet {

    private AttendanceService attendanceService;
    private EnrollmentLookupService enrollmentLookup = new EnrollmentLookupService();

    public AttendanceServlet() {}

    AttendanceServlet(AttendanceService attendanceService) {
        this.attendanceService = attendanceService;
    }

    AttendanceServlet(AttendanceService attendanceService, EnrollmentLookupService enrollmentLookup) {
        this.attendanceService = java.util.Objects.requireNonNull(attendanceService);
        this.enrollmentLookup = java.util.Objects.requireNonNull(enrollmentLookup);
    }

    @Override
    public void init() {
        if (attendanceService == null) {
            attendanceService = new AttendanceService();
        }
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        HttpSession session = req.getSession();
        User currentUser = (User) session.getAttribute("currentUser");

        String path = req.getServletPath();
        
        try {
            if ("/intern/attendance".equals(path)) {
                if (!UserRoleChecks.isIntern(currentUser)) {
                    resp.sendError(HttpServletResponse.SC_FORBIDDEN, "Access denied");
                    return;
                }
                
                var choices = enrollmentLookup.lookup(currentUser, null);
                req.setAttribute("enrollmentChoices", choices);
                String enrollParam = req.getParameter("enrollmentId");
                int enrollmentId = 0;
                try {
                    enrollmentId = (enrollParam != null && !enrollParam.trim().isEmpty()) ? Integer.parseInt(enrollParam) : 0;
                } catch (NumberFormatException e) {
                    // ignore, keep 0
                }
                
                if (enrollmentId > 0) {
                    final int selected = enrollmentId;
                    if (choices.stream().noneMatch(c -> c.enrollmentId() == selected)) {
                        resp.sendError(HttpServletResponse.SC_FORBIDDEN, "Không có quyền truy cập ghi danh.");
                        return;
                    }
                } else if (choices.size() == 1) {
                    enrollmentId = choices.get(0).enrollmentId();
                }

                LocalDate from = LocalDate.now().minusDays(30);
                LocalDate to = LocalDate.now();
                try {
                    if (req.getParameter("from") != null && !req.getParameter("from").isEmpty()) {
                        from = LocalDate.parse(req.getParameter("from"));
                    }
                    if (req.getParameter("to") != null && !req.getParameter("to").isEmpty()) {
                        to = LocalDate.parse(req.getParameter("to"));
                    }
                } catch (Exception e) {
                    // use defaults
                }
                
                List<AttendanceReportRow> report = attendanceService.getReport(currentUser, enrollmentId > 0 ? enrollmentId : null, null, from, to);
                req.setAttribute("report", report);
                req.setAttribute("from", from);
                req.setAttribute("to", to);
                req.setAttribute("enrollmentId", enrollmentId > 0 ? enrollmentId : null);
                req.getRequestDispatcher("/WEB-INF/views/intern/attendance.jsp").forward(req, resp);
                
            } else if ("/hr/attendance".equals(path)) {
                if (!UserRoleChecks.canManageHr(currentUser)) {
                    resp.sendError(HttpServletResponse.SC_FORBIDDEN, "Access denied");
                    return;
                }
                
                String enrollParam = req.getParameter("enrollmentId");
                Integer enrollmentId = null;
                try {
                    enrollmentId = (enrollParam != null && !enrollParam.trim().isEmpty()) ? Integer.parseInt(enrollParam) : null;
                } catch (NumberFormatException e) { }
                
                String progParam = req.getParameter("programId");
                Integer programId = null;
                try {
                    programId = (progParam != null && !progParam.trim().isEmpty()) ? Integer.parseInt(progParam) : null;
                } catch (NumberFormatException e) { }
                
                LocalDate from = LocalDate.now().minusDays(30);
                LocalDate to = LocalDate.now();
                try {
                    if (req.getParameter("from") != null && !req.getParameter("from").isEmpty()) {
                        from = LocalDate.parse(req.getParameter("from"));
                    }
                    if (req.getParameter("to") != null && !req.getParameter("to").isEmpty()) {
                        to = LocalDate.parse(req.getParameter("to"));
                    }
                } catch (Exception e) { }
                
                String status = req.getParameter("status");
                List<AttendanceReportRow> report = attendanceService.getReport(currentUser, enrollmentId, programId, from, to, (status != null && !status.trim().isEmpty()) ? status.trim() : null);

                // Cung MOT payload phuc vu ca hai duong lay du lieu ma FE2 duoc giao:
                // tra thang JSON, hoac nhung vao the <script> cua trang HTML.
                String json = toJson(report);
                if (wantsJson(req)) {
                    writeJson(resp, json);
                    return;
                }
                req.setAttribute("attendanceJson", json);
                req.setAttribute("report", report);
                req.setAttribute("from", from);
                req.setAttribute("to", to);
                req.setAttribute("enrollmentId", enrollmentId);
                req.setAttribute("programId", programId);
                req.setAttribute("status", status);
                req.getRequestDispatcher("/WEB-INF/views/hr/attendance.jsp").forward(req, resp);
            }
        } catch (ScheduleLeaveException e) {
            resp.sendError(e.status(), e.getMessage());
        } catch (SQLException e) {
            java.util.logging.Logger.getLogger(AttendanceServlet.class.getName())
                    .log(java.util.logging.Level.SEVERE, "Attendance database operation failed", e);
            resp.setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
            if (wantsJson(req)) writeJson(resp, "{\"error\":\"Không thể xử lý dữ liệu, vui lòng thử lại.\"}");
            else resp.sendError(HttpServletResponse.SC_SERVICE_UNAVAILABLE, "Không thể xử lý dữ liệu, vui lòng thử lại.");
        } catch (IllegalArgumentException e) {
            // Client xin JSON thi phai nhan JSON: neu forward sang /error/500.jsp thi
            // ho nhan ve HTML, va loi nhap lieu (khoang > 367 ngay, from > to) bi bao
            // nham thanh 500.
            if (wantsJson(req)) {
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                writeJson(resp, "{\"error\":" + quote(e.getMessage()) + "}");
                return;
            }
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, e.getMessage());
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        if (!com.internship.util.CsrfToken.valid(req)) {
            resp.sendError(HttpServletResponse.SC_FORBIDDEN, "Invalid CSRF Token");
            return;
        }

        HttpSession session = req.getSession();
        User currentUser = (User) session.getAttribute("currentUser");

        String path = req.getServletPath();
        
        if ("/intern/attendance".equals(path)) {
            String action = req.getParameter("action");
            int enrollmentId = 0;
            try {
                String enrollParam = req.getParameter("enrollmentId");
                if (enrollParam != null && !enrollParam.trim().isEmpty()) {
                    enrollmentId = Integer.parseInt(enrollParam);
                }
            } catch (NumberFormatException e) {
                resp.sendRedirect(req.getContextPath() + "/intern/attendance?error=" + java.net.URLEncoder.encode("Mã ghi danh không hợp lệ", "UTF-8"));
                return;
            }
            
            try {
                if ("checkin".equals(action)) {
                    attendanceService.checkIn(currentUser, enrollmentId);
                    resp.sendRedirect(req.getContextPath() + "/intern/attendance?enrollmentId=" + enrollmentId + "&success=true");
                } else if ("checkout".equals(action)) {
                    attendanceService.checkOut(currentUser, enrollmentId);
                    resp.sendRedirect(req.getContextPath() + "/intern/attendance?enrollmentId=" + enrollmentId + "&success=true");
                } else {
                    resp.sendRedirect(req.getContextPath() + "/intern/attendance?enrollmentId=" + enrollmentId + "&error=" + java.net.URLEncoder.encode("Hành động không hợp lệ", "UTF-8"));
                }
            } catch (SQLException | IllegalArgumentException e) {
                resp.sendRedirect(req.getContextPath() + "/intern/attendance?enrollmentId=" + enrollmentId + "&error=" + java.net.URLEncoder.encode(e.getMessage(), "UTF-8"));
            }
        }
    }

    /** Client xin JSON qua ?format=json, hoac qua header Accept chua application/json. */
    private static boolean wantsJson(HttpServletRequest req) {
        if ("json".equalsIgnoreCase(req.getParameter("format"))) {
            return true;
        }
        String accept = req.getHeader("Accept");
        return accept != null && accept.contains("application/json");
    }

    private static void writeJson(HttpServletResponse resp, String json) throws IOException {
        resp.setContentType("application/json;charset=UTF-8");
        resp.setCharacterEncoding("UTF-8");
        resp.setHeader("Cache-Control", "no-store");
        resp.getWriter().write(json);
    }

    /**
     * Chuoi JSON nay dung cho CA HAI duong: lam body cua response JSON, va nhung vao
     * the &lt;script id="attendance-data"&gt; cua trang HTML. Vi vay moi '&lt;' duoc escape
     * thanh < de chuoi khong the dong the script som; van la JSON hop le.
     */
    // Package-private (khong phai private) de AttendanceReportJsonTest goi truc tiep,
    // theo dung loi Be4Json + EvaluationSummaryJsonTest da co trong repo.
    static String toJson(List<AttendanceReportRow> rows) {
        StringBuilder out = new StringBuilder(rows.size() * 220 + 2).append('[');
        for (int i = 0; i < rows.size(); i++) {
            AttendanceReportRow r = rows.get(i);
            if (i > 0) {
                out.append(',');
            }
            out.append("{\"enrollmentId\":").append(r.getEnrollmentId())
               .append(",\"internName\":").append(quote(r.getInternName()))
               .append(",\"programName\":").append(quote(r.getProgramName()))
               .append(",\"workDate\":").append(quote(text(r.getWorkDate())))
               .append(",\"startTime\":").append(quote(text(r.getStartTime())))
               .append(",\"endTime\":").append(quote(text(r.getEndTime())))
               .append(",\"checkInTime\":").append(quote(text(r.getCheckInTime())))
               .append(",\"checkOutTime\":").append(quote(text(r.getCheckOutTime())))
               .append(",\"approvedLeave\":").append(r.isApprovedLeave())
               .append(",\"status\":").append(quote(r.getStatus()))
               .append(",\"workedHours\":").append(workedHours(r.getWorkedHours()))
               .append('}');
        }
        return out.append(']').toString();
    }

    private static String text(Object value) {
        return value == null ? null : value.toString();
    }

    /** NaN/vo cuc khong phai JSON hop le, nen quy ve 0.0. */
    private static String workedHours(double value) {
        return (Double.isFinite(value) && value >= 0) ? String.valueOf(value) : "0.0";
    }

    private static String quote(String value) {
        if (value == null) {
            return "null";
        }
        StringBuilder out = new StringBuilder(value.length() + 2).append('"');
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            switch (c) {
                case '"' -> out.append("\\\"");
                case '\\' -> out.append("\\\\");
                case '\n' -> out.append("\\n");
                case '\r' -> out.append("\\r");
                case '\t' -> out.append("\\t");
                case '<' -> out.append("\\u003c");
                default -> {
                    if (c < 0x20) {
                        out.append(String.format("\\u%04x", (int) c));
                    } else {
                        out.append(c);
                    }
                }
            }
        }
        return out.append('"').toString();
    }
}
