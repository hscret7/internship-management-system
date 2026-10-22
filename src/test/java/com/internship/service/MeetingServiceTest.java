package com.internship.service;

import com.internship.dao.MeetingDAO;
import com.internship.model.Meeting;
import org.junit.jupiter.api.Test;
import java.lang.reflect.Proxy;
import java.sql.*;
import java.time.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class MeetingServiceTest {
    private static final Instant NOW = Instant.parse("2026-10-07T14:00:00Z");
    private static MeetingRequest request(List<Integer> ids) {
        return new MeetingRequest(1," Weekly meeting ",null,"Room 1",NOW.plusSeconds(3600),NOW.plusSeconds(7200),ids);
    }
    @Test void rejectsDuplicateParticipantsBeforeOpeningDatabase() {
        assertThrows(IllegalArgumentException.class, () -> request(List.of(1,1)));
    }
    @Test void requestDefensivelyCopiesAndSortsParticipants() {
        List<Integer> source = new ArrayList<>(List.of(2,1));
        MeetingRequest q = request(source); source.clear();
        assertEquals(List.of(1,2), q.enrollmentIds()); assertEquals("Weekly meeting",q.title());
        assertThrows(UnsupportedOperationException.class, () -> q.enrollmentIds().add(3));
    }
    @Test void rejectsReversedOrOutOfRangeTimes() {
        assertThrows(IllegalArgumentException.class, () -> new MeetingRequest(1,"a",null,"b",NOW,NOW,List.of(1)));
        assertThrows(IllegalArgumentException.class, () -> new MeetingRequest(1,"a",null,"b",NOW,Instant.parse("2040-01-01T00:00:00Z"),List.of(1)));
    }
    @Test void createCommitsParticipantsAndMeetingTogether() throws Exception {
        Harness h = new Harness(); assertEquals(31,h.service().create(9,request(List.of(1))));
        assertTrue(h.committed); assertFalse(h.rolledBack); assertTrue(h.closed); assertTrue(h.wrote);
    }
    @Test void internCannotSchedule() {
        Harness h = new Harness(); h.role="INTERN";
        assertThrows(SecurityException.class, () -> h.service().create(9,request(List.of(1))));
        assertTrue(h.rolledBack); assertFalse(h.wrote);
    }
    @Test void mentorRequiresCurrentAssignmentForEveryParticipant() {
        Harness h = new Harness(); h.role="MENTOR"; h.allowed=false;
        assertThrows(SecurityException.class, () -> h.service().create(9,request(List.of(1))));
        assertEquals(9L,h.checkedMentor); assertFalse(h.wrote); assertTrue(h.rolledBack);
    }
    @Test void sqlFailureRollsBackAndCloses() {
        Harness h = new Harness(); h.failWrite=true;
        assertThrows(SQLException.class, () -> h.service().create(9,request(List.of(1))));
        assertTrue(h.rolledBack); assertFalse(h.committed); assertTrue(h.closed);
    }
    @Test void staleRevisionNeverWrites() {
        Harness h = new Harness();
        assertThrows(IllegalStateException.class, () -> h.service().update(9,31,2,request(List.of(1))));
        assertFalse(h.wrote); assertTrue(h.rolledBack);
    }
    @Test void formerMentorCannotCancelEvenIfOriginalOrganizer() {
        Harness h = new Harness(); h.role="MENTOR"; h.allowed=false;
        assertThrows(SecurityException.class, () -> h.service().cancel(9,31,1));
        assertFalse(h.wrote); assertTrue(h.rolledBack);
    }
    @Test void hrCanCancelAfterMentorChanges() throws Exception {
        Harness h = new Harness(); h.service().cancel(9,31,1);
        assertTrue(h.wrote); assertTrue(h.committed);
    }
    @Test void updateChecksOldAndNewParticipants() throws Exception {
        Harness h = new Harness(); h.role="MENTOR";
        h.service().update(9,31,1,request(List.of(2)));
        assertEquals(List.of(1,2),h.checkedIds); assertTrue(h.committed);
    }
    @Test void pastMeetingRejectedWithoutConnection() {
        var s = new MeetingService(() -> { fail("DB opened"); return null; },new MeetingDAO(),Clock.fixed(NOW.plusSeconds(4000),ZoneOffset.UTC));
        assertThrows(IllegalArgumentException.class, () -> s.create(9,request(List.of(1))));
    }
    @Test void internDetailsHideOtherParticipantIds() throws Exception {
        Harness h=new Harness();h.role="INTERN";
        assertEquals(List.of(),h.service().get(9,31).enrollmentIds());
        assertTrue(h.committed);
    }
    @Test void inaccessibleDetailsAreUnavailable() {
        Harness h=new Harness();h.allowed=false;
        assertThrows(MeetingService.NotFound.class,()->h.service().get(9,31));
        assertTrue(h.rolledBack);
    }
    @Test void listRechecksVisibilityBeforeReturning() throws Exception {
        Harness h=new Harness();h.allowed=false;
        assertTrue(h.service().list(9,null,null,1,20).items().isEmpty());
    }
    @Test void invalidPaginationDoesNotOpenConnection() {
        Harness h=new Harness();
        assertThrows(IllegalArgumentException.class,()->h.service().list(9,null,null,0,20));
        assertFalse(h.closed);
    }

    @Test void nonOrganizerDeniedBeforeStaleRevisionOnCancel() {
        Harness h=new Harness();h.role="MENTOR";h.organizer=3;
        assertThrows(SecurityException.class,()->h.service().cancel(9,31,2));
        assertFalse(h.wrote);assertTrue(h.rolledBack);
    }
    @Test void nonOrganizerDeniedBeforeCancelledStateOnUpdate() {
        Harness h=new Harness();h.role="MENTOR";h.organizer=3;h.meetingStatus="CANCELLED";
        assertThrows(SecurityException.class,()->h.service().update(9,31,1,request(List.of(1))));
        assertFalse(h.wrote);
    }
    @Test void formerOrganizerDeniedBeforeStaleRevision() {
        Harness h=new Harness();h.role="MENTOR";h.allowed=false;
        assertThrows(SecurityException.class,()->h.service().cancel(9,31,2));
        assertFalse(h.wrote);
    }
    @Test void authorizedCancelledUpdateStillConflicts() {
        Harness h=new Harness();h.meetingStatus="CANCELLED";
        assertThrows(IllegalStateException.class,()->h.service().update(9,31,1,request(List.of(1))));
        assertFalse(h.wrote);
    }
    @Test void currentRevisionRepeatedCancellationIsNoOp() throws Exception {
        Harness h=new Harness();h.meetingStatus="CANCELLED";
        h.service().cancel(9,31,1);assertFalse(h.wrote);assertTrue(h.committed);
    }
    @Test void businessWritesRecordMatchingAuditBeforeCommit() throws Exception {
        Harness create=new Harness();create.service().create(9,request(List.of(1)));
        assertEquals(List.of("MEETING_CREATE"),create.audits);assertTrue(create.committed);
        Harness update=new Harness();update.service().update(9,31,1,request(List.of(1)));
        assertEquals(List.of("MEETING_UPDATE"),update.audits);assertTrue(update.committed);
        Harness cancel=new Harness();cancel.service().cancel(9,31,1);
        assertEquals(List.of("MEETING_CANCEL"),cancel.audits);assertTrue(cancel.committed);
    }
    @Test void auditFailureRollsBackEveryMeetingWrite() {
        for(String action:List.of("create","update","cancel")) {
            Harness h=new Harness();h.failAudit=true;
            assertThrows(SQLException.class,()->{
                switch(action) {
                    case "create"->h.service().create(9,request(List.of(1)));
                    case "update"->h.service().update(9,31,1,request(List.of(1)));
                    default->h.service().cancel(9,31,1);
                }
            });assertTrue(h.rolledBack);assertFalse(h.committed);
        }
    }
    @Test void deniedStaleAndRepeatedCancelDoNotAudit() throws Exception {
        Harness denied=new Harness();denied.role="INTERN";
        assertThrows(SecurityException.class,()->denied.service().create(9,request(List.of(1))));assertTrue(denied.audits.isEmpty());
        Harness stale=new Harness();assertThrows(IllegalStateException.class,()->stale.service().cancel(9,31,99));assertTrue(stale.audits.isEmpty());
        Harness repeated=new Harness();repeated.meetingStatus="CANCELLED";repeated.service().cancel(9,31,1);assertTrue(repeated.audits.isEmpty());
    }
    static class Harness {
        long organizer=9; String meetingStatus="SCHEDULED";
        String role="HR"; boolean failAudit; List<String> audits=new ArrayList<>();
        boolean allowed=true,failWrite,wrote,committed,rolledBack,closed;
        Long checkedMentor; List<Integer> checkedIds=new ArrayList<>();
        Connection connection = (Connection) Proxy.newProxyInstance(Connection.class.getClassLoader(),new Class[]{Connection.class},(p,m,a)->switch(m.getName()) {
            case "createStatement" -> Proxy.newProxyInstance(Statement.class.getClassLoader(),new Class[]{Statement.class},(sp,sm,sa)-> {
                if(sm.getName().equals("execute")) { assertEquals("SET SESSION time_zone = '+00:00'",sa[0]); return false; } return null;
            });
            case "commit" -> {committed=true; yield null;}
            case "rollback" -> {rolledBack=true; yield null;}
            case "close" -> {closed=true; yield null;}
            default -> null;
        });
        MeetingDAO dao = new MeetingDAO() {
            @Override public void recordAudit(Connection c,long actor,String action,long id) throws SQLException {
                assertSame(connection,c);assertEquals(9,actor);assertEquals(31,id);audits.add(action);
                if(failAudit)throw new SQLException("audit");
            }
            @Override public List<Long> visibleIds(Connection c,long actor,String role,Integer program,String status,int page,int size) {return List.of(31L);}
            @Override public boolean canRead(Connection c,long actor,String role,Meeting m) {return allowed;}
            @Override public String lockActor(Connection c,long actor) {return role;}
            @Override public boolean lockProgram(Connection c,int program) {return true;}
            @Override public boolean lockParticipant(Connection c,int program,int enrollment,Long mentor) {
                checkedMentor=mentor;checkedIds.add(enrollment);return allowed;
            }
            @Override public long create(Connection c,long actor,MeetingRequest q) throws SQLException {
                if(failWrite)throw new SQLException("test");wrote=true;return 31;
            }
            @Override public Meeting lockMeeting(Connection c,long id) {
                return new Meeting(id,1,organizer,"Meeting",null,"Room",NOW.plusSeconds(3600),NOW.plusSeconds(7200),meetingStatus,1,List.of(1));
            }
            @Override public void update(Connection c,long id,MeetingRequest q) {wrote=true;}
            @Override public void cancel(Connection c,long id) {wrote=true;}
        };
        MeetingService service() {return new MeetingService(()->connection,dao,Clock.fixed(NOW,ZoneOffset.UTC));}
    }
}
