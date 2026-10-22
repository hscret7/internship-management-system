package com.internship.service;

import com.internship.dao.*;
import com.internship.model.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.io.Reader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.*;
import java.util.List;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.junit.jupiter.api.Assertions.*;

/** Opt-in real-MySQL regression. Every test creates and drops only its own disposable database. */
@EnabledIfSystemProperty(named = "be4.mysql", matches = "true")
class Be4Sprint2MySqlTest {
    private static final long HR = 3_000_000_010L;
    private static final long OLD = 3_000_000_011L;
    private static final long NEW = 3_000_000_012L;
    private static final long INTERN = 3_000_000_013L;
    private static final long OTHER_MENTOR = 3_000_000_014L;
    private static final long OTHER_INTERN = 3_000_000_015L;
    private static final Pattern JDBC_URL = Pattern.compile("^(jdbc:mysql://[^/]+/)([^?]+)(\\?.*)?$");
    private static final Pattern TEST_DATABASE = Pattern.compile("be4_sprint2_test_[0-9a-f]{32}");
    private String database;
    private String serverUrl;
    private String testUrl;
    private Properties credentials;
    private boolean created;
    private int enrollmentId;
    private EnrollmentService enrollments;
    private TaskService tasks;
    private WeeklyReportService reports;
    private EvaluationService evaluations;
    private DashboardService dashboards;
    private EnrollmentQueryService queries;

    @BeforeEach
    void setUp() throws Exception {
        Properties settings = new Properties();
        try (Reader reader = Files.newBufferedReader(Path.of("src/main/resources/db.properties"),
                StandardCharsets.UTF_8)) {
            settings.load(reader);
        }
        Matcher matcher = JDBC_URL.matcher(settings.getProperty("db.url", ""));
        if (!matcher.matches()) throw new IllegalStateException("Expected a single-host MySQL JDBC URL");
        Class.forName(settings.getProperty("db.driver", "com.mysql.cj.jdbc.Driver"));
        credentials = new Properties();
        credentials.setProperty("user", settings.getProperty("db.username"));
        credentials.setProperty("password", settings.getProperty("db.password"));
        String options = matcher.group(3) == null ? "" : matcher.group(3);
        serverUrl = matcher.group(1) + options;
        database = "be4_sprint2_test_" + UUID.randomUUID().toString().replace("-", "");
        testUrl = matcher.group(1) + database + options;
        try (Connection connection = DriverManager.getConnection(serverUrl, credentials);
             Statement statement = connection.createStatement()) {
            // No IF NOT EXISTS: cleanup is authorized only after this CREATE succeeds.
            statement.executeUpdate("CREATE DATABASE `" + database + "` CHARACTER SET utf8mb4 "
                    + "COLLATE utf8mb4_unicode_ci");
            created = true;
        }
        try {
            for (String file : List.of("database/schema.sql", "sql/schema_be2.sql",
                    "sql/schema_be3.sql", "sql/schema_be4.sql", "sql/schema_be4_sprint3.sql",
                    "sql/schema_be4_attendance_export.sql")) {
                loadSchema(file);
            }
            seed();
            enrollments = enrollmentService(new ProgramEnrollmentDAO());
            tasks = taskService(new TaskDAO());
            reports = reportService(new ReportFeedbackDAO());
            evaluations = evaluationService(new EvaluationDAO());
            dashboards = new DashboardService(this::open, new DashboardDAO(), new TaskAccessDAO());
            queries = new EnrollmentQueryService(this::open, new Be4EnrollmentDAO(), new TaskAccessDAO());
            enrollmentId = enrollments.enrollIntern(1, 1, HR);
            enrollments.assignMentor(enrollmentId, 1, HR);
        } catch (Exception exception) {
            try { tearDown(); } catch (Exception cleanup) { exception.addSuppressed(cleanup); }
            throw exception;
        }
    }

    @AfterEach
    void tearDown() throws Exception {
        if (!created) return;
        if (!TEST_DATABASE.matcher(database).matches()) {
            throw new IllegalStateException("Refusing cleanup outside the generated test database");
        }
        try (Connection connection = DriverManager.getConnection(serverUrl, credentials);
             Statement statement = connection.createStatement()) {
            statement.executeUpdate("DROP DATABASE `" + database + "`");
            created = false;
        }
    }

    @Test
    void attendanceExportEvidenceMigrationIsRerunnableAndRechecksDatabaseRole() throws Exception {
        loadSchema("sql/schema_be4_attendance_export.sql");
        var service = new AttendanceReportService(this::open, new AttendanceDAO(), new WorkScheduleDAO());
        var date = java.time.LocalDate.of(2026, 10, 10);
        var snapshot = new AttendanceReportService.Snapshot(new AttendanceReportService.Filter(1, enrollmentId, date, date), List.of(), java.time.Instant.now());
        service.recordExport(HR, snapshot, "xlsx");
        assertEquals("ATTENDANCE", scalarString("SELECT report_kind FROM report_export_events WHERE actor_user_id=?", HR));
        assertEquals("2026-10-10", scalarString("SELECT report_from FROM report_export_events WHERE actor_user_id=?", HR));
        assertNull(scalarString("SELECT cohort_from FROM report_export_events WHERE actor_user_id=?", HR));
        assertThrows(SecurityException.class, () -> service.recordExport(INTERN, snapshot, "pdf"));
        execute("UPDATE users SET status='INACTIVE' WHERE user_id=?", HR);
        assertThrows(SecurityException.class, () -> service.recordExport(HR, snapshot, "pdf"));
    }

