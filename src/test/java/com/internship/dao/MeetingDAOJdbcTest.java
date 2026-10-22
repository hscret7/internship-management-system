package com.internship.dao;

import com.internship.service.MeetingRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import java.io.InputStream;
import java.nio.file.*;
import java.sql.*;
import java.time.Instant;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

/** Explicit local-only opt-in. Writes only newly created meetings, always rolls back. */
@EnabledIfSystemProperty(named="be1.meeting.jdbc", matches="true")
class MeetingDAOJdbcTest {
    @Test void localCreateUpdateCancelRoundTripAndRollback() throws Exception {
        Properties config = new Properties();
        try (InputStream in = Files.newInputStream(Path.of("src/main/resources/db.properties"))) {
            config.load(in);
        }
        String url = config.getProperty("db.url", "");
        // Restrict to the previously verified WSL Docker database. Do not accept a shared host.
        assertTrue(url.startsWith("jdbc:mysql://127.0.0.1:3308/internship_management?")
                || url.equals("jdbc:mysql://127.0.0.1:3308/internship_management"),
                "This test only supports the verified local Docker database");
        String username = config.getProperty("db.username");
        String password = config.getProperty("db.password");
        MeetingDAO dao = new MeetingDAO();
        long meetingId = 0;
        Instant start = Instant.now().plusSeconds(3600).truncatedTo(java.time.temporal.ChronoUnit.MICROS);
        MeetingRequest first = new MeetingRequest(1,"BE1 JDBC rollback test",null,"Test room",start,start.plusSeconds(1800),List.of(1));
        try (Connection c = DriverManager.getConnection(url,username,password)) {
            c.setAutoCommit(false);
            try {
                try (Statement s = c.createStatement()) { s.execute("SET SESSION time_zone = '+00:00'"); }
                assertEquals("HR",dao.lockActor(c,3),"Expected ACTIVE local HR user ID3");
                assertTrue(dao.lockProgram(c,1),"Expected PLANNED/ACTIVE program ID1");
                assertTrue(dao.lockParticipant(c,1,1,null),"Expected active Intern enrollment ID1");
                meetingId = dao.create(c,3,first);
                var stored = dao.lockMeeting(c,meetingId);
                assertNotNull(stored);
                assertEquals(start,stored.startsAt());
                assertEquals(start.plusSeconds(1800),stored.endsAt());
                assertEquals(List.of(1),stored.enrollmentIds());
                assertEquals(3,stored.organizerUserId());
                assertEquals(1,stored.revision());
                assertTrue(dao.visibleIds(c,3,"HR",1,null,1,100).contains(meetingId));
                assertTrue(dao.visibleIds(c,5,"INTERN",1,null,1,100).contains(meetingId));
                assertTrue(dao.canRead(c,5,"INTERN",stored));
                assertFalse(dao.canRead(c,999999,"INTERN",stored));
                var changed = new MeetingRequest(1,"BE1 JDBC updated",null,"New room",start.plusSeconds(60),start.plusSeconds(1860),List.of(1));
                dao.update(c,meetingId,changed);
                stored = dao.lockMeeting(c,meetingId);
                assertEquals(2,stored.revision());
                assertEquals("BE1 JDBC updated",stored.title());
                assertEquals(changed.startsAt(),stored.startsAt());
                assertEquals(List.of(1),stored.enrollmentIds());
                dao.cancel(c,meetingId);
                stored = dao.lockMeeting(c,meetingId);
                assertEquals("CANCELLED",stored.status());
                assertEquals(3,stored.revision());
                assertEquals(List.of(1),stored.enrollmentIds());
            } finally {
                c.rollback();
            }
        }
        assertTrue(meetingId > 0);
        // Verify only the test's ID: unrelated concurrent meetings must not affect this assertion.
        try (Connection c = DriverManager.getConnection(url,username,password)) {
            for (String table : List.of("meetings","meeting_participants")) {
                try (PreparedStatement s = c.prepareStatement("SELECT COUNT(*) FROM " + table + " WHERE meeting_id=?")) {
                    s.setLong(1,meetingId);
                    try (ResultSet r = s.executeQuery()) { assertTrue(r.next()); assertEquals(0,r.getLong(1),"Rollback failed for " + table); }
                }
            }
        }
    }
}
