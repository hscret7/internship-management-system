package com.internship.service;

import java.time.Instant;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class NotificationEventTest {
    private NotificationEvent event(String key,String kind,String source,long id,int revision,String title){return new NotificationEvent(key,kind,source,id,revision,title,"body",Instant.EPOCH);}
    @Test void stableServerEventAccepted(){assertEquals("task:1:2",event("task:1:2","TASK","TASK",1,2,"Assigned").key());}
    @Test void invalidSourceAndKindsRejected(){assertThrows(IllegalArgumentException.class,()->event("x","OTHER","TASK",1,1,"x"));assertThrows(IllegalArgumentException.class,()->event("x","TASK","URL",1,1,"x"));}
    @Test void identifiersAndKeyAreBounded(){assertThrows(IllegalArgumentException.class,()->event("x","TASK","TASK",0,1,"x"));assertThrows(IllegalArgumentException.class,()->event("x","TASK","TASK",1,0,"x"));assertThrows(IllegalArgumentException.class,()->event("a b","TASK","TASK",1,1,"x"));}
    @Test void emptyAndOversizedTitlesRejected(){assertThrows(IllegalArgumentException.class,()->event("x","TASK","TASK",1,1," "));assertThrows(IllegalArgumentException.class,()->event("x","TASK","TASK",1,1,"x".repeat(201)));}
    @Test void disabledInboxDoesNotOpenDatabase(){var service=new InAppNotificationService(()->{throw new AssertionError();},new com.internship.dao.InAppNotificationDAO(),false);assertThrows(InAppNotificationService.Disabled.class,()->service.list(1,false,1,20));}
    @Test void invalidPaginationRejectedBeforeDatabase(){var service=new InAppNotificationService(()->{throw new AssertionError();},new com.internship.dao.InAppNotificationDAO(),true);assertThrows(IllegalArgumentException.class,()->service.list(1,false,0,20));assertThrows(IllegalArgumentException.class,()->service.list(1,false,1,101));}
}