    @Test
    void evaluationSummariesIdentifyPeopleEvenWithMatchingNamesAndMultipleEnrollments() throws Exception {
        execute("UPDATE intern_profiles SET full_name='Nguyễn Văn An' WHERE profile_id IN (1,2)");
        int secondEnrollment = enrollments.enrollIntern(2, 1, HR);
        enrollments.assignMentor(secondEnrollment, 1, HR);
        evaluations.create(OLD, enrollmentId, BigDecimal.TEN, "Người thứ nhất");
        evaluations.create(OLD, secondEnrollment, BigDecimal.TEN, "Người thứ hai");
        execute("INSERT INTO applications(application_id,intern_id,status,reviewed_by) VALUES(3,1,'APPROVED',?)", HR);
        execute("INSERT INTO program_enrollments(enrollment_id,application_id,program_id,intern_profile_id,status) "
                + "VALUES(100,3,1,1,'COMPLETED')");
        execute("INSERT INTO evaluations(enrollment_id,mentor_id,score,comment) VALUES(100,1,10,'Đợt trước')");

        List<EvaluationSummary> summaries = evaluations.findAllForHr(HR);
        assertEquals(3, summaries.size());
        assertEquals(1, summaries.stream().map(EvaluationSummary::internName).distinct().count());
        assertEquals(2, summaries.stream().map(EvaluationSummary::internProfileId).distinct().count());
        assertEquals(2, summaries.stream().filter(item -> Integer.valueOf(1).equals(item.internProfileId())).count());
        assertTrue(summaries.stream().allMatch(item -> item.internProfileId() != null && item.internProfileId() > 0));
    }

    @Test
    void schemaKeepsEnrollmentAndActorTypesAndRejectsBrokenReferences() throws Exception {
        for (String table : List.of("task_assignments", "weekly_reports", "evaluations")) {
            assertEquals("int", scalarString("SELECT COLUMN_TYPE FROM information_schema.COLUMNS "
                    + "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = ? AND COLUMN_NAME = 'enrollment_id'", table));
            assertEquals("program_enrollments", scalarString("SELECT REFERENCED_TABLE_NAME "
                    + "FROM information_schema.KEY_COLUMN_USAGE WHERE TABLE_SCHEMA = DATABASE() "
                    + "AND TABLE_NAME = ? AND COLUMN_NAME = 'enrollment_id' AND REFERENCED_TABLE_NAME IS NOT NULL", table));
        }
        for (String[] column : List.of(new String[]{"tasks", "created_by_user_id"},
                new String[]{"report_feedbacks", "reviewer_user_id"})) {
            assertEquals("bigint", scalarString("SELECT COLUMN_TYPE FROM information_schema.COLUMNS "
                    + "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = ? AND COLUMN_NAME = ?", (Object[]) column));
        }
        assertThrows(SQLIntegrityConstraintViolationException.class,
                () -> execute("INSERT INTO task_assignments(task_id,enrollment_id) VALUES(999,?)", enrollmentId));
        assertThrows(SQLIntegrityConstraintViolationException.class,
                () -> execute("INSERT INTO weekly_reports(enrollment_id,week_number) VALUES(999,1)"));
        Task task = tasks.createTask(OLD, "Nhiệm vụ kiểm tra", null, null);
        tasks.assignTask(OLD, task.getTaskId(), enrollmentId);
        assertThrows(IllegalStateException.class, () -> tasks.assignTask(OLD, task.getTaskId(), enrollmentId));
        reports.createDraft(INTERN, enrollmentId, (short) 1, "Báo cáo");
        assertThrows(IllegalStateException.class,
                () -> reports.createDraft(INTERN, enrollmentId, (short) 1, "Không được trùng"));
    }

