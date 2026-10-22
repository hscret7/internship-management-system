package com.internship.service;

import com.internship.dao.*;
import com.internship.model.*;
import com.internship.util.DBConnection;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.*;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.*;

/** Opt-in: uses local credentials but redirects this test JVM to a new disposable database. */
@EnabledIfSystemProperty(named = "be3.mysql", matches = "true")
class Be3MySqlIntegrationTest {
    private static String database;
    private static String adminUrl;
    private static String username;
    private static String password;
    private static String originalUrl;
    private static Field urlField;
    private static boolean created;

    @BeforeAll
    static void prepareIsolatedDatabase() throws Exception {
        Properties properties = new Properties();
        try (var input = DBConnection.class.getClassLoader().getResourceAsStream("db.properties")) {
            assertNotNull(input, "Create local db.properties before opting into MySQL tests");
            properties.load(new InputStreamReader(input, StandardCharsets.UTF_8));
        }
        // DBConnection belongs to BE1: no production changes. The override lives only in this JVM.
        urlField = DBConnection.class.getDeclaredField("url");
        urlField.setAccessible(true);
        originalUrl = (String) urlField.get(null);
        var match = java.util.regex.Pattern.compile("^(jdbc:mysql://[^/]+/)[^?]+(.*)$").matcher(originalUrl);
        assertTrue(match.matches(), "This integration check supports a single-host MySQL JDBC URL");
        username = properties.getProperty("db.username"); password = properties.getProperty("db.password");
        adminUrl = match.group(1) + match.group(2);
        database = "be3_sprint2_test_" + UUID.randomUUID().toString().replace("-", "");
        try (Connection admin = DriverManager.getConnection(adminUrl, username, password);
             Statement statement = admin.createStatement()) {
            statement.execute("CREATE DATABASE `" + database + "` CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci");
            created = true;
        }
        urlField.set(null, match.group(1) + database + match.group(2));
        try (Connection connection = DBConnection.getConnection(); Statement statement = connection.createStatement()) {
            for (String schema : new String[]{"database/schema.sql", "sql/schema_be2.sql", "sql/schema_be3.sql", "sql/schema_be4.sql"}) {
                String sql = Files.readString(Path.of(schema), StandardCharsets.UTF_8)
                        .replace("internship_management", database).replaceAll("(?m)^\\s*--.*$", "");
                for (String command : sql.split(";")) {
                    if (!command.isBlank()) statement.execute(command);
                }
            }
            for (int id = 1; id <= 7; id++) {
                String role = id == 1 ? "HR" : id >= 4 && id <= 6 ? "MENTOR" : "INTERN";
                statement.executeUpdate("INSERT INTO users(user_id, username, email, password_hash, full_name, role_id) "
                        + "SELECT " + id + ", 'be3_test_" + id + "', 'test" + id + "@example.test', 'not-a-login-hash', "
                        + "'Test User " + id + "', role_id FROM roles WHERE role_name='" + role + "'");
            }
            for (int id : new int[]{2, 3, 7}) {
                statement.executeUpdate("INSERT INTO intern_profiles(profile_id, user_id, full_name, phone, email, university, major) "
                        + "VALUES(" + id + "," + id + ",'Test Intern','0000000000','intern" + id + "@example.test','ICTU','IT')");
                statement.executeUpdate("INSERT INTO applications(application_id, intern_id, status) VALUES(" + id + "," + id + ",'APPROVED')");
            }
        }
    }

    @AfterAll
    static void cleanOnlyOurDatabase() throws Exception {
        if (urlField != null && originalUrl != null) urlField.set(null, originalUrl);
        if (created) {
            assertTrue(database.matches("be3_sprint2_test_[0-9a-f]{32}"));
            try (Connection admin = DriverManager.getConnection(adminUrl, username, password);
                 Statement statement = admin.createStatement()) {
                statement.execute("DROP DATABASE `" + database + "`");
            }
        }
    }

