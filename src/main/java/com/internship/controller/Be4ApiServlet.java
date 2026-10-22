package com.internship.controller;

import com.internship.model.User;
import com.internship.service.DashboardService;
import com.internship.service.EvaluationService;
import com.internship.service.EnrollmentQueryService;
import com.internship.service.TaskService;
import com.internship.service.WeeklyReportService;
import com.internship.service.Sprint3WorkflowService;
import com.internship.service.EndTermReportService;
import com.internship.service.EndTermExport;
import com.internship.service.ProgramStatisticsReportService;
import com.internship.service.ProgramStatisticsExport;
import com.internship.service.AttendanceReportService;
import com.internship.service.AttendanceExport;
import com.internship.dao.ProgramStatisticsDAO;
import com.internship.dao.Sprint3WorkflowDAO;
import java.util.List;
import java.util.Map;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.math.BigDecimal;
import java.security.SecureRandom;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;

/** JSON endpoints consumed by FE2/FE3. Actor identity always comes from the login session. */
@WebServlet({"/hr/be4/*", "/mentor/be4/*", "/intern/be4/*"})
public class Be4ApiServlet extends HttpServlet {
    private static final String CSRF_KEY = "be4CsrfToken";
    private static final SecureRandom RANDOM = new SecureRandom();

    private TaskService tasks;
    private WeeklyReportService reports;
    private EvaluationService evaluations;
    private DashboardService dashboards;
    private EnrollmentQueryService enrollments;
    private Sprint3WorkflowService workflows;
    private EndTermReportService endTerm;
    private AttendanceReportService attendanceExports;

    @Override
    public void init() {
        tasks = new TaskService();
        reports = new WeeklyReportService();
        evaluations = new EvaluationService();
        dashboards = new DashboardService();
        enrollments = new EnrollmentQueryService();
        workflows = new Sprint3WorkflowService();
        endTerm = new EndTermReportService();
        attendanceExports = new AttendanceReportService();
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        dispatch(request, response, false);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        dispatch(request, response, true);
    }