    @Test
    void handoverPreservesHistoryAndMovesAccessToNewMentor() throws Exception {
        Task task = tasks.createTask(OLD, "Hoàn thành tài liệu tiếng Việt", "Nội dung có dấu", null);
        TaskAssignment assignment = tasks.assignTask(OLD, task.getTaskId(), enrollmentId);
        tasks.updateProgress(INTERN, assignment.getAssignmentId(), 30, "Đang thực hiện");
        WeeklyReport report = submittedReport((short) 1);
        ReportFeedback feedback = reports.review(OLD, report.getReportId(), "Nhận xét trước khi bàn giao", 4);
        Evaluation evaluation = evaluations.create(OLD, enrollmentId, new BigDecimal("85.50"), "Đạt yêu cầu");

        changeTo(2);

        assertEquals(enrollmentId, queries.findForIntern(INTERN).getFirst().enrollmentId());
        assertEquals(enrollmentId, queries.findForMentor(NEW).getFirst().enrollmentId());
        assertTrue(queries.findForMentor(OLD).isEmpty());
        assertEquals(task.getTaskId(), tasks.findTasksForMentor(NEW, enrollmentId).getFirst().getTaskId());
        assertEquals(OLD, tasks.findTasksForMentor(NEW, enrollmentId).getFirst().getCreatedByUserId());
        assertEquals(assignment.getAssignmentId(), tasks.findAssignmentsForIntern(INTERN, enrollmentId)
                .getFirst().getAssignmentId());
        assertEquals(30, tasks.findProgressHistory(NEW, assignment.getAssignmentId())
                .getFirst().getCompletionPercentage());
        assertEquals("Nội dung có dấu", tasks.findTasksForIntern(INTERN, enrollmentId).getFirst().getDescription());
        assertEquals(report.getReportId(), reports.findForMentor(NEW, enrollmentId).getFirst().getReportId());
        assertEquals(feedback.getFeedbackId(), reports.findFeedback(NEW, report.getReportId())
                .getFirst().getFeedbackId());
        assertEquals(OLD, reports.findFeedback(INTERN, report.getReportId()).getFirst().getReviewerUserId());
        assertEquals(evaluation.getEvaluationId(), evaluations.findForActor(NEW, enrollmentId).getEvaluationId());
        assertEquals(1, evaluations.findForActor(INTERN, enrollmentId).getMentorId());
        assertEquals("Mentor cũ", evaluations.findAllForHr(HR).getFirst().mentorName());
        assertEquals(2, historyCount());
        assertEquals(2, currentMentorId());

        assertThrows(SecurityException.class, () -> tasks.findTasksForMentor(OLD, enrollmentId));
        assertThrows(SecurityException.class, () -> tasks.findAssignmentsForMentor(OLD, enrollmentId));
        assertThrows(SecurityException.class, () -> tasks.findProgressHistory(OLD, assignment.getAssignmentId()));
        assertThrows(SecurityException.class, () -> reports.findForMentor(OLD, enrollmentId));
        assertThrows(SecurityException.class, () -> reports.findFeedback(OLD, report.getReportId()));
        assertThrows(SecurityException.class, () -> evaluations.findForActor(OLD, enrollmentId));
        assertThrows(SecurityException.class, () -> tasks.assignTask(OLD, task.getTaskId(), enrollmentId));
        assertThrows(IllegalStateException.class,
                () -> evaluations.create(NEW, enrollmentId, BigDecimal.TEN, "Không được ghi đè"));

        Task newTask = tasks.createTask(NEW, "Nhiệm vụ mentor mới", null, null);
        assertEquals(enrollmentId, tasks.assignTask(NEW, newTask.getTaskId(), enrollmentId).getEnrollmentId());
        tasks.updateProgress(INTERN, assignment.getAssignmentId(), 100, "Hoàn thành sau bàn giao");
        assertEquals(2, tasks.findProgressHistory(INTERN, assignment.getAssignmentId()).size());
        assertEquals(TaskAssignmentStatus.DONE, tasks.findAssignmentsForMentor(NEW, enrollmentId)
                .stream().filter(value -> value.getAssignmentId() == assignment.getAssignmentId()).findFirst()
                .orElseThrow().getStatus());
    }

    @Test
    void newMentorCanReviewReportSubmittedBeforeHandover() throws Exception {
        WeeklyReport report = submittedReport((short) 1);
        changeTo(2);
        assertThrows(SecurityException.class, () -> reports.review(OLD, report.getReportId(), "Không còn quyền", 3));
        assertEquals(NEW, reports.review(NEW, report.getReportId(), "Đã tiếp nhận báo cáo", 5).getReviewerUserId());
        assertEquals(WeeklyReportStatus.REVIEWED, reports.findForIntern(INTERN, enrollmentId).getFirst().getStatus());
        reports.review(NEW, report.getReportId(), "Bổ sung nhận xét lần hai", 5);
        assertEquals(2, reports.findFeedback(INTERN, report.getReportId()).size());
    }

    @Test
    void newMentorCreatesEvaluationWithoutRewritingEnrollment() throws Exception {
        changeTo(2);
        assertThrows(SecurityException.class,
                () -> evaluations.create(OLD, enrollmentId, BigDecimal.TEN, "Mentor cũ"));
        Evaluation result = evaluations.create(NEW, enrollmentId, new BigDecimal("90.25"), "Tốt");
        assertEquals(2, result.getMentorId());
        assertEquals(enrollmentId, result.getEnrollmentId());
        assertEquals(new BigDecimal("90.25"), evaluations.findForActor(INTERN, enrollmentId).getScore());
        assertEquals("Mentor mới", evaluations.findAllForHr(HR).getFirst().mentorName());
    }

    @Test
    void dashboardTracksCurrentMentorAndKeepsHistoricalEvaluationAuthor() throws Exception {
        Task task = tasks.createTask(OLD, "Nhiệm vụ", null, null);
        TaskAssignment assignment = tasks.assignTask(OLD, task.getTaskId(), enrollmentId);
        tasks.updateProgress(INTERN, assignment.getAssignmentId(), 20, "Đang làm");
        submittedReport((short) 1);
        evaluations.create(OLD, enrollmentId, BigDecimal.TEN, "Lịch sử tác giả");
        assertEquals(1, dashboards.getMentorDashboard(OLD).activeInterns());

        changeTo(2);
        MentorDashboard old = dashboards.getMentorDashboard(OLD);
        MentorDashboard next = dashboards.getMentorDashboard(NEW);
        assertEquals(0, old.activeInterns());
        assertEquals(0, old.tasksInProgress());
        assertEquals(0, old.reportsSubmitted());
        assertEquals(1, old.evaluationsCompleted()); // Historical authorship, not current workload.
        assertEquals(1, next.activeInterns());
        assertEquals(1, next.tasksInProgress());
        assertEquals(1, next.reportsSubmitted());
        assertEquals(0, next.evaluationsCompleted());
        HrDashboard hr = dashboards.getHrDashboard(HR);
        assertEquals(1, hr.activeEnrollments());
        assertEquals(1, hr.totalInterns());
        assertEquals(1, hr.activeInternsByMentor().size());
        assertEquals("Mentor mới (#2)", hr.activeInternsByMentor().getFirst().label());
        assertEquals(1, hr.activeInternsByMentor().getFirst().count());

        changeTo(1);
        assertEquals(1, dashboards.getMentorDashboard(OLD).activeInterns());
        assertEquals(1, dashboards.getMentorDashboard(OLD).tasksInProgress());
        assertEquals(0, dashboards.getMentorDashboard(NEW).activeInterns());
        assertEquals(3, historyCount());
        assertEquals(1, dashboards.getHrDashboard(HR).activeInternsByMentor().getFirst().count());
    }

