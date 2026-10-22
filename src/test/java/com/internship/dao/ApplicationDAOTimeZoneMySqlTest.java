package com.internship.dao;

import com.internship.model.Application;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.junit.jupiter.api.parallel.ResourceLock;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.time.Instant;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.TimeZone;
import java.util.UUID;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

/** Opt-in actual JDBC regression; all fixture writes/locks are confined to a new disposable DB. */
@EnabledIfSystemProperty(named = "be3.mysql", matches = "true")
@ResourceLock("java.util.TimeZone.default")
class ApplicationDAOTimeZoneMySqlTest {
    private static final long REVIEWER = 3_000_000_010L;
    private static final int PROFILE = 211;
    private static final Map<Integer, Instant> SUBMITTED = Map.of(
            901, Instant.parse("2026-10-12T23:59:59Z"),
            902, Instant.parse("2026-10-12T02:00:00Z"),
            903, Instant.parse("2026-10-11T23:59:59Z"));
    private static final Map<Integer, Instant> REVIEWED = Map.of(
            902, Instant.parse("2026-10-12T03:30:00Z"),
            903, Instant.parse("2026-10-12T00:00:01Z"));
    private static String database, prefix, query, username, password;
    private static boolean created;

    @BeforeAll static void prepareDisposableDatabase() throws Exception {
        Properties properties = new Properties();
        try (var input = ApplicationDAOTimeZoneMySqlTest.class.getClassLoader()
                .getResourceAsStream("db.properties")) {
            assertNotNull(input, "Configure local db.properties before enabling be3.mysql tests");
            properties.load(new InputStreamReader(input, StandardCharsets.UTF_8));
        }
        Class.forName(properties.getProperty("db.driver"));
        var match = Pattern.compile("^(jdbc:mysql://[^/]+/)[^?]+(?:\\?(.*))?$")
                .matcher(properties.getProperty("db.url"));
        assertTrue(match.matches(), "Only a single-host MySQL URL is supported by this fixture");
        prefix = match.group(1);
        query = Arrays.stream((match.group(2) == null ? "" : match.group(2)).split("&"))
                .filter(value -> !value.isBlank() && !value.matches(
                        "(?i)(serverTimezone|connectionTimeZone|preserveInstants|forceConnectionTimeZoneToSession)=.*"))
                .collect(Collectors.joining("&"));
        username = properties.getProperty("db.username");
        password = properties.getProperty("db.password");
        database = "be3_app_time_test_" + UUID.randomUUID().toString().replace("-", "");
        try (Connection admin = DriverManager.getConnection(url("", "UTC"), username, password);
             var statement = admin.createStatement()) {
            statement.execute("CREATE DATABASE `" + database + "` CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci");
            created = true;
        }
        try (Connection connection = open("+00:00", "UTC"); var statement = connection.createStatement()) {
            for (String schema : List.of("database/schema.sql", "sql/schema_be2.sql")) {
                String script = Files.readString(Path.of(schema), StandardCharsets.UTF_8)
                        .replace("internship_management", database).replaceAll("(?m)^\\s*--.*$", "");
                for (String command : script.split(";")) {
                    if (!command.isBlank()) statement.execute(command);
                }
            }
            assertSession(connection, "+00:00");
            // Plain UTC literals are interpreted by the known UTC session, independently of JVM/JDBC zones.
            statement.executeUpdate("INSERT INTO users(user_id,username,email,password_hash,full_name,role_id) "
                    + "SELECT " + REVIEWER + ",'time_hr','time_hr@example.test','not-a-login-hash','Time HR',role_id "
                    + "FROM roles WHERE role_name='HR'");
            statement.executeUpdate("INSERT INTO users(user_id,username,email,password_hash,full_name,role_id) "
                    + "SELECT 3000000011,'time_intern','time_intern@example.test','not-a-login-hash','Time Intern',role_id "
                    + "FROM roles WHERE role_name='INTERN'");
            statement.executeUpdate("INSERT INTO intern_profiles(profile_id,user_id,full_name,phone,email,university,major) "
                    + "VALUES(" + PROFILE + ",3000000011,'Time Intern','0000000000','time_intern@example.test','Test','Test')");
            statement.executeUpdate("INSERT INTO applications(application_id,intern_id,status,submitted_at,reviewed_by,reviewed_at,reject_reason) VALUES "
                    + "(901," + PROFILE + ",'PENDING','2026-10-12 23:59:59',NULL,NULL,NULL),"
                    + "(902," + PROFILE + ",'APPROVED','2026-10-12 02:00:00'," + REVIEWER + ",'2026-10-12 03:30:00',NULL),"
                    + "(903," + PROFILE + ",'REJECTED','2026-10-11 23:59:59'," + REVIEWER + ",'2026-10-12 00:00:01','Test reason')");
            try (var columns = statement.executeQuery("SELECT COLUMN_NAME,DATA_TYPE FROM INFORMATION_SCHEMA.COLUMNS "
                    + "WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='applications' "
                    + "AND COLUMN_NAME IN ('submitted_at','reviewed_at')")) {
                int count = 0;
                while (columns.next()) { assertEquals("timestamp", columns.getString("DATA_TYPE")); count++; }
                assertEquals(2, count);
            }
        }
    }

    @AfterAll static void cleanOnlyOurDatabase() throws Exception {
        if (!created) return;
        assertTrue(database.matches("be3_app_time_test_[0-9a-f]{32}"), "Never drop an application database");
        try (Connection admin = DriverManager.getConnection(url("", "UTC"), username, password);
             var statement = admin.createStatement()) {
            statement.execute("DROP DATABASE `" + database + "`");
            created = false;
        }
    }

