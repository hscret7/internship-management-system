package com.internship.service;

import com.internship.model.Meeting;
import com.internship.service.meeting.MeetingNotifications;
import org.junit.jupiter.api.Test;
import java.sql.*;
import java.time.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class MeetingInboxTransactionTest {
    @Test void inboxFailureAlsoRollsBackEmailHookInSameTransaction() {
        var h=new MeetingServiceTest.Harness();var email=new Sink();var inbox=new Sink();inbox.fail=true;
        var service=new MeetingService(()->h.connection,h.dao,Clock.fixed(NOW,ZoneOffset.UTC),email,inbox);
        assertThrows(SQLException.class,()->service.create(9,request()));
        assertEquals(1,email.records);assertEquals(1,inbox.records);assertTrue(h.rolledBack);assertFalse(h.committed);
    }
    private static final Instant NOW=Instant.parse("2026-10-07T14:00:00Z");
    private static MeetingRequest request(){return new MeetingRequest(1,"Test",null,"Room",NOW.plusSeconds(3600),NOW.plusSeconds(7200),List.of(2));}
    static class Sink implements MeetingNotifications {
        boolean fail; int records,captures; Meeting before,after;Map<Long,String> old;
        public Map<Long,String> beforeChange(Connection c,Meeting m){captures++;return Map.of(5L,"old@example.test");}
        public void record(Connection c,Meeting b,Map<Long,String> recipients,Meeting a)throws SQLException {
            records++;before=b;after=a;old=recipients;if(fail)throw new SQLException("Outbox failed");
        }
    }
    @Test void createRecordsBeforeCommitAndFailureRollsBack() {
        var h=new MeetingServiceTest.Harness();var sink=new Sink();sink.fail=true;
        var s=new MeetingService(()->h.connection,h.dao,Clock.fixed(NOW,ZoneOffset.UTC),MeetingNotifications.disabled(),sink);
        assertThrows(SQLException.class,()->s.create(9,request()));assertEquals(1,sink.records);assertNull(sink.before);
        assertTrue(h.rolledBack);assertFalse(h.committed);assertTrue(h.closed);
    }
    @Test void updateCapturesOldRecipientsAndRecordsInSameTransaction()throws Exception {
        var h=new MeetingServiceTest.Harness();var sink=new Sink();
        new MeetingService(()->h.connection,h.dao,Clock.fixed(NOW,ZoneOffset.UTC),MeetingNotifications.disabled(),sink).update(9,31,1,request());
        assertEquals(1,sink.captures);assertEquals(Map.of(5L,"old@example.test"),sink.old);assertTrue(h.committed);assertEquals(1,sink.records);
    }
    @Test void staleOrUnauthorizedWriteCannotEnqueue() {
        var h=new MeetingServiceTest.Harness();var sink=new Sink();
        var s=new MeetingService(()->h.connection,h.dao,Clock.fixed(NOW,ZoneOffset.UTC),MeetingNotifications.disabled(),sink);
        assertThrows(IllegalStateException.class,()->s.update(9,31,2,request()));assertEquals(0,sink.records);
        h.role="MENTOR";h.organizer=3;
        assertThrows(SecurityException.class,()->s.cancel(9,31,1));assertEquals(0,sink.captures);
    }
    @Test void repeatedCancellationDoesNotCaptureOrEnqueue()throws Exception {
        var h=new MeetingServiceTest.Harness();h.meetingStatus="CANCELLED";var sink=new Sink();
        new MeetingService(()->h.connection,h.dao,Clock.fixed(NOW,ZoneOffset.UTC),MeetingNotifications.disabled(),sink).cancel(9,31,1);
        assertEquals(0,sink.records);assertEquals(0,sink.captures);assertTrue(h.committed);
    }
}