    @Test
    void failedHandoverLeavesOldAccessAndDataIntact() throws Exception {
        Task task = tasks.createTask(OLD, "Nhiệm vụ chưa bàn giao", null, null);
        tasks.assignTask(OLD, task.getTaskId(), enrollmentId);
        WeeklyReport report = submittedReport((short) 1);
        assertThrows(IllegalStateException.class, () -> changeTo(3)); // Other department.
        assertEquals(1, currentMentorId());
        assertEquals(1, historyCount());
        assertEquals(task.getTaskId(), tasks.findTasksForMentor(OLD, enrollmentId).getFirst().getTaskId());
        assertEquals(OLD, reports.review(OLD, report.getReportId(), "Mentor cũ vẫn phụ trách", 5).getReviewerUserId());
        assertThrows(SecurityException.class, () -> reports.findForMentor(NEW, enrollmentId));
        assertEquals(1, dashboards.getMentorDashboard(OLD).activeInterns());
    }

    @Test
    void completedEnrollmentIsReadableButNotWritableAndCancelledIsExcludedFromHrRate() throws Exception {
        Task task = tasks.createTask(OLD, "Nhiệm vụ", null, null);
        TaskAssignment assignment = tasks.assignTask(OLD, task.getTaskId(), enrollmentId);
        WeeklyReport report = submittedReport((short) 1);
        Evaluation evaluation = evaluations.create(OLD, enrollmentId, BigDecimal.TEN, "Kết thúc");
        execute("UPDATE program_enrollments SET status='COMPLETED',completed_at=CURRENT_TIMESTAMP "
                + "WHERE enrollment_id=?", enrollmentId);
        execute("INSERT INTO program_enrollments(application_id,program_id,intern_profile_id,status) "
                + "VALUES(2,1,2,'CANCELLED')");
        assertEquals(evaluation.getEvaluationId(), evaluations.findForActor(INTERN, enrollmentId).getEvaluationId());
        assertEquals(1, tasks.findTasksForMentor(OLD, enrollmentId).size());
        assertEquals(1, reports.findForIntern(INTERN, enrollmentId).size());
        assertThrows(SecurityException.class, () -> tasks.assignTask(OLD, task.getTaskId(), enrollmentId));
        assertThrows(SecurityException.class, () -> tasks.updateProgress(INTERN, assignment.getAssignmentId(), 50, null));
        assertThrows(SecurityException.class, () -> reports.createDraft(INTERN, enrollmentId, (short) 2, "Mới"));
        assertThrows(SecurityException.class, () -> reports.review(OLD, report.getReportId(), "Mới", 5));
        assertThrows(SecurityException.class, () -> evaluations.create(OLD, enrollmentId, BigDecimal.TEN, null));
        assertEquals(0, dashboards.getMentorDashboard(OLD).activeInterns());
        assertEquals(0, dashboards.getMentorDashboard(OLD).tasksTodo());
        assertEquals(0, dashboards.getMentorDashboard(OLD).reportsSubmitted());
        HrDashboard hr = dashboards.getHrDashboard(HR);
        assertEquals(1, hr.totalInterns());
        assertEquals(1, hr.completedEnrollments());
        assertEquals(new BigDecimal("100.00"), hr.completionRatePercent());
        assertTrue(hr.activeInternsByMentor().isEmpty());
    }

    @Test
    void outsidersAndLockedAccountsCannotReadOrWriteAnotherEnrollment() throws Exception {
        Task task = tasks.createTask(OLD, "Nhiệm vụ riêng", null, null);
        TaskAssignment assignment = tasks.assignTask(OLD, task.getTaskId(), enrollmentId);
        WeeklyReport report = submittedReport((short) 1);
        assertThrows(SecurityException.class, () -> tasks.findTasksForIntern(OTHER_INTERN, enrollmentId));
        assertThrows(SecurityException.class, () -> tasks.updateProgress(OTHER_INTERN, assignment.getAssignmentId(), 50, null));
        assertThrows(SecurityException.class, () -> reports.findFeedback(OTHER_MENTOR, report.getReportId()));
        assertThrows(SecurityException.class, () -> evaluations.findForActor(OTHER_INTERN, enrollmentId));
        assertThrows(SecurityException.class, () -> evaluations.findAllForHr(INTERN));
        assertThrows(SecurityException.class, () -> dashboards.getHrDashboard(NEW));
        execute("UPDATE users SET status='LOCKED' WHERE user_id=?", OLD);
        assertThrows(SecurityException.class, () -> tasks.findTasksForMentor(OLD, enrollmentId));
        assertThrows(SecurityException.class, () -> reports.review(OLD, report.getReportId(), "Bị khóa", 3));
        assertThrows(SecurityException.class, () -> evaluations.create(OLD, enrollmentId, BigDecimal.TEN, null));
        assertThrows(SecurityException.class, () -> queries.findForMentor(OLD));
    }

