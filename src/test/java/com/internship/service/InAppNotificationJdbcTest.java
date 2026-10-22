package com.internship.service;
import com.internship.service.meeting.MeetingNotifications;

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
@EnabledIfSystemProperty(named="be1.inapp.jdbc",matches="true")
class InAppNotificationJdbcTest {
    private static final Instant NOW=Instant.now();
    private String url;
    private Connection open()throws SQLException{return DriverManager.getConnection(url,"root","");}
    private void sql(String text)throws SQLException {try(var c=open();var s=c.createStatement()){s.execute(text);}}
    private long count(String query)throws SQLException {try(var c=open();var s=c.createStatement();var r=s.executeQuery(query)){assertTrue(r.next());return r.getLong(1);}}
    private MeetingService service(MeetingNotifications notifications){return new MeetingService(this::open,new MeetingDAO(),Clock.fixed(NOW,ZoneOffset.UTC),MeetingNotifications.disabled(),notifications);}
    private MeetingRequest request(List<Integer> ids){return new MeetingRequest(1,"Test meeting",null,"Room",NOW.plusSeconds(3600),NOW.plusSeconds(5400),ids);}
    @BeforeEach void fixture()throws Exception {
        url=System.getProperty("be1.inapp.jdbc.url","jdbc:mysql://127.0.0.1:13319/be1_inapp_test?connectionTimeZone=UTC");
        assertEquals("jdbc:mysql://127.0.0.1:13319/be1_inapp_test?connectionTimeZone=UTC",url,"Only the disposable fixture DB is accepted");
        try(var c=open();var s=c.createStatement()) {
            s.execute("SET FOREIGN_KEY_CHECKS=0");
            for(String t:List.of("audit_logs","inapp_notifications","meeting_participants","meetings","mentor_assignments","mentors","program_enrollments","intern_profiles","users","roles","internship_programs"))s.execute("DROP TABLE IF EXISTS "+t);
            s.execute("SET FOREIGN_KEY_CHECKS=1");
            s.execute("CREATE TABLE roles(role_id BIGINT PRIMARY KEY,role_name VARCHAR(20)) ENGINE=InnoDB");
            s.execute("CREATE TABLE users(user_id BIGINT PRIMARY KEY,email VARCHAR(255),role_id BIGINT,status VARCHAR(20)) ENGINE=InnoDB");
            s.execute("CREATE TABLE internship_programs(program_id INT PRIMARY KEY,status VARCHAR(20)) ENGINE=InnoDB");
            s.execute("CREATE TABLE intern_profiles(profile_id INT PRIMARY KEY,user_id BIGINT) ENGINE=InnoDB");
            s.execute("CREATE TABLE program_enrollments(enrollment_id INT PRIMARY KEY,program_id INT,intern_profile_id INT,status VARCHAR(20)) ENGINE=InnoDB");
            s.execute("CREATE TABLE mentors(mentor_id INT PRIMARY KEY,user_id BIGINT,status VARCHAR(20)) ENGINE=InnoDB");
            s.execute("CREATE TABLE mentor_assignments(assignment_id INT PRIMARY KEY,enrollment_id INT,mentor_id INT,ended_at DATETIME) ENGINE=InnoDB");
            for(String file:List.of("sql/schema_be1_audit.sql","sql/schema_be1_meetings.sql","sql/schema_be1_inapp_notifications.sql")) {
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
    private MeetingService meetings(){return service(new MeetingInboxDAO(new InAppNotificationDAO(),Clock.fixed(NOW,ZoneOffset.UTC)));}
    private InAppNotificationService inbox(){return new InAppNotificationService(this::open,new InAppNotificationDAO(),true);}
    private NotificationEvent event(){return new NotificationEvent("task:31:1","TASK","TASK",31,1,"Assigned","Task body",NOW.minusSeconds(1));}
    @Test void createAtomicAndReminderHiddenUntilDue()throws Exception {
        meetings().create(3,request(List.of(1)));
        assertEquals(2,count("SELECT COUNT(*) FROM inapp_notifications"));
        assertEquals(1,inbox().list(5,false,1,20).items().size());assertEquals(1,inbox().list(5,false,1,20).unreadCount());
        assertEquals(0,inbox().list(6,false,1,20).items().size());
    }
    @Test void meetingAuditMatchesCommittedWritesAndRepeatCancelDoesNotDuplicate() throws Exception {
        long id=meetings().create(3,request(List.of(1)));
        meetings().update(3,id,1,request(List.of(1)));meetings().cancel(3,id,2);meetings().cancel(3,id,3);
        assertEquals(3,count("SELECT COUNT(*) FROM audit_logs WHERE actor_user_id=3 AND object_type='MEETING' AND object_id="+id+" AND result='SUCCESS'"));
        for(String action:List.of("MEETING_CREATE","MEETING_UPDATE","MEETING_CANCEL"))
            assertEquals(1,count("SELECT COUNT(*) FROM audit_logs WHERE action='"+action+"' AND object_id="+id));
    }
    @Test void auditFailureRollsBackMeetingParticipantsAndBothNotificationRows() throws Exception {
        sql("CREATE TRIGGER fail_audit BEFORE INSERT ON audit_logs FOR EACH ROW SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='fixture audit failure'");
        assertThrows(SQLException.class,()->meetings().create(3,request(List.of(1))));
        for(String table:List.of("meetings","meeting_participants","inapp_notifications","audit_logs"))
            assertEquals(0,count("SELECT COUNT(*) FROM "+table));
    }
    @Test void auditFailureRollsBackUpdateAndCancelWithInboxState() throws Exception {
        long id=meetings().create(3,request(List.of(1)));
        long rows=count("SELECT COUNT(*) FROM inapp_notifications");
        sql("CREATE TRIGGER fail_audit BEFORE INSERT ON audit_logs FOR EACH ROW SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='fixture audit failure'");
        assertThrows(SQLException.class,()->meetings().update(3,id,1,request(List.of(2))));
        assertThrows(SQLException.class,()->meetings().cancel(3,id,1));
        assertEquals(1,count("SELECT revision FROM meetings WHERE meeting_id="+id));
        assertEquals(1,count("SELECT COUNT(*) FROM meetings WHERE meeting_id="+id+" AND status='SCHEDULED'"));
        assertEquals(1,count("SELECT enrollment_id FROM meeting_participants WHERE meeting_id="+id));
        assertEquals(rows,count("SELECT COUNT(*) FROM inapp_notifications"));
        assertEquals(0,count("SELECT COUNT(*) FROM inapp_notifications WHERE superseded_at IS NOT NULL"));
        assertEquals(1,count("SELECT COUNT(*) FROM audit_logs"));
    }
    @Test void localTimezoneConnectionStillStoresAndReadsUtc() throws Exception {
        TimeZone previous = TimeZone.getDefault();
        try {
            TimeZone.setDefault(TimeZone.getTimeZone("Asia/Ho_Chi_Minh"));
            String localUrl = url.replace("connectionTimeZone=UTC", "connectionTimeZone=LOCAL");
            var notifications = new InAppNotificationService(
                () -> DriverManager.getConnection(localUrl,"root",""), new InAppNotificationDAO(), true);
            Instant due = NOW.minusSeconds(10).truncatedTo(java.time.temporal.ChronoUnit.MICROS);
            Instant expiry = NOW.plusSeconds(3600).truncatedTo(java.time.temporal.ChronoUnit.MICROS);
            var dao = new InAppNotificationDAO();
            try (var c = DriverManager.getConnection(localUrl,"root","")) {
                c.setAutoCommit(false);
                dao.publish(c,5,new NotificationEvent("meeting:77:1:REMINDER","REMINDER","MEETING",77,1,"Reminder","Body",due));
                dao.expireReminders(c,77,1,expiry);
                c.commit();
            }
            var n = notifications.list(5,false,1,20).items().getFirst();
            assertEquals(due,n.availableAt());
            assertEquals(1,notifications.list(5,false,1,20).unreadCount());
            try (var c=open(); var s=c.createStatement(); var r=s.executeQuery("SELECT available_at,expires_at FROM inapp_notifications WHERE source_id=77")) {
                assertTrue(r.next());
                Calendar utc=Calendar.getInstance(TimeZone.getTimeZone("UTC"));
                assertEquals(due,r.getTimestamp(1,utc).toInstant());
                assertEquals(expiry,r.getTimestamp(2,utc).toInstant());
            }
            notifications.markRead(5,n.id());
            Instant read = notifications.list(5,false,1,20).items().getFirst().readAt();
            assertNotNull(read);
            assertTrue(Duration.between(Instant.now(),read).abs().toSeconds()<30);
            notifications.markRead(5,n.id());
            assertEquals(read,notifications.list(5,false,1,20).items().getFirst().readAt());
        } finally { TimeZone.setDefault(previous); }
    }
    @Test void inboxFailureRollsBackBusinessWrite()throws Exception {
        sql("DROP TABLE inapp_notifications");assertThrows(SQLException.class,()->meetings().create(3,request(List.of(1))));
        assertEquals(0,count("SELECT COUNT(*) FROM meetings"));assertEquals(0,count("SELECT COUNT(*) FROM meeting_participants"));assertEquals(0,count("SELECT COUNT(*) FROM audit_logs"));
    }
    @Test void changeNotifiesRemovedAndAddedAndCancelIsIdempotent()throws Exception {
        var svc=meetings();long id=svc.create(3,request(List.of(1)));svc.update(3,id,1,request(List.of(2)));
        assertEquals(2,count("SELECT COUNT(*) FROM inapp_notifications WHERE kind='CHANGE'"));
        assertEquals(1,count("SELECT COUNT(*) FROM inapp_notifications WHERE kind='REMINDER' AND superseded_at IS NOT NULL"));
        svc.cancel(3,id,2);svc.cancel(3,id,3);
        assertEquals(1,count("SELECT COUNT(*) FROM inapp_notifications WHERE kind='CANCEL'"));
        assertEquals(0,count("SELECT COUNT(*) FROM inapp_notifications WHERE kind='REMINDER' AND superseded_at IS NULL"));
    }
    @Test void onlyOwnerCanMarkVisibleNotificationAndReadIsIdempotent()throws Exception {
        meetings().create(3,request(List.of(1)));var n=inbox().list(5,false,1,20).items().getFirst();
        assertThrows(InAppNotificationService.NotFound.class,()->inbox().markRead(6,n.id()));
        inbox().markRead(5,n.id());var first=inbox().list(5,false,1,20).items().getFirst().readAt();
        inbox().markRead(5,n.id());assertEquals(first,inbox().list(5,false,1,20).items().getFirst().readAt());
        assertEquals(0,inbox().list(5,true,1,20).items().size());assertEquals(0,inbox().list(5,false,1,20).unreadCount());
        long reminder=count("SELECT notification_id FROM inapp_notifications WHERE kind='REMINDER'");
        assertThrows(InAppNotificationService.NotFound.class,()->inbox().markRead(5,reminder));
    }
    @Test void reminderDueVisibleAndExpiredOrSupersededHidden()throws Exception {
        meetings().create(3,request(List.of(1)));
        sql("UPDATE inapp_notifications SET available_at=UTC_TIMESTAMP(6)-INTERVAL 1 SECOND WHERE kind='REMINDER'");
        assertEquals(2,inbox().list(5,false,1,20).unreadCount());
        sql("UPDATE inapp_notifications SET expires_at=UTC_TIMESTAMP(6)-INTERVAL 1 SECOND WHERE kind='REMINDER'");
        assertEquals(1,inbox().list(5,false,1,20).unreadCount());
        sql("UPDATE inapp_notifications SET superseded_at=UTC_TIMESTAMP(6)");assertEquals(0,inbox().list(5,false,1,20).unreadCount());
    }
    @Test void publisherRequiresTransactionAndRollsBackWithCaller()throws Exception {
        var dao=new InAppNotificationDAO();try(var c=open()) {
            assertThrows(IllegalArgumentException.class,()->dao.publish(c,5,event()));
            c.setAutoCommit(false);dao.publish(c,5,event());c.rollback();
        }assertEquals(0,count("SELECT COUNT(*) FROM inapp_notifications"));
    }
    @Test void duplicateEventIsImmutableAndConcurrentPublicationDeduplicates()throws Exception {
        var start=new java.util.concurrent.CountDownLatch(1);var pool=java.util.concurrent.Executors.newFixedThreadPool(2);
        try {
            java.util.concurrent.Callable<Void> job=()->{start.await();try(var c=open()){c.setAutoCommit(false);new InAppNotificationDAO().publish(c,5,event());c.commit();}return null;};
            var a=pool.submit(job);var b=pool.submit(job);start.countDown();a.get(10,java.util.concurrent.TimeUnit.SECONDS);b.get(10,java.util.concurrent.TimeUnit.SECONDS);
        }finally{pool.shutdownNow();}
        assertEquals(1,count("SELECT COUNT(*) FROM inapp_notifications"));
        var n=inbox().list(5,false,1,20).items().getFirst();inbox().markRead(5,n.id());
        try(var c=open()){c.setAutoCommit(false);new InAppNotificationDAO().publish(c,5,event());c.commit();}
        assertNotNull(inbox().list(5,false,1,20).items().getFirst().readAt());
    }
    @Test void inactiveUsersCannotReadOrReceive()throws Exception {
        sql("UPDATE users SET status='LOCKED' WHERE user_id=5");
        assertThrows(SecurityException.class,()->inbox().list(5,false,1,20));
        try(var c=open()){c.setAutoCommit(false);new InAppNotificationDAO().publish(c,5,event());c.commit();}
        assertEquals(0,count("SELECT COUNT(*) FROM inapp_notifications"));
    }
    @Test void staleOrUnauthorizedWritesCannotPublish()throws Exception {
        long id=meetings().create(3,request(List.of(1)));long before=count("SELECT COUNT(*) FROM inapp_notifications");
        assertThrows(IllegalStateException.class,()->meetings().update(3,id,99,request(List.of(2))));
        assertThrows(SecurityException.class,()->meetings().cancel(4,id,1));
        assertEquals(before,count("SELECT COUNT(*) FROM inapp_notifications"));
    }
}
