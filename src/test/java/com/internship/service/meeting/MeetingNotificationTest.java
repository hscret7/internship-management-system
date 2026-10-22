package com.internship.service.meeting;

import com.internship.model.Meeting;
import org.junit.jupiter.api.Test;
import java.time.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class MeetingNotificationTest {
    @Test void defaultSettingsCannotSendOrEnqueue() {
        var s=MeetingNotificationSettings.from(new Properties());assertFalse(s.send());assertFalse(s.enqueue());assertEquals(30,s.reminderMinutes());
    }
    @Test void sendWithoutEnqueueAndInvalidBooleansRejected() {
        var p=new Properties();p.setProperty("meeting.notifications.send.enabled","true");
        assertThrows(IllegalArgumentException.class,()->MeetingNotificationSettings.from(p));
        p.setProperty("meeting.notifications.send.enabled","typo");assertThrows(IllegalArgumentException.class,()->MeetingNotificationSettings.from(p));
    }
    @Test void mailboxRejectsMultipleAddressesAndHeaderInjection() {
        assertEquals("intern@example.test",MeetingMail.mailbox("intern@example.test"));
        for(String s:List.of("a@example.test,b@example.test","A <a@example.test>","bad","a@example.test\r\nBcc: bad@example.test"))
            assertThrows(IllegalArgumentException.class,()->MeetingMail.mailbox(s));
    }
    @Test void removedMessageAndReminderArePlainTextVietnamTime() {
        var m=new Meeting(1,1,3,"Topic\r\nInjected",null,"Room",Instant.parse("2026-10-08T10:00:00Z"),Instant.parse("2026-10-08T11:00:00Z"),"SCHEDULED",2,List.of(1));
        var removed=MeetingMail.render("CHANGE",m,true,false);assertTrue(removed.body().contains("không còn"));assertFalse(removed.subject().contains("\n"));
        assertTrue(MeetingMail.render("REMINDER",m,false,false).body().contains("17:00 +07:00"));
    }
    @Test void completionFailureIsNotMisclassifiedAsSendFailure() {
        final int[] calls={0};
        var w=new MeetingNotificationWorker(()->{throw new AssertionError();},(a,b,c)->{},Clock.systemUTC(),new MeetingNotificationSettings(true,true,30,5,600,10)) {
            @Override public Delivery claim(){return new Delivery(1,"token","a@example.test","Subject","Body",1);}
            @Override public boolean finish(Delivery d,boolean sent,String code,boolean terminal)throws java.sql.SQLException {
                calls[0]++;assertTrue(sent);throw new java.sql.SQLException("Acknowledge failed");
            }
        };
        assertThrows(java.sql.SQLException.class,w::runOnce);assertEquals(1,calls[0]);
    }
    @Test void disabledListenerDoesNotNeedMailConfiguration() {
        var listener=new MeetingNotificationListener();listener.contextInitialized(null);listener.contextDestroyed(null);
    }
    @Test void disabledWorkerDoesNotOpenDatabaseOrSend() throws Exception {
        var w=new MeetingNotificationWorker(()->{throw new AssertionError("Opened DB");},(a,b,c)->{throw new AssertionError("Sent mail");},Clock.systemUTC(),MeetingNotificationSettings.from(new Properties()));
        assertFalse(w.runOnce());
    }
}