    enum MentorWrite { TASK, FEEDBACK, EVALUATION }

    @ParameterizedTest
    @EnumSource(MentorWrite.class)
    void mentorWriteAndHandoverCannotCommitAcrossAStalePermissionCheck(MentorWrite operation) throws Exception {
        CountDownLatch authorized = new CountDownLatch(1);
        CountDownLatch allowWrite = new CountDownLatch(1);
        CountDownLatch changeAttempted = new CountDownLatch(1);
        CountDownLatch changeCompleted = new CountDownLatch(1);
        Callable<?> write;
        switch (operation) {
            case TASK -> {
                Task task = tasks.createTask(OLD, "Giao nhiệm vụ đồng thời", null, null);
                TaskService gated = taskService(new TaskDAO() {
                    @Override public Task findById(Connection connection, long taskId) throws SQLException {
                        pause(authorized, allowWrite);
                        return super.findById(connection, taskId);
                    }
                });
                write = () -> gated.assignTask(OLD, task.getTaskId(), enrollmentId);
            }
            case FEEDBACK -> {
                WeeklyReport report = submittedReport((short) 1);
                WeeklyReportService gated = reportService(new ReportFeedbackDAO() {
                    @Override public ReportFeedback create(Connection connection, long reportId,
                            long reviewer, String comment, Integer rating) throws SQLException {
                        pause(authorized, allowWrite);
                        return super.create(connection, reportId, reviewer, comment, rating);
                    }
                });
                write = () -> gated.review(OLD, report.getReportId(), "Phản hồi đồng thời", 4);
            }
            case EVALUATION -> {
                EvaluationService gated = evaluationService(new EvaluationDAO() {
                    @Override public Evaluation create(Connection connection, int enrollment, int mentor,
                            BigDecimal score, String comment) throws SQLException {
                        pause(authorized, allowWrite);
                        return super.create(connection, enrollment, mentor, score, comment);
                    }
                });
                write = () -> gated.create(OLD, enrollmentId, BigDecimal.TEN, "Đánh giá đồng thời");
            }
            default -> throw new AssertionError(operation);
        }
        EnrollmentService handover = enrollmentService(new ProgramEnrollmentDAO() {
            @Override public ProgramEnrollment getEnrollmentByIdForUpdate(int id, Connection connection)
                    throws SQLException {
                changeAttempted.countDown();
                return super.getEnrollmentByIdForUpdate(id, connection);
            }
        });
        ExecutorService workers = Executors.newFixedThreadPool(2);
        try {
            Future<?> writeResult = workers.submit(write);
            assertTrue(authorized.await(10, SECONDS), "Mentor write did not reach the authorized stage");
            Future<?> changeResult = workers.submit(() -> {
                try { return handover.changeMentor(enrollmentId, 2, "Bàn giao trong khi thao tác", HR); }
                finally { changeCompleted.countDown(); }
            });
            assertTrue(changeAttempted.await(10, SECONDS));
            assertFalse(changeCompleted.await(400, TimeUnit.MILLISECONDS),
                    "HR committed a handover while an authorized mentor write was still in flight");
            allowWrite.countDown();
            assertNotNull(writeResult.get(15, SECONDS));
            assertNotNull(changeResult.get(15, SECONDS));
            assertEquals(2, currentMentorId());
            assertThrows(SecurityException.class,
                    () -> evaluations.create(OLD, enrollmentId, BigDecimal.TEN, "Không còn quyền"));
        } finally {
            allowWrite.countDown();
            workers.shutdownNow();
            assertTrue(workers.awaitTermination(10, SECONDS), "Database workers did not finish before cleanup");
        }
    }

    private static void pause(CountDownLatch entered, CountDownLatch release) throws SQLException {
        entered.countDown();
        try {
            if (!release.await(10, SECONDS)) throw new SQLException("Timed out waiting for the regression gate");
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new SQLException("Regression gate interrupted", exception);
        }
    }

