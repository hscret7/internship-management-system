package com.internship.dao;

import com.internship.model.Application;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.ResourceLock;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.lang.reflect.Proxy;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.List;
import java.util.TimeZone;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

@ResourceLock("java.util.TimeZone.default")
class ApplicationDAOTimeZoneTest {
    private static final Instant SUBMITTED = Instant.parse("2026-10-12T02:00:00.123456Z");
    private static final Instant REVIEWED = Instant.parse("2026-10-12T03:30:00.654321Z");

    static Stream<Arguments> readCases() {
        return List.of("UTC", "Asia/Ho_Chi_Minh", "America/New_York").stream()
                .flatMap(zone -> Stream.of(ReadPath.values()).flatMap(path ->
                        Stream.of(false, true).map(pending -> Arguments.of(zone, path, pending))));
    }

    @ParameterizedTest(name = "{0}, {1}, pending={2}")
    @MethodSource("readCases")
    void everyReadPathPreservesInstantsPrecisionAndNullReview(String zone, ReadPath path, boolean pending)
            throws Exception {
        TimeZone original = TimeZone.getDefault();
        try {
            TimeZone.setDefault(TimeZone.getTimeZone(zone));
            FakeJdbc jdbc = new FakeJdbc();
            jdbc.pending = pending;
            Application application = path.read(new ApplicationDAO(() -> jdbc.connection), jdbc.connection);

            assertNotNull(application);
            assertEquals(SUBMITTED, application.getSubmittedAt().toInstant());
            assertEquals(SUBMITTED.toString(), application.getSubmittedAtIso());
            if (pending) {
                assertNull(application.getReviewedAt());
                assertNull(application.getReviewedAtIso());
                assertNull(application.getReviewedBy());
            } else {
                assertEquals(REVIEWED, application.getReviewedAt().toInstant());
                assertEquals(REVIEWED.toString(), application.getReviewedAtIso());
                assertEquals(3_000_000_010L, application.getReviewedBy());
            }
            assertEquals(7, application.getApplicationId());
            assertEquals(19, application.getInternId());
            assertEquals(path.ownsConnection, jdbc.closed);
        } finally {
            TimeZone.setDefault(original);
        }
    }

    @Test void noRowRetainsExistingNullAndEmptyListContract() throws Exception {
        for (ReadPath path : ReadPath.values()) {
            FakeJdbc jdbc = new FakeJdbc();
            jdbc.rowExists = false;
            assertNull(path.read(new ApplicationDAO(() -> jdbc.connection), jdbc.connection), path.name());
            assertEquals(path.ownsConnection, jdbc.closed);
        }
    }

    @Test void nullSubmissionRemainsNullRatherThanInventingAnEpoch() throws Exception {
        FakeJdbc jdbc = new FakeJdbc();
        jdbc.submittedEpoch = null;
        Application application = new ApplicationDAO(() -> jdbc.connection).getApplicationById(7);
        assertNull(application.getSubmittedAt());
        assertNull(application.getSubmittedAtIso());
    }

    @Test void invalidPrecisionFailsInsteadOfSilentlyRoundingTheInstant() {
        FakeJdbc jdbc = new FakeJdbc();
        jdbc.submittedEpoch = new BigDecimal("1.0000000001");
        assertThrows(SQLException.class, () -> new ApplicationDAO(() -> jdbc.connection).getApplicationById(7));
        assertTrue(jdbc.closed);
    }

    @Test void sqlFailurePropagatesAndClosesOnlyOwnedConnection() {
        FakeJdbc jdbc = new FakeJdbc();
        jdbc.queryFailure = true;
        assertThrows(SQLException.class, () -> new ApplicationDAO(() -> jdbc.connection).getAllApplications());
        assertTrue(jdbc.closed);
        jdbc.closed = false;
        assertThrows(SQLException.class, () -> new ApplicationDAO(() -> jdbc.connection)
                .getApplicationByIdForUpdate(7, jdbc.connection));
        assertFalse(jdbc.closed);
    }