    static Stream<Arguments> timezoneCases() {
        return List.of("UTC", "GMT+07:00").stream().flatMap(jvm ->
                List.of("UTC", "Asia/Ho_Chi_Minh").stream().flatMap(jdbc ->
                        List.of("+00:00", "+07:00").stream().map(session -> Arguments.of(jvm, jdbc, session))));
    }

    @ParameterizedTest(name = "JVM={0}, JDBC={1}, MySQL session={2}")
    @MethodSource("timezoneCases")
    void allReadsKeepTheStoredInstantsNullsAndSession(String jvm, String jdbc, String session) throws Exception {
        TimeZone original = TimeZone.getDefault();
        try {
            TimeZone.setDefault(TimeZone.getTimeZone(jvm));
            ApplicationDAO dao = new ApplicationDAO(() -> open(session, jdbc));
            try (Connection connection = open(session, jdbc)) {
                Map<Integer, StoredRow> before = storage(connection);
                assertEquals(3, before.size());
                assertAllRows(dao.getAllApplications());
                assertAllRows(dao.getApplicationsByInternId(PROFILE));
                List<Application> pending = dao.getPendingApplications();
                assertEquals(1, pending.size());
                assertEquals(901, pending.getFirst().getApplicationId());
                assertApplication(pending.getFirst());

                connection.setAutoCommit(false);
                try {
                    for (int id : List.of(901, 902, 903)) {
                        assertApplication(dao.getApplicationById(id));
                        assertApplication(dao.getApplicationById(id, connection));
                        assertApplication(dao.getApplicationByIdForUpdate(id, connection));
                        assertFalse(connection.isClosed(), "DAO must not close a caller-owned transaction");
                        assertSession(connection, session);
                    }
                } finally {
                    connection.rollback();
                    connection.setAutoCommit(true);
                }
                assertSession(connection, session);
                assertEquals(before, storage(connection), "Reads must not rewrite existing timestamp values/status/reviewer");
            }
        } finally {
            TimeZone.setDefault(original);
        }
    }

    private static void assertAllRows(List<Application> applications) {
        assertEquals(3, applications.size());
        assertEquals(SUBMITTED.keySet(), applications.stream().map(Application::getApplicationId).collect(Collectors.toSet()));
        applications.forEach(ApplicationDAOTimeZoneMySqlTest::assertApplication);
    }

    private static void assertApplication(Application application) {
        assertNotNull(application);
        int id = application.getApplicationId();
        Instant submitted = SUBMITTED.get(id), reviewed = REVIEWED.get(id);
        assertNotNull(submitted);
        assertEquals(submitted, application.getSubmittedAt().toInstant());
        assertEquals(submitted.toString(), application.getSubmittedAtIso());
        if (reviewed == null) {
            assertNull(application.getReviewedAt());
            assertNull(application.getReviewedAtIso());
            assertNull(application.getReviewedBy());
            assertEquals("PENDING", application.getStatus());
        } else {
            assertEquals(reviewed, application.getReviewedAt().toInstant());
            assertEquals(reviewed.toString(), application.getReviewedAtIso());
            assertEquals(REVIEWER, application.getReviewedBy());
            assertEquals(id == 902 ? "APPROVED" : "REJECTED", application.getStatus());
        }
    }

    private static Map<Integer, StoredRow> storage(Connection connection) throws SQLException {
        Map<Integer, StoredRow> result = new HashMap<>();
        try (var statement = connection.createStatement(); var rows = statement.executeQuery(
                "SELECT application_id,CAST(submitted_at AS CHAR),CAST(reviewed_at AS CHAR),"
                        + "status,reviewed_by,reject_reason FROM applications ORDER BY application_id")) {
            while (rows.next()) {
                int id = rows.getInt(1);
                String submitted = rows.getString(2), reviewed = rows.getString(3), status = rows.getString(4);
                long reviewer = rows.getLong(5);
                Long nullableReviewer = rows.wasNull() ? null : reviewer;
                result.put(id, new StoredRow(submitted, reviewed, status, nullableReviewer, rows.getString(6)));
            }
        }
        return result;
    }
    private record StoredRow(String submitted, String reviewed, String status, Long reviewer, String reason) { }

    private static Connection open(String session, String jdbc) throws SQLException {
        Connection connection = DriverManager.getConnection(url(database, jdbc), username, password);
        try {
            assertTrue(List.of("+00:00", "+07:00").contains(session));
            try (var statement = connection.createStatement()) {
                statement.execute("SET SESSION time_zone = '" + session + "'");
            }
            assertSession(connection, session);
            return connection;
        } catch (SQLException | RuntimeException | AssertionError failure) {
            try { connection.close(); } catch (SQLException closeFailure) { failure.addSuppressed(closeFailure); }
            throw failure;
        }
    }

    private static void assertSession(Connection connection, String expected) throws SQLException {
        try (var statement = connection.createStatement(); var rows = statement.executeQuery("SELECT @@session.time_zone")) {
            assertTrue(rows.next());
            assertEquals(expected, rows.getString(1));
        }
    }

    private static String url(String databaseName, String jdbc) {
        return prefix + databaseName + "?" + query + (query.isEmpty() ? "" : "&")
                + "connectionTimeZone=" + jdbc + "&preserveInstants=true&forceConnectionTimeZoneToSession=false";
    }
}