    @Test
    void realTransactionsCapacityHistoryOverviewAndConcurrentDuplicates() throws Exception {
        CatalogService catalog = new CatalogService(); EnrollmentService service = new EnrollmentService();
        int department = catalog.createDepartment("Engineering", null);
        int otherDepartment = catalog.createDepartment("Operations", null);
        Date start = Date.valueOf("2026-10-01"); Date end = Date.valueOf("2026-10-31");
        int program = catalog.createProgram("  Java\t Internship  ", department, "Original", start, end, 5);
        assertEquals("Java Internship", catalog.getProgram(program).getProgramName());
        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "UPDATE internship_programs SET program_name=? WHERE program_id=?")) {
            statement.setString(1, "\t Java  Internship \t"); statement.setInt(2, program); statement.executeUpdate();
        }
        assertThrows(IllegalStateException.class, () -> catalog.createProgram(" java  INTERNSHIP ", department, null, start, end, 5));
        catalog.updateProgram(program, "Java Internship", department, "Edited", start, end, 5);
        catalog.activateProgram(program);
        int mentor1 = catalog.createMentor(4L, department, "Java", 1);
        int mentor2 = catalog.createMentor(5L, department, "Java", 2);
        catalog.createMentor(6L, otherDepartment, null, 2);
        assertThrows(IllegalStateException.class, () -> catalog.createMentor(4L, department, null, 1));
        assertEquals(2, catalog.getMentors(department).size());
        assertTrue(catalog.getEligibleMentorUsers().isEmpty());

        // No contract rows exist; the agreed enrollment flow must still work.
        var first = service.enrollInternWithMentor(2, program, mentor1, 1L);
        assertNotNull(first.assignmentId());
        var second = service.enrollInternWithMentor(3, program, mentor1, 1L);
        assertNull(second.assignmentId()); assertNotNull(second.mentorWarning());
        assertNotNull(service.getEnrollmentById(second.enrollmentId()));
        service.assignMentor(second.enrollmentId(), mentor2, 1L);
        assertThrows(IllegalStateException.class, () -> service.enrollIntern(3, program, 1L));
        assertThrows(IllegalStateException.class, () -> catalog.updateProgram(program, "P", department, null,
                start, Date.valueOf("2026-11-01"), 5));
        assertThrows(IllegalStateException.class, () -> catalog.updateProgram(program, "P", otherDepartment, null, start, end, 5));
        catalog.updateProgram(program, "Java Final", department, "Name-only edit", start, end, 5);

        // BE4 regression: all downstream records keep the original enrollment and their authors.
        try (Connection connection = DBConnection.getConnection(); Statement statement = connection.createStatement()) {
            statement.executeUpdate("INSERT INTO tasks(task_id,title,created_by_user_id) VALUES(1,'Existing task',4)");
            statement.executeUpdate("INSERT INTO task_assignments(task_id,enrollment_id) VALUES(1," + first.enrollmentId() + ")");
            statement.executeUpdate("INSERT INTO weekly_reports(enrollment_id,week_number,content) VALUES(" + first.enrollmentId() + ",1,'Existing report')");
            statement.executeUpdate("INSERT INTO evaluations(enrollment_id,mentor_id,score) VALUES(" + first.enrollmentId() + "," + mentor1 + ",90)");
        }
        service.changeMentor(first.enrollmentId(), mentor2, "Mentor transfer", 1L);
        assertEquals(2, service.getMentorHistory(first.enrollmentId()).size());
        assertEquals(mentor2, service.getCurrentMentor(first.enrollmentId()).getMentorId());
        MentorDAO mentors = new MentorDAO();
        assertEquals(0, mentors.getMentorById(mentor1).getCurrentMenteeCount());
        assertEquals(2, mentors.getMentorById(mentor2).getCurrentMenteeCount());
        assertTrue(mentors.getMentorById(mentor2).isAtCapacity());
        ProgramEnrollment overview = service.getActiveEnrollmentByIntern(2);
        assertEquals(start, overview.getStartDate()); assertEquals(end, overview.getEndDate());
        assertEquals("Engineering", overview.getDepartmentName()); assertEquals("Java Final", overview.getProgramName());
        assertEquals(mentor2, overview.getCurrentMentorId());
        for (String table : new String[]{"task_assignments", "weekly_reports", "evaluations"}) {
            assertEquals(1, scalar("SELECT COUNT(*) FROM " + table + " WHERE enrollment_id=" + first.enrollmentId()));
        }
        assertEquals(mentor1, scalar("SELECT mentor_id FROM evaluations WHERE enrollment_id=" + first.enrollmentId()));
        try (Connection connection = DBConnection.getConnection()) {
            new ProgramEnrollmentDAO().updateEnrollmentStatus(second.enrollmentId(), "COMPLETED", connection);
        }
        assertEquals(1, mentors.getMentorById(mentor2).getCurrentMenteeCount());

        CountDownLatch startTogether = new CountDownLatch(1);
        try (var pool = Executors.newFixedThreadPool(2)) {
            Callable<Boolean> create = () -> {
                startTogether.await();
                try { new CatalogService().createProgram(" Concurrent Program ", department, null, start, end, 0); return true; }
                catch (IllegalStateException e) { return false; }
            };
            Future<Boolean> a = pool.submit(create); Future<Boolean> b = pool.submit(create);
            startTogether.countDown();
            assertNotEquals(a.get(15, TimeUnit.SECONDS), b.get(15, TimeUnit.SECONDS));
        }
        assertEquals(1, scalar("SELECT COUNT(*) FROM internship_programs WHERE program_name='Concurrent Program'"));
    }

    private int scalar(String sql) throws SQLException {
        try (Connection connection = DBConnection.getConnection(); Statement statement = connection.createStatement();
             ResultSet results = statement.executeQuery(sql)) {
            assertTrue(results.next()); return results.getInt(1);
        }
    }

    @Test
    void be2DocumentAndContractStatesStayIndependentOfBe3Enrollment() throws Exception {
        long reviewerId = 3_000_000_010L;
        long internUserId = 3_000_000_011L;
        long otherInternUserId = 3_000_000_012L;
        try (Connection connection = DBConnection.getConnection(); Statement statement = connection.createStatement()) {
            for (long id : new long[]{reviewerId, internUserId, otherInternUserId}) {
                String role = id == reviewerId ? "HR" : "INTERN";
                statement.executeUpdate("INSERT INTO users(user_id,username,email,password_hash,full_name,role_id) "
                        + "SELECT " + id + ", 'integration_" + id + "', 'integration" + id
                        + "@example.test', 'not-a-login-hash', 'BE2 BE3 Integration', role_id FROM roles WHERE role_name='" + role + "'");
            }
            for (int profileId : new int[]{211, 212}) {
                long userId = profileId == 211 ? internUserId : otherInternUserId;
                statement.executeUpdate("INSERT INTO intern_profiles(profile_id,user_id,full_name,phone,email,university,major) "
                        + "VALUES(" + profileId + "," + userId + ",'Integration Intern','0000000000','integration"
                        + profileId + "@example.test','ICTU','IT')");
            }
            statement.executeUpdate("INSERT INTO applications(application_id,intern_id,status) VALUES(211,211,'PENDING')");
        }
        UserDAO users = new UserDAO();
        User reviewer = users.findById(reviewerId);
        User owner = users.findById(internUserId);
        User otherIntern = users.findById(otherInternUserId);
        Document document = new Document();
        document.setProfileId(211); document.setDocumentType("CV");
        document.setFileName("integration.pdf"); document.setFilePath("test-only/integration.pdf");
        document.setFileSize(100); document.setMimeType("application/pdf"); document.setStatus("PENDING");
        int documentId = new DocumentDAO().addDocument(document);
        DocumentService documents = new DocumentService();
        assertTrue(documents.approveDocument(documentId, reviewer));
        assertEquals(reviewerId, documents.getDocumentById(documentId).getReviewedBy());
        assertNotNull(documents.getDocumentById(documentId).getReviewedAt());
        ApplicationDAO applications = new ApplicationDAO();
        assertEquals("PENDING", applications.getApplicationById(211).getStatus(),
                "Reviewing a document must not approve an application");

        CatalogService catalog = new CatalogService();
        int department = catalog.createDepartment("BE2 BE3 Integration", null);
        int program = catalog.createProgram("Contract compatibility", department, null,
                Date.valueOf("2026-10-05"), Date.valueOf("2026-12-31"), 2);
        catalog.activateProgram(program);
        EnrollmentService enrollments = new EnrollmentService();
        assertThrows(IllegalStateException.class, () -> enrollments.enrollIntern(211, program, reviewerId));
        new ApplicationService((recipient, subject, content) -> { }).approveApplication(211, reviewerId);
        assertEquals(reviewerId, applications.getApplicationById(211).getReviewedBy());
        assertNull(enrollments.getActiveEnrollmentByIntern(211), "Approval must not auto-enroll an intern");

        Contract contract = new Contract(); contract.setApplicationId(211);
        contract.setFileName("contract.pdf"); contract.setFilePath("test-only/contract.pdf");
        contract.setFileSize(100); contract.setMimeType("application/pdf");
        contract.setUploadedBy(reviewerId); contract.setStatus("PENDING_CONFIRMATION");
        int contractId = new ContractDAO().saveOrUpdateContract(contract);
        int enrollmentId = enrollments.enrollIntern(211, program, reviewerId);
        assertNull(enrollments.getCurrentMentor(enrollmentId));
        assertEquals("PENDING_CONFIRMATION", documents.getContractById(contractId).getStatus(),
                "Enrollment does not require or confirm a contract");
        assertThrows(IllegalAccessException.class, () -> documents.confirmContract(contractId, otherIntern));
        assertTrue(documents.confirmContract(contractId, owner));
        assertTrue(documents.confirmContract(contractId, owner), "Confirmation remains idempotent");
        Contract confirmed = documents.getContractById(contractId);
        assertEquals("CONFIRMED", confirmed.getStatus()); assertNotNull(confirmed.getConfirmedAt());
        assertEquals(reviewerId, confirmed.getUploadedBy());
        assertEquals(enrollmentId, enrollments.getActiveEnrollmentByIntern(211).getEnrollmentId());
        assertEquals("APPROVED", applications.getApplicationById(211).getStatus());
    }
}
