package com.internship.service.meeting;

import com.internship.dao.*;
import com.internship.service.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import java.nio.file.*;
import java.sql.*;
import java.time.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

/** Own disposable database only; never accepts the application's internship_management DB. */
@EnabledIfSystemProperty(named="be1.notifications.jdbc",matches="true")
class MeetingNotificationJdbcTest {
    private static final Instant NOW=Instant.parse("2026-10-08T12:00:00Z");
    private String url;
    private Connection open()throws SQLException{return DriverManager.getConnection(url,"root","");}
    private void sql(String text)throws SQLException {try(var c=open();var s=c.createStatement()){s.execute(text);}}
    private long count(String query)throws SQLException {try(var c=open();var s=c.createStatement();var r=s.executeQuery(query)){assertTrue(r.next());return r.getLong(1);}}
    private MeetingService service(MeetingNotifications notifications){return new MeetingService(this::open,new MeetingDAO(),Clock.fixed(NOW,ZoneOffset.UTC),notifications);}
    private MeetingRequest request(List<Integer> ids){return new MeetingRequest(1,"Test meeting",null,"Room",NOW.plusSeconds(3600),NOW.plusSeconds(5400),ids);}
    private MeetingNotificationDAO outbox(){return new MeetingNotificationDAO(Clock.fixed(NOW,ZoneOffset.UTC),30);}
    private MeetingNotificationWorker worker(com.internship.service.email.EmailService sender,Instant now,int attempts){
        return new MeetingNotificationWorker(this::open,sender,Clock.fixed(now,ZoneOffset.UTC),new MeetingNotificationSettings(true,true,30,attempts,600,10));
    }
    @BeforeEach void fixture()throws Exception {
        url=System.getProperty("be1.notifications.jdbc.url","jdbc:mysql://127.0.0.1:13318/be1_notification_test?connectionTimeZone=UTC");
        assertEquals("jdbc:mysql://127.0.0.1:13318/be1_notification_test?connectionTimeZone=UTC",url,"Only the disposable fixture DB is accepted");
        try(var c=open();var s=c.createStatement()) {
            s.execute("SET FOREIGN_KEY_CHECKS=0");
            for(String t:List.of("audit_logs","meeting_notification_outbox","meeting_recipient_snapshots","meeting_participants","meetings","mentor_assignments","mentors","program_enrollments","intern_profiles","users","roles","internship_programs"))s.execute("DROP TABLE IF EXISTS "+t);
            s.execute("SET FOREIGN_KEY_CHECKS=1");
            s.execute("CREATE TABLE roles(role_id BIGINT PRIMARY KEY,role_name VARCHAR(20)) ENGINE=InnoDB");
            s.execute("CREATE TABLE users(user_id BIGINT PRIMARY KEY,email VARCHAR(255),role_id BIGINT,status VARCHAR(20)) ENGINE=InnoDB");
            s.execute("CREATE TABLE internship_programs(program_id INT PRIMARY KEY,status VARCHAR(20)) ENGINE=InnoDB");
            s.execute("CREATE TABLE intern_profiles(profile_id INT PRIMARY KEY,user_id BIGINT) ENGINE=InnoDB");
            s.execute("CREATE TABLE program_enrollments(enrollment_id INT PRIMARY KEY,program_id INT,intern_profile_id INT,status VARCHAR(20)) ENGINE=InnoDB");
            s.execute("CREATE TABLE mentors(mentor_id INT PRIMARY KEY,user_id BIGINT,status VARCHAR(20)) ENGINE=InnoDB");
            s.execute("CREATE TABLE mentor_assignments(assignment_id INT PRIMARY KEY,enrollment_id INT,mentor_id INT,ended_at DATETIME) ENGINE=InnoDB");
            for(String file:List.of("sql/schema_be1_audit.sql","sql/schema_be1_meetings.sql","sql/schema_be1_meeting_notifications.sql")) {
                String raw=Files.readString(Path.of(file)).replaceAll("(?m)^--.*$","");
                for(String command:raw.split(";"))if(!command.isBlank())s.execute(command);
            }
            s.execute("INSERT INTO roles VALUES(1,'HR'),(2,'INTERN'),(3,'MENTOR')");
            s.execute("INSERT INTO users VALUES(3,'hr@example.test',1,'ACTIVE'),(4,'mentor@example.test',3,'ACTIVE'),(5,'old@example.test',2,'ACTIVE'),(6,'new@example.test',2,'ACTIVE')");
            s.execute("INSERT INTO internship_programs VALUES(1,'ACTIVE')");
            s.execute("INSERT INTO intern_profiles VALUES(1,5),(2,6)");
            s.execute("INSERT INTO program_enrollments VALUES(1,1,1,'ACTIVE'),(2,1,2,'ACTIVE')");
            s.execute("INSERT INTO mentors VALUES(1,4,'ACTIVE')");
            s.execute("INSERT INTO mentor_assignments VALUES(1,1,1,NULL),(2,2,1,NULL)");
        }
    }
    @Test void auditFailureRollsBackEmailOutboxAndRecipients() throws Exception {
        sql("CREATE TRIGGER fail_audit BEFORE INSERT ON audit_logs FOR EACH ROW SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='fixture audit failure'");
        assertThrows(SQLException.class,()->service(new MeetingNotificationDAO(Clock.fixed(NOW,ZoneOffset.UTC),30)).create(3,request(List.of(1))));
        for(String table:List.of("meetings","meeting_participants","meeting_notification_outbox","meeting_recipient_snapshots","audit_logs"))
            assertEquals(0,count("SELECT COUNT(*) FROM "+table));
    }
    @Test void createCommitsAndChangeKeepsOldAddressThenCancelDedups()throws Exception {
        var s=service(outbox());long id=s.create(3,request(List.of(1)));
        assertEquals(2,count("SELECT COUNT(*) FROM meeting_notification_outbox"));
        sql("UPDATE users SET email='changed@example.test' WHERE user_id=5");
        s.update(3,id,1,request(List.of(2)));
        assertEquals(1,count("SELECT COUNT(*) FROM meeting_notification_outbox WHERE event_type='CHANGE' AND recipient_user_id=5 AND recipient_email='old@example.test' AND body LIKE '%không còn%'"));
        assertEquals(1,count("SELECT COUNT(*) FROM meeting_notification_outbox WHERE event_type='REMINDER' AND revision=1 AND state='SUPERSEDED'"));
        assertEquals(1,count("SELECT COUNT(*) FROM meeting_recipient_snapshots WHERE revision=2 AND recipient_user_id=6"));
        s.cancel(3,id,2);s.cancel(3,id,3);
        assertEquals(1,count("SELECT COUNT(*) FROM meeting_notification_outbox WHERE event_type='CANCEL'"));
        assertEquals(0,count("SELECT COUNT(*) FROM meeting_notification_outbox WHERE event_type='REMINDER' AND state='PENDING'"));
    }
    @Test void failingOutboxRollsBackMeetingAndParticipants()throws Exception {
        sql("DROP TABLE meeting_notification_outbox");
        assertThrows(SQLException.class,()->service(outbox()).create(3,request(List.of(1))));
        assertEquals(0,count("SELECT COUNT(*) FROM meetings"));assertEquals(0,count("SELECT COUNT(*) FROM meeting_participants"));assertEquals(0,count("SELECT COUNT(*) FROM meeting_recipient_snapshots"));
    }
    @Test void invalidEmailRollsBackWithoutSending()throws Exception {
        sql("UPDATE users SET email='a@example.test,b@example.test' WHERE user_id=5");
        assertThrows(IllegalArgumentException.class,()->service(outbox()).create(3,request(List.of(1))));assertEquals(0,count("SELECT COUNT(*) FROM meetings"));
    }
    @Test void claimIsExclusiveAndCompletionIsTokenFenced()throws Exception {
        service(outbox()).create(3,request(List.of(1)));var a=worker((x,y,z)->{},NOW,5);var b=worker((x,y,z)->{},NOW,5);
        var first=a.claim();assertNotNull(first);assertNull(b.claim());
        assertFalse(b.finish(new MeetingNotificationWorker.Delivery(first.id(),"wrong",first.email(),first.subject(),first.body(),first.attempt()),true,null,false));
        assertTrue(a.finish(first,true,null,false));assertEquals(1,count("SELECT COUNT(*) FROM meeting_notification_outbox WHERE state='SENT'"));
    }
    @Test void expirationRecoversAndOldOwnerCannotAcknowledge()throws Exception {
        service(outbox()).create(3,request(List.of(1)));var a=worker((x,y,z)->{},NOW,5);var first=a.claim();
        var b=worker((x,y,z)->{},NOW.plusSeconds(601),5);var recovered=b.claim();assertEquals(first.id(),recovered.id());assertEquals(2,recovered.attempt());assertNotEquals(first.token(),recovered.token());
        assertFalse(a.finish(first,true,null,false));assertTrue(b.finish(recovered,true,null,false));
    }
    @Test void retryIsBoundedAndMeetingSurvivesSenderFailure()throws Exception {
        service(outbox()).create(3,request(List.of(1)));
        var fail=worker((x,y,z)->{throw new RuntimeException("sensitive provider detail");},NOW,2);
        assertTrue(fail.runOnce());assertEquals(1,count("SELECT COUNT(*) FROM meeting_notification_outbox WHERE state='PENDING' AND attempts=1 AND error_code='EMAIL_FAILURE'"));
        assertFalse(fail.runOnce());assertTrue(worker((x,y,z)->{throw new RuntimeException();},NOW.plusSeconds(31),2).runOnce());
        assertEquals(1,count("SELECT COUNT(*) FROM meeting_notification_outbox WHERE state='FAILED' AND attempts=2"));assertEquals(1,count("SELECT COUNT(*) FROM meetings"));
    }
    @Test void sendsOutsideTransactionAndDeliversReminderOnlyBeforeStart()throws Exception {
        long id=service(outbox()).create(3,request(List.of(1)));
        var sent=new ArrayList<String>();
        var w=worker((to,subject,body)->{try{assertEquals(1,count("SELECT COUNT(*) FROM meeting_notification_outbox WHERE state='IN_FLIGHT'"));sql("UPDATE meetings SET title='Mail runs without scheduling lock' WHERE meeting_id="+id);}catch(SQLException e){throw new AssertionError(e);}sent.add(subject);},NOW,5);
        assertTrue(w.runOnce());assertFalse(w.runOnce());assertEquals(1,sent.size());
        assertTrue(worker((a,b,c)->sent.add(b),NOW.plusSeconds(1800),5).runOnce());assertEquals(2,sent.size());
    }
    @Test void expiredReminderIsSuppressedAndEventsMaintainOrder()throws Exception {
        var s=service(outbox());long id=s.create(3,request(List.of(1)));s.cancel(3,id,1);
        List<String> titles=new ArrayList<>();var w=worker((a,b,c)->titles.add(b),NOW.plusSeconds(4000),5);
        assertTrue(w.runOnce());assertTrue(w.runOnce());assertFalse(w.runOnce());assertTrue(titles.get(0).contains("Lời mời"));assertTrue(titles.get(1).contains("Hủy"));
        assertEquals(1,count("SELECT COUNT(*) FROM meeting_notification_outbox WHERE event_type='REMINDER' AND state='SUPERSEDED'"));
    }
    @Test void updateFailureRestoresPreviousParticipantsAndRevision()throws Exception {
        long id=service(outbox()).create(3,request(List.of(1)));
        sql("DROP TABLE meeting_notification_outbox");
        assertThrows(SQLException.class,()->service(outbox()).update(3,id,1,request(List.of(2))));
        assertEquals(1,count("SELECT revision FROM meetings WHERE meeting_id="+id));
        assertEquals(1,count("SELECT enrollment_id FROM meeting_participants WHERE meeting_id="+id));
        assertEquals(0,count("SELECT COUNT(*) FROM meeting_recipient_snapshots WHERE revision=2"));
    }
    @Test void insideLeadWindowReminderIsDueNowAndAfterStartIsSuppressed()throws Exception {
        var q=new MeetingRequest(1,"Soon",null,"Room",NOW.plusSeconds(60),NOW.plusSeconds(3600),List.of(1));
        service(outbox()).create(3,q);
        List<String> sent=new ArrayList<>();var w=worker((a,b,c)->sent.add(b),NOW,5);
        assertTrue(w.runOnce());assertTrue(w.runOnce());assertEquals(2,sent.size());
        fixture();service(outbox()).create(3,q);
        var late=worker((a,b,c)->{},NOW.plusSeconds(61),5);assertTrue(late.runOnce());assertFalse(late.runOnce());
        assertEquals(1,count("SELECT COUNT(*) FROM meeting_notification_outbox WHERE event_type='REMINDER' AND state='SUPERSEDED'"));
    }
    @Test void twoWorkersCannotClaimSameEventConcurrently()throws Exception {
        service(outbox()).create(3,request(List.of(1)));
        var pool=java.util.concurrent.Executors.newFixedThreadPool(2);
        var gate=new java.util.concurrent.CountDownLatch(1);
        try {
            var a=pool.submit(()->{gate.await();return worker((x,y,z)->{},NOW,5).claim();});
            var b=pool.submit(()->{gate.await();return worker((x,y,z)->{},NOW,5).claim();});gate.countDown();
            var first=a.get(10,java.util.concurrent.TimeUnit.SECONDS);var second=b.get(10,java.util.concurrent.TimeUnit.SECONDS);
            // SKIP LOCKED can return no row while the other transaction holds
            // cleanup/join locks. A single poll is not an eventual-progress guarantee.
            assertFalse(first!=null && second!=null,
                "Only one event is due; two successful claims are forbidden: first="+first+", second="+second);
            var owner=worker((x,y,z)->{},NOW,5);
            var claimed=first!=null?first:second;
            if(claimed==null)claimed=owner.claim(); // Both concurrent transactions have now closed.
            assertNotNull(claimed,"The due event must be claimable after contention ends");
            assertEquals(1,claimed.attempt(),"Contention must not consume delivery attempts");
            assertEquals(1,count("SELECT COUNT(*) FROM meeting_notification_outbox WHERE state='IN_FLIGHT' AND attempts=1"));
            assertNull(owner.claim(),"An active lease must exclude another claim");
            assertTrue(owner.finish(claimed,true,null,false),"The winning token must acknowledge its event");
            assertFalse(owner.finish(claimed,true,null,false),"Acknowledgment must be single-use");
            assertEquals(1,count("SELECT COUNT(*) FROM meeting_notification_outbox WHERE state='SENT'"));
        }finally{pool.shutdownNow();}
    }
    @Test void legacyMeetingCapturesRecipientsBeforeReplacement()throws Exception {
        long id=service(MeetingNotifications.disabled()).create(3,request(List.of(1)));service(outbox()).update(3,id,1,request(List.of(2)));
        assertEquals(1,count("SELECT COUNT(*) FROM meeting_notification_outbox WHERE event_type='CHANGE' AND recipient_user_id=5"));
    }
}