    @ParameterizedTest
    @EnumSource(MentorWrite.class)
    void requestWaitingBehindHandoverRechecksAndRejectsOldMentor(MentorWrite operation) throws Exception {
        Task task = tasks.createTask(OLD, "Nhiệm vụ trong lúc bàn giao", null, null);
        WeeklyReport report = submittedReport((short) 1);
        CountDownLatch lockAttempted = new CountDownLatch(1);
        TaskAccessDAO observedAccess = new TaskAccessDAO() {
            @Override public void lockEnrollmentForMentorWrite(Connection connection, int enrollment)
                    throws SQLException {
                lockAttempted.countDown();
                super.lockEnrollmentForMentorWrite(connection, enrollment);
            }
        };
        Callable<?> write = switch (operation) {
            case TASK -> () -> new TaskService(this::open, new TaskDAO(), new TaskAssignmentDAO(),
                    new TaskProgressDAO(), observedAccess).assignTask(OLD, task.getTaskId(), enrollmentId);
            case FEEDBACK -> () -> new WeeklyReportService(this::open, new WeeklyReportDAO(),
                    new ReportFeedbackDAO(), observedAccess).review(OLD, report.getReportId(), "Mentor cũ", 3);
            case EVALUATION -> () -> new EvaluationService(this::open, new EvaluationDAO(), observedAccess)
                    .create(OLD, enrollmentId, BigDecimal.TEN, "Mentor cũ");
        };
        ExecutorService worker = Executors.newSingleThreadExecutor();
        // Simulate BE3's uncommitted handover with its actual DAO and lock order.
        try (Connection hrConnection = open()) {
            hrConnection.setAutoCommit(false);
            new ProgramEnrollmentDAO().getEnrollmentByIdForUpdate(enrollmentId, hrConnection);
            Future<?> result = worker.submit(write);
            try {
                assertTrue(lockAttempted.await(10, SECONDS));
                assertFalse(result.isDone());
                MentorAssignmentDAO assignmentDAO = new MentorAssignmentDAO();
                assertTrue(assignmentDAO.endActiveAssignment(enrollmentId, "Bàn giao trước thao tác", hrConnection));
                MentorAssignment next = new MentorAssignment();
                next.setEnrollmentId(enrollmentId);
                next.setMentorId(2);
                next.setAssignedBy(HR);
                assignmentDAO.createAssignment(next, hrConnection);
                hrConnection.commit();
                ExecutionException rejected = assertThrows(ExecutionException.class, () -> result.get(15, SECONDS));
                assertInstanceOf(SecurityException.class, rejected.getCause());
                assertTrue(tasks.findAssignmentsForMentor(NEW, enrollmentId).isEmpty());
                assertTrue(reports.findFeedback(NEW, report.getReportId()).isEmpty());
                assertNull(evaluations.findForActor(NEW, enrollmentId));
            } finally {
                hrConnection.rollback(); // Release any lock even if the test fails before commit.
                worker.shutdownNow();
                assertTrue(worker.awaitTermination(10, SECONDS));
            }
        }
    }

    private Connection open() throws SQLException {
        return DriverManager.getConnection(testUrl, credentials);
    }

    private EnrollmentService enrollmentService(ProgramEnrollmentDAO enrollmentDAO) {
        return new EnrollmentService(new ApplicationDAO(), enrollmentDAO, new MentorAssignmentDAO(),
                new InternshipProgramDAO(), new MentorDAO(), this::open);
    }

    private TaskService taskService(TaskDAO taskDAO) {
        return new TaskService(this::open, taskDAO, new TaskAssignmentDAO(), new TaskProgressDAO(), new TaskAccessDAO());
    }

    private WeeklyReportService reportService(ReportFeedbackDAO feedbackDAO) {
        return new WeeklyReportService(this::open, new WeeklyReportDAO(), feedbackDAO, new TaskAccessDAO());
    }

    private EvaluationService evaluationService(EvaluationDAO evaluationDAO) {
        return new EvaluationService(this::open, evaluationDAO, new TaskAccessDAO());
    }

    private WeeklyReport submittedReport(short week) throws SQLException {
        WeeklyReport draft = reports.createDraft(INTERN, enrollmentId, week, "Nội dung báo cáo tuần " + week);
        return reports.submit(INTERN, draft.getReportId());
    }

    private Sprint3WorkflowService sprint3() { return new Sprint3WorkflowService(this::open,new TaskAccessDAO(),new Sprint3WorkflowDAO()); }

    @Test void sprint3AtomicTaskValidatesDeadlineAndProgressRecordsActualActor() throws Exception {
        assertThrows(IllegalArgumentException.class,()->sprint3().createAssignedTask(OLD,enrollmentId,"Task","Mô tả",java.time.LocalDate.of(2027,1,1)));
        assertEquals("0",scalarString("SELECT COUNT(*) FROM tasks"));
        var created=sprint3().createAssignedTask(OLD,enrollmentId,"Task","Mô tả",java.time.LocalDate.of(2026,11,1));
        var entry=sprint3().updateProgress(INTERN,created.assignment().getAssignmentId(),60,"Đã làm xong phần chính");
        assertEquals(String.valueOf(INTERN),scalarString("SELECT actor_user_id FROM task_progress_actors WHERE progress_id=?",entry.getProgressId()));
        assertEquals("IN_PROGRESS",scalarString("SELECT status FROM task_assignments WHERE assignment_id=?",created.assignment().getAssignmentId()));
        assertThrows(SecurityException.class,()->sprint3().updateProgress(OTHER_INTERN,created.assignment().getAssignmentId(),100,"Other"));
    }

    @Test void sprint3DraftIsPrivateAndRequiresAllSectionsBeforeSubmit() throws Exception {
        var r=sprint3().createDraft(INTERN,enrollmentId,(short)1,"", "", "");
        assertTrue(reports.findForMentor(OLD,enrollmentId).isEmpty());
        assertThrows(IllegalArgumentException.class,()->sprint3().submit(INTERN,r.getReportId()));
        sprint3().updateDraft(INTERN,r.getReportId(),"Hoàn thành task","Không có khó khăn","Kiểm tra tuần tới");
        sprint3().submit(INTERN,r.getReportId());
        assertEquals(1,reports.findForMentor(OLD,enrollmentId).size());
        assertThrows(IllegalStateException.class,()->sprint3().updateDraft(INTERN,r.getReportId(),"Sửa","Không","Sửa"));
        assertThrows(IllegalArgumentException.class,()->sprint3().createDraft(INTERN,enrollmentId,(short)100,"a","b","c"));
        assertEquals("Kiểm tra tuần tới",scalarString("SELECT next_plan FROM weekly_report_sections WHERE report_id=?",r.getReportId()));
    }