    enum ReadPath {
        BY_ID(true), BY_ID_TRANSACTION(false), FOR_UPDATE(false), PENDING(true), ALL(true), BY_INTERN(true);
        final boolean ownsConnection;
        ReadPath(boolean ownsConnection) { this.ownsConnection = ownsConnection; }

        Application read(ApplicationDAO dao, Connection connection) throws SQLException {
            return switch (this) {
                case BY_ID -> dao.getApplicationById(7);
                case BY_ID_TRANSACTION -> dao.getApplicationById(7, connection);
                case FOR_UPDATE -> dao.getApplicationByIdForUpdate(7, connection);
                case PENDING -> first(dao.getPendingApplications());
                case ALL -> first(dao.getAllApplications());
                case BY_INTERN -> first(dao.getApplicationsByInternId(19));
            };
        }
        private static Application first(List<Application> applications) {
            return applications.isEmpty() ? null : applications.getFirst();
        }
    }

    private static class FakeJdbc {
        boolean pending, closed, queryFailure;
        boolean rowExists = true;
        BigDecimal submittedEpoch = epoch(SUBMITTED);
        final Connection connection = proxy(Connection.class, (name, args) -> switch (name) {
            case "prepareStatement" -> {
                String sql = (String) args[0];
                assertTrue(sql.contains("UNIX_TIMESTAMP(a.submitted_at) AS submitted_at_epoch"));
                assertTrue(sql.contains("UNIX_TIMESTAMP(a.reviewed_at) AS reviewed_at_epoch"));
                yield proxy(PreparedStatement.class, (method, values) -> switch (method) {
                    case "executeQuery" -> {
                        if (queryFailure) throw new SQLException("Test query failure");
                        yield result();
                    }
                    case "setInt", "close" -> null;
                    default -> throw new AssertionError("Unexpected statement operation: " + method);
                });
            }
            case "close" -> { closed = true; yield null; }
            default -> throw new AssertionError("Reads must not alter the connection/session: " + name);
        });

        ResultSet result() {
            int[] row = {0};
            boolean[] lastNull = {false};
            return proxy(ResultSet.class, (method, args) -> switch (method) {
                case "next" -> rowExists && ++row[0] == 1;
                case "getBigDecimal" -> switch ((String) args[0]) {
                    case "submitted_at_epoch" -> submittedEpoch;
                    case "reviewed_at_epoch" -> pending ? null : epoch(REVIEWED);
                    default -> throw new AssertionError("Use the projected instant, not a local datetime");
                };
                case "getInt" -> switch ((String) args[0]) {
                    case "application_id" -> 7;
                    case "intern_id" -> 19;
                    default -> throw new AssertionError("Unexpected integer column");
                };
                case "getLong" -> { assertEquals("reviewed_by", args[0]); lastNull[0] = pending; yield pending ? 0L : 3_000_000_010L; }
                case "wasNull" -> lastNull[0];
                case "getString" -> switch ((String) args[0]) {
                    case "status" -> pending ? "PENDING" : "APPROVED";
                    case "reject_reason" -> null;
                    case "intern_name", "reviewer_name" -> "Test User";
                    case "intern_email" -> "test@example.test";
                    default -> throw new AssertionError("Unexpected text column");
                };
                case "close" -> null;
                default -> throw new AssertionError("Do not read timestamps as local JDBC/SQL strings: " + method);
            });
        }
    }

    private static BigDecimal epoch(Instant instant) {
        return BigDecimal.valueOf(instant.getEpochSecond()).add(BigDecimal.valueOf(instant.getNano(), 9));
    }
    interface Call { Object invoke(String method, Object[] args) throws Throwable; }
    @SuppressWarnings("unchecked") private static <T> T proxy(Class<T> type, Call call) {
        return (T) Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type},
                (proxy, method, args) -> call.invoke(method.getName(), args));
    }
}