    private void dispatch(HttpServletRequest request, HttpServletResponse response, boolean post)
            throws IOException {
        response.setCharacterEncoding("UTF-8");
        response.setContentType("application/json;charset=UTF-8");
        response.setHeader("Cache-Control", "no-store");
        response.setHeader("X-Content-Type-Options", "nosniff");
        try {
            HttpSession session = request.getSession(false);
            if (session == null || !(session.getAttribute("currentUser") instanceof User actor)) {
                send(response, 401, Be4Json.fields("error", "Cần đăng nhập"));
                return;
            }
            String area = request.getServletPath();
            String role = actor.getRole() == null ? null : actor.getRole().getRoleName();
            if (!areaAllowed(area, role)) {
                send(response, 403, Be4Json.fields("error", "Không có quyền truy cập"));
                return;
            }
            String path = request.getPathInfo();
            if (path == null) path = "/";
            if (!post && "/hr/be4".equals(area) && "/attendance/export".equals(path)) {
                String format = request.getParameter("format");
                if (!"xlsx".equals(format) && !"pdf".equals(format)) throw new IllegalArgumentException("format phải là xlsx hoặc pdf");
                var snapshot = attendanceExports.snapshot(actor.getUserId(), attendanceFilter(request));
                byte[] file;
                try { file = "xlsx".equals(format) ? new AttendanceExport().xlsx(snapshot) : new AttendanceExport().pdf(snapshot); }
                catch (IOException | IllegalArgumentException failure) {
                    getServletContext().log("BE4 attendance export rendering failed", failure);
                    send(response, 503, Be4Json.fields("error", "Không thể tạo file chuyên cần. Kiểm tra font PDF và thử lại.")); return;
                }
                attendanceExports.recordExport(actor.getUserId(), snapshot, format);
                response.setContentType("xlsx".equals(format) ? "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet" : "application/pdf");
                response.setHeader("Content-Disposition", "attachment; filename=\"chuyen-can." + format + "\"");
                response.setContentLength(file.length); response.getOutputStream().write(file); return;
            }
            if (!post && "/hr/be4".equals(area) && "/program-statistics/export".equals(path)) {
                String format=request.getParameter("format");
                if(!"xlsx".equals(format)&&!"pdf".equals(format))throw new IllegalArgumentException("format phải là xlsx hoặc pdf");
                String status=request.getParameter("status");if(status!=null&&status.isBlank())status=null;
                var filter=new ProgramStatisticsDAO.Filter(optionalInteger(request,"programId"),status,optionalDate(request,"cohortFrom"),optionalDate(request,"cohortTo"));
                var service=new ProgramStatisticsReportService();var snapshot=service.snapshot(actor.getUserId(),filter);byte[] file;
                try {file="xlsx".equals(format)?new ProgramStatisticsExport().xlsx(snapshot):new ProgramStatisticsExport().pdf(snapshot);}
                catch(IOException|IllegalArgumentException e){getServletContext().log("BE4 statistics export rendering failed",e);send(response,503,Be4Json.fields("error","Không thể tạo file thống kê. Kiểm tra font PDF và thử lại."));return;}
                service.recordExport(actor.getUserId(),snapshot,format);
                response.setContentType("pdf".equals(format)?"application/pdf":"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
                response.setHeader("Content-Disposition","attachment; filename=\"thong-ke-thuc-tap."+format+"\"");response.setContentLength(file.length);response.getOutputStream().write(file);return;
            }
            if (!post && "/hr/be4".equals(area) && "/end-term/export".equals(path)) {
                String format = request.getParameter("format");
                if (!"xlsx".equals(format) && !"pdf".equals(format)) throw new IllegalArgumentException("format phải là xlsx hoặc pdf");
                var snapshot = endTerm.exportSnapshot(actor.getUserId(), reportFilter(request));
                byte[] file;
                try { file = "xlsx".equals(format) ? new EndTermExport().xlsx(snapshot) : new EndTermExport().pdf(snapshot); }
                catch (IOException | IllegalArgumentException e) {
                    getServletContext().log("BE4 export rendering failed", e);
                    send(response, 503, Be4Json.fields("error", "Không thể tạo file. Kiểm tra cấu hình font PDF và thử lại.")); return;
                }
                // Records successful file generation, not a claim that the browser finished downloading.
                endTerm.recordExport(actor.getUserId(), snapshot, format);
                response.setContentType("xlsx".equals(format) ? "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet" : "application/pdf");
                response.setHeader("Content-Disposition", "attachment; filename=\"end-term-report." + format + "\"");
                response.setContentLength(file.length); response.getOutputStream().write(file); return;
            }
            if (!post && "/csrf".equals(path)) {
                send(response, 200, Be4Json.fields("csrfToken", csrfToken(session)));
                return;
            }
            if (post) {
                String contentType = request.getContentType();
                if (contentType == null || !contentType.toLowerCase(java.util.Locale.ROOT)
                        .startsWith("application/x-www-form-urlencoded")) {
                    send(response, 415, Be4Json.fields("error", "POST yêu cầu form-urlencoded"));
                    return;
                }
                request.setCharacterEncoding("UTF-8");
                String submitted = request.getHeader("X-CSRF-Token");
                if (submitted == null) submitted = request.getParameter("_csrf");
                if (!csrfValid(session, submitted)) {
                    send(response, 403, Be4Json.fields("error", "CSRF token không hợp lệ"));
                    return;
                }
            }
            Object payload = post ? postPayload(area, path, actor.getUserId(), request)
                    : getPayload(area, path, actor.getUserId(), request);
            send(response, 200, payload);
        } catch (UnknownRoute exception) {
            send(response, 404, Be4Json.fields("error", exception.getMessage()));
        } catch (IllegalArgumentException exception) {
            send(response, 400, Be4Json.fields("error", exception.getMessage()));
        } catch (SecurityException exception) {
            send(response, 403, Be4Json.fields("error", exception.getMessage()));
        } catch (IllegalStateException exception) {
            send(response, 409, Be4Json.fields("error", exception.getMessage()));
        } catch (SQLException exception) {
            getServletContext().log("BE4 database operation failed", exception);
            send(response, 500, Be4Json.fields("error", "Lỗi dữ liệu, vui lòng thử lại"));
        }
    }

    private Object getPayload(String area, String path, long userId, HttpServletRequest request)
            throws SQLException {
        if ("/hr/be4".equals(area) && "/dashboard".equals(path)) {
            return Be4Json.hrDashboard(dashboards.getHrDashboard(userId));
        }
        if ("/hr/be4".equals(area) && "/end-term".equals(path)) {
            var page=endTerm.list(userId,reportFilter(request),optionalInteger(request,"page")==null?1:intParam(request,"page"),optionalInteger(request,"size")==null?20:intParam(request,"size"));
            return Be4Json.fields("items",page.items().stream().map(Be4ApiServlet::endTermRow).toList(),"page",page.page(),"size",page.size(),"hasMore",page.hasMore(),"generatedAt",page.generatedAt().toString());
        }
        if ("/hr/be4".equals(area) && "/evaluations".equals(path)) {
            return evaluations.findAllForHr(userId).stream()
                    .map(Be4Json::evaluationSummary).toList();
        }
        if ("/mentor/be4".equals(area) && "/dashboard".equals(path)) {
            return Be4Json.mentorDashboard(dashboards.getMentorDashboard(userId));
        }
        if ("/intern/be4".equals(area)) {
            return switch (path) {
                case "/enrollments" -> enrollments.findForIntern(userId).stream()
                        .map(Be4Json::enrollment).toList();
                case "/tasks" -> tasks.findTasksForIntern(userId, intParam(request, "enrollmentId"))
                        .stream().map(Be4Json::task).toList();
                case "/assignments" -> tasks.findAssignmentsForIntern(userId, intParam(request, "enrollmentId"))
                        .stream().map(Be4Json::assignment).toList();
                case "/progress" -> progressHistory(userId, longParam(request, "assignmentId"));
                case "/reports" -> structuredReports(reports.findForIntern(userId, intParam(request, "enrollmentId")));
                case "/assessments" -> workflows.assessments(userId, intParam(request, "enrollmentId")).stream().map(Be4ApiServlet::assessment).toList();
                case "/feedback" -> reports.findFeedback(userId, longParam(request, "reportId"))
                        .stream().map(Be4Json::feedback).toList();
                case "/evaluation" -> Be4Json.nullableEvaluation(
                        evaluations.findForActor(userId, intParam(request, "enrollmentId")));
                default -> throw new UnknownRoute();
            };
        }
        if ("/mentor/be4".equals(area)) {
            return switch (path) {
                case "/meeting-participants" -> workflows.meetingParticipants(userId);
                case "/enrollments" -> enrollments.findForMentor(userId).stream()
                        .map(Be4Json::enrollment).toList();
                case "/tasks" -> tasks.findTasksForMentor(userId, intParam(request, "enrollmentId"))
                        .stream().map(Be4Json::task).toList();
                case "/assignments" -> tasks.findAssignmentsForMentor(userId, intParam(request, "enrollmentId"))
                        .stream().map(Be4Json::assignment).toList();
                case "/progress" -> progressHistory(userId, longParam(request, "assignmentId"));
                case "/reports" -> structuredReports(reports.findForMentor(userId, intParam(request, "enrollmentId")));
                case "/assessments" -> workflows.assessments(userId, intParam(request, "enrollmentId")).stream().map(Be4ApiServlet::assessment).toList();
                case "/feedback" -> reports.findFeedback(userId, longParam(request, "reportId"))
                        .stream().map(Be4Json::feedback).toList();
                case "/evaluation" -> Be4Json.nullableEvaluation(
                        evaluations.findForActor(userId, intParam(request, "enrollmentId")));
                default -> throw new UnknownRoute();
            };
        }
        if ("/hr/be4".equals(area) && "/evaluation".equals(path)) {
            return Be4Json.nullableEvaluation(
                    evaluations.findForActor(userId, intParam(request, "enrollmentId")));
        }
        if ("/hr/be4".equals(area) && "/assessments".equals(path)) return workflows.assessments(userId, intParam(request, "enrollmentId")).stream().map(Be4ApiServlet::assessment).toList();
        throw new UnknownRoute();
    }

    private Object postPayload(String area, String path, long userId, HttpServletRequest request)
            throws SQLException {
        if ("/mentor/be4".equals(area)) {
            return switch (path) {
                case "/tasks/create-assign" -> {
                    var created = workflows.createAssignedTask(userId, intParam(request, "enrollmentId"), request.getParameter("title"), request.getParameter("description"), optionalDate(request, "dueDate"));
                    yield Be4Json.fields("task", Be4Json.task(created.task()), "assignment", Be4Json.assignment(created.assignment()));
                }
                case "/assessments" -> assessment(workflows.assess(userId, intParam(request, "enrollmentId"), intParam(request, "skillsScore"), intParam(request, "attitudeScore"), request.getParameter("comment")));
                case "/tasks" -> Be4Json.task(tasks.createTask(userId,
                        request.getParameter("title"), request.getParameter("description"),
                        optionalDate(request, "dueDate")));
                case "/assignments" -> Be4Json.assignment(workflows.assignExistingTask(userId,
                        longParam(request, "taskId"), intParam(request, "enrollmentId")));
                case "/feedback" -> Be4Json.feedback(reports.review(userId,
                        longParam(request, "reportId"), request.getParameter("comment"),
                        optionalInteger(request, "rating")));
                case "/evaluation" -> Be4Json.evaluation(evaluations.create(userId,
                        intParam(request, "enrollmentId"), decimalParam(request, "score"),
                        request.getParameter("comment")));
                default -> throw new UnknownRoute();
            };
        }
        if ("/intern/be4".equals(area)) {
            return switch (path) {
                case "/progress" -> Be4Json.progress(workflows.updateProgress(userId,
                        longParam(request, "assignmentId"), intParam(request, "completionPercentage"),
                        request.getParameter("note")));
                // New drafts must use the Sprint 3 contract. Existing legacy drafts remain readable/editable.
                case "/reports" -> {
                    if (!structured(request)) throw new IllegalArgumentException("Báo cáo mới cần các trường results, difficulties, nextPlan; content chỉ dùng cho nháp cũ.");
                    yield Be4Json.report(workflows.createDraft(userId,
                            intParam(request, "enrollmentId"), shortParam(request, "weekNumber"), request.getParameter("results"), request.getParameter("difficulties"), request.getParameter("nextPlan")));
                }
                case "/reports/update" -> Be4Json.report(structured(request) ? workflows.updateDraft(userId,
                        longParam(request, "reportId"), request.getParameter("results"), request.getParameter("difficulties"), request.getParameter("nextPlan")) : reports.updateDraft(userId,
                        longParam(request, "reportId"), request.getParameter("content")));
                case "/reports/submit" -> Be4Json.report(workflows.submit(userId,
                        longParam(request, "reportId")));
                default -> throw new UnknownRoute();
            };
        }
        throw new UnknownRoute();
    }

    private static boolean structured(HttpServletRequest r) { return r.getParameter("results") != null || r.getParameter("difficulties") != null || r.getParameter("nextPlan") != null; }
    private Object structuredReports(List<com.internship.model.WeeklyReport> rows) throws SQLException {
        var sections = workflows.reportSections(rows);
        return rows.stream().map(r -> {
            var item = Be4Json.report(r); var s = sections.get(r.getReportId());
            item.put("results", s == null ? null : s.results()); item.put("difficulties", s == null ? null : s.difficulties()); item.put("nextPlan", s == null ? null : s.nextPlan());
            item.put("legacyContent", s == null); return item;
        }).toList();
    }
    private Object progressHistory(long actor, long assignment) throws SQLException {
        var rows = tasks.findProgressHistory(actor, assignment);var authors = workflows.progressActors(rows);
        return rows.stream().map(r -> { var item=Be4Json.progress(r);item.put("updatedBy", authors.get(r.getProgressId())); return item; }).toList();
    }
    static Map<String,Object> assessment(Sprint3WorkflowDAO.Assessment a) { return Be4Json.fields("assessmentId",a.assessmentId(),"enrollmentId",a.enrollmentId(),"mentorId",a.mentorId(),"mentorName",a.mentorName(),"skillsScore",a.skillsScore(),"attitudeScore",a.attitudeScore(),"comment",a.comment(),"createdAt",a.createdAt()); }
    private static EndTermReportService.Filter reportFilter(HttpServletRequest r) { return new EndTermReportService.Filter(optionalInteger(r,"programId"),optionalInteger(r,"enrollmentId"),r.getParameter("status")==null||r.getParameter("status").isBlank()?null:r.getParameter("status")); }
    static AttendanceReportService.Filter attendanceFilter(HttpServletRequest request) {
        var allowed = java.util.Set.of("format", "programId", "enrollmentId", "from", "to");
        var names = request.getParameterNames();
        while (names.hasMoreElements()) {
            String name = names.nextElement();
            if (!allowed.contains(name)) throw new IllegalArgumentException("Bộ lọc chuyên cần không hỗ trợ: " + name);
            String[] values = request.getParameterValues(name);
            if (values != null && values.length != 1) throw new IllegalArgumentException("Không lặp lại tham số: " + name);
        }
        return new AttendanceReportService.Filter(optionalInteger(request, "programId"), optionalInteger(request, "enrollmentId"),
                optionalDate(request, "from"), optionalDate(request, "to"));
    }
    private static Map<String,Object> endTermRow(EndTermReportService.Row r) {return Be4Json.fields("enrollmentId",r.enrollmentId(),"programId",r.programId(),"internProfileId",r.internProfileId(),"internName",r.internName(),"university",r.university(),"major",r.major(),"programName",r.programName(),"status",r.status(),"startDate",r.startDate(),"endDate",r.endDate(),"mentorName",r.mentorName(),"taskCount",r.taskCount(),"tasksDone",r.tasksDone(),"submittedReports",r.submittedReports(),"reviewedReports",r.reviewedReports(),"assessments",r.assessments().stream().map(Be4ApiServlet::assessment).toList(),"legacyScore",r.legacyScore());}

    private static boolean areaAllowed(String area, String role) {
        if (role == null) return false;
        return ("/hr/be4".equals(area) && ("HR".equals(role) || "ADMIN".equals(role)))
                || ("/mentor/be4".equals(area) && "MENTOR".equals(role))
                || ("/intern/be4".equals(area) && "INTERN".equals(role));
    }

    private static int intParam(HttpServletRequest request, String name) {
        try {
            return Integer.parseInt(required(request, name));
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException(name + " phải là số nguyên", exception);
        }
    }

    private static short shortParam(HttpServletRequest request, String name) {
        try {
            return Short.parseShort(required(request, name));
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException(name + " phải là số nguyên", exception);
        }
    }

    private static long longParam(HttpServletRequest request, String name) {
        try {
            return Long.parseLong(required(request, name));
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException(name + " phải là số nguyên", exception);
        }
    }

    private static BigDecimal decimalParam(HttpServletRequest request, String name) {
        try {
            return new BigDecimal(required(request, name));
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException(name + " phải là số thập phân", exception);
        }
    }

    private static Integer optionalInteger(HttpServletRequest request, String name) {
        String value = request.getParameter(name);
        if (value == null || value.isBlank()) return null;
        return intParam(request, name);
    }

    private static LocalDate optionalDate(HttpServletRequest request, String name) {
        String value = request.getParameter(name);
        if (value == null || value.isBlank()) return null;
        try {
            return LocalDate.parse(value);
        } catch (DateTimeParseException exception) {
            throw new IllegalArgumentException(name + " phải là YYYY-MM-DD", exception);
        }
    }

    private static String required(HttpServletRequest request, String name) {
        String value = request.getParameter(name);
        if (value == null || value.isBlank()) throw new IllegalArgumentException("Thiếu " + name);
        return value.trim();
    }

    private static String csrfToken(HttpSession session) {
        synchronized (session) {
            String token = (String) session.getAttribute(CSRF_KEY);
            if (token == null) {
                byte[] bytes = new byte[32];
                RANDOM.nextBytes(bytes);
                token = java.util.HexFormat.of().formatHex(bytes);
                session.setAttribute(CSRF_KEY, token);
            }
            return token;
        }
    }

    private static boolean csrfValid(HttpSession session, String supplied) {
        Object stored = session.getAttribute(CSRF_KEY);
        return stored instanceof String token && supplied != null
                && java.security.MessageDigest.isEqual(
                token.getBytes(java.nio.charset.StandardCharsets.UTF_8),
                supplied.getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }

    private static void send(HttpServletResponse response, int status, Object body) throws IOException {
        response.setStatus(status);
        response.getWriter().write(Be4Json.stringify(body));
    }

    private static final class UnknownRoute extends IllegalArgumentException {
        private UnknownRoute() {
            super("Không tìm thấy đường dẫn BE4");
        }
    }
}