    @Test void sprint3MultipleAssessmentsKeepAuthorsAcrossHandover() throws Exception {
        var first=sprint3().assess(OLD,enrollmentId,4,5,"Nhận xét mentor cũ");changeTo(2);
        assertThrows(SecurityException.class,()->sprint3().assess(OLD,enrollmentId,5,5,"Không còn quyền"));
        var second=sprint3().assess(NEW,enrollmentId,5,4,"Nhận xét mentor mới");
        var history=sprint3().assessments(INTERN,enrollmentId);assertEquals(2,history.size());assertEquals(second.assessmentId(),history.getFirst().assessmentId());assertEquals(first.mentorId(),history.get(1).mentorId());
        assertEquals(2,sprint3().assessments(HR,enrollmentId).size());assertThrows(SecurityException.class,()->sprint3().assessments(OTHER_INTERN,enrollmentId));
    }

    @Test void sprint3TerminalEnrollmentHasReadableHistoryButRejectsWrites() throws Exception {
        sprint3().assess(OLD,enrollmentId,4,5,"Đã đánh giá");
        execute("UPDATE program_enrollments SET status='COMPLETED',completed_at=CURRENT_TIMESTAMP WHERE enrollment_id=?",enrollmentId);
        execute("UPDATE mentor_assignments SET ended_at=CURRENT_TIMESTAMP WHERE enrollment_id=?",enrollmentId);
        assertEquals(1,sprint3().assessments(OLD,enrollmentId).size());assertEquals("COMPLETED",queries.findForMentor(OLD).getFirst().status());
        assertThrows(SecurityException.class,()->sprint3().assess(OLD,enrollmentId,5,5,"Again"));
        assertThrows(SecurityException.class,()->sprint3().createDraft(INTERN,enrollmentId,(short)2,"a","b","c"));
    }

    @Test void sprint3EndTermDoesNotMultiplyDetailRowsAndExportsAreAuthorized() throws Exception {
        evaluations.create(OLD,enrollmentId,new BigDecimal("82.50"),"Điểm cũ được giữ");
        var t=sprint3().createAssignedTask(OLD,enrollmentId,"Task","Mô tả",java.time.LocalDate.of(2026,11,1));sprint3().updateProgress(INTERN,t.assignment().getAssignmentId(),100,"Done");
        var r=sprint3().createDraft(INTERN,enrollmentId,(short)1,"Kết quả","Không","Kế hoạch");sprint3().submit(INTERN,r.getReportId());reports.review(OLD,r.getReportId(),"Lần 1",4);reports.review(OLD,r.getReportId(),"Lần 2",5);
        sprint3().assess(OLD,enrollmentId,4,5,"Lần 1");sprint3().assess(OLD,enrollmentId,5,5,"Lần 2");
        var service=new EndTermReportService(this::open);var page=service.list(HR,new EndTermReportService.Filter(1,null,null),1,20);assertEquals(1,page.items().size());var row=page.items().getFirst();assertEquals(1,row.taskCount());assertEquals(1,row.tasksDone());assertEquals(1,row.submittedReports());assertEquals(1,row.reviewedReports());assertEquals(2,row.assessments().size());assertEquals(new BigDecimal("82.50"),row.legacyScore());
        assertThrows(SecurityException.class,()->service.exportSnapshot(INTERN,new EndTermReportService.Filter(null,null,null)));
        var snapshot=service.exportSnapshot(HR,new EndTermReportService.Filter(1,null,null));service.recordExport(HR,snapshot,"xlsx");assertEquals("1",scalarString("SELECT COUNT(*) FROM report_export_events WHERE actor_user_id=?",HR));
    }

    @Test void sprint3ProgressEvidenceFailureRollsBackPercentageAndAssignment() throws Exception {
        var t=sprint3().createAssignedTask(OLD,enrollmentId,"Task","Mô tả",java.time.LocalDate.of(2026,11,1));
        execute("CREATE TRIGGER reject_actor BEFORE INSERT ON task_progress_actors FOR EACH ROW SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='fixture failure'");
        assertThrows(SQLException.class,()->sprint3().updateProgress(INTERN,t.assignment().getAssignmentId(),100,"Done"));
        assertEquals("0",scalarString("SELECT COUNT(*) FROM task_progress"));assertEquals("TODO",scalarString("SELECT status FROM task_assignments WHERE assignment_id=?",t.assignment().getAssignmentId()));
    }

    @Test void sprint3EvaluationMigrationCanRunTwiceAndPreservesLegacyContract() throws Exception {
        evaluations.create(OLD,enrollmentId,new BigDecimal("75.50"),"Nhận xét cũ");
        var criteria=sprint3().assess(OLD,enrollmentId,4,5,"Hai tiêu chí mới");
        loadSchema("sql/schema_be4_sprint3.sql");
        assertEquals(new BigDecimal("75.50"),evaluations.findForActor(INTERN,enrollmentId).getScore());
        assertEquals(criteria.assessmentId(),sprint3().assessments(INTERN,enrollmentId).getFirst().assessmentId());
        assertEquals(1,evaluations.findAllForHr(HR).size());
        assertThrows(Exception.class,()->execute("INSERT INTO evaluations(enrollment_id,mentor_id,score) VALUES(?,?,?)",enrollmentId,1,80));
        assertThrows(SQLException.class,()->execute("INSERT INTO evaluations(enrollment_id,mentor_id,skill_score,attitude_score,comment) VALUES(?,?,0,5,'Sai')",enrollmentId,1));
        assertEquals("2",scalarString("SELECT COUNT(*) FROM evaluations WHERE enrollment_id=?",enrollmentId));
    }

    @Test void statisticsExportUsesBe3AggregatorAndRestrictsScope()throws Exception {
        var service=new ProgramStatisticsReportService(this::open);
        var filter=new ProgramStatisticsDAO.Filter(1,"ACTIVE",null,null);
        var snapshot=service.snapshot(HR,filter);assertEquals(1,snapshot.summary().interns());assertEquals(1,snapshot.summary().enrollments());assertEquals(0,snapshot.summary().completed());
        try(var c=open()){assertEquals(new ProgramStatisticsDAO().summarize(c,filter),snapshot.summary());}
        assertThrows(SecurityException.class,()->service.snapshot(INTERN,filter));
        service.recordExport(HR,snapshot,"xlsx");assertEquals("PROGRAM_STATISTICS",scalarString("SELECT report_kind FROM report_export_events WHERE actor_user_id=?",HR));
    }

    private void changeTo(int mentorId) throws SQLException {
        enrollments.changeMentor(enrollmentId, mentorId, "Bàn giao hướng dẫn", HR);
    }

    private int currentMentorId() throws SQLException {
        try (Connection connection = open()) {
            return new MentorAssignmentDAO().getActiveAssignment(enrollmentId, connection).getMentorId();
        }
    }

    private int historyCount() throws SQLException {
        return Integer.parseInt(scalarString("SELECT COUNT(*) FROM mentor_assignments WHERE enrollment_id=?", enrollmentId));
    }

    private void loadSchema(String file) throws Exception {
        String schema = Files.readString(Path.of(file), StandardCharsets.UTF_8)
                .replace("\uFEFF", "").replace("internship_management", database)
                .replaceAll("(?m)^\\s*--.*$", "");
        try (Connection connection = open(); Statement statement = connection.createStatement()) {
            for (String sql : schema.split(";")) {
                if (!sql.isBlank()) statement.execute(sql.trim());
            }
        }
    }

    private void seed() throws SQLException {
        seedUser(HR, "hr_test", "HR kiểm tra", "HR");
        seedUser(OLD, "mentor_old", "Mentor cũ", "MENTOR");
        seedUser(NEW, "mentor_new", "Mentor mới", "MENTOR");
        seedUser(INTERN, "intern_test", "Thực tập sinh kiểm tra", "INTERN");
        seedUser(OTHER_MENTOR, "mentor_other", "Mentor khác", "MENTOR");
        seedUser(OTHER_INTERN, "intern_other", "Thực tập sinh khác", "INTERN");
        execute("INSERT INTO departments(department_id,department_name) VALUES(1,'Phòng kỹ thuật'),(2,'Phòng khác')");
        execute("INSERT INTO internship_programs(program_id,program_name,department_id,start_date,end_date,max_interns,status) "
                + "VALUES(1,'Chương trình kiểm tra',1,'2026-09-01','2026-12-31',10,'ACTIVE')");
        execute("INSERT INTO mentors(mentor_id,user_id,department_id,max_mentees,status) "
                + "VALUES(1,?,1,5,'ACTIVE'),(2,?,1,5,'ACTIVE'),(3,?,2,5,'ACTIVE')", OLD, NEW, OTHER_MENTOR);
        seedProfile(1, INTERN, "Thực tập sinh kiểm tra");
        seedProfile(2, OTHER_INTERN, "Thực tập sinh khác");
        execute("INSERT INTO applications(application_id,intern_id,status,reviewed_by) "
                + "VALUES(1,1,'APPROVED',?),(2,2,'APPROVED',?)", HR, HR);
    }

    private void seedUser(long id, String username, String name, String role) throws SQLException {
        execute("INSERT INTO users(user_id,username,email,password_hash,full_name,role_id,status) "
                + "SELECT ?,?,?,'test-only-non-login-hash',?,role_id,'ACTIVE' FROM roles WHERE role_name=?",
                id, username, username + "@example.invalid", name, role);
    }

    private void seedProfile(int id, long userId, String name) throws SQLException {
        execute("INSERT INTO intern_profiles(profile_id,user_id,full_name,phone,email,university,major,status) "
                + "VALUES(?,?,?,'0000000000',?,'Trường kiểm tra','Công nghệ thông tin','ACTIVE')",
                id, userId, name, "intern" + id + "@example.invalid");
    }

    private void execute(String sql, Object... values) throws SQLException {
        try (Connection connection = open(); PreparedStatement statement = connection.prepareStatement(sql)) {
            bind(statement, values);
            statement.executeUpdate();
        }
    }

    private String scalarString(String sql, Object... values) throws SQLException {
        try (Connection connection = open(); PreparedStatement statement = connection.prepareStatement(sql)) {
            bind(statement, values);
            try (ResultSet rows = statement.executeQuery()) {
                assertTrue(rows.next());
                return rows.getString(1);
            }
        }
    }

    private static void bind(PreparedStatement statement, Object... values) throws SQLException {
        for (int index = 0; index < values.length; index++) statement.setObject(index + 1, values[index]);
    }
}
