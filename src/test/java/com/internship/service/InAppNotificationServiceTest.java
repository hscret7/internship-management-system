package com.internship.service;

import com.internship.dao.InAppNotificationDAO;
import com.internship.model.InAppNotification;
import java.lang.reflect.Proxy;
import java.sql.*;
import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class InAppNotificationServiceTest {
    static class H {
        boolean active=true,found=true,fail,committed,rolledBack,closed;long actor,id;
        Statement statement=(Statement)Proxy.newProxyInstance(Statement.class.getClassLoader(),new Class<?>[]{Statement.class},(p,m,a)->m.getName().equals("execute")?true:null);
        Connection connection=(Connection)Proxy.newProxyInstance(Connection.class.getClassLoader(),new Class<?>[]{Connection.class},(p,m,a)->switch(m.getName()){
            case "createStatement"->statement;case "commit"->{committed=true;yield null;}case "rollback"->{rolledBack=true;yield null;}case "close"->{closed=true;yield null;}default->null;});
        InAppNotificationDAO dao=new InAppNotificationDAO(){
            @Override public boolean active(Connection c,long user){actor=user;return active;}
            @Override public List<InAppNotification> list(Connection c,long user,boolean unread,int page,int size)throws SQLException{if(fail)throw new SQLException("failure");return List.of();}
            @Override public long unread(Connection c,long user){return 0;}
            @Override public boolean markRead(Connection c,long user,long n){actor=user;id=n;return found;}
        };
        InAppNotificationService service=new InAppNotificationService(()->connection,dao,true);
    }
    @Test void currentUserIsCheckedAndSuccessfulReadCommits()throws Exception{var h=new H();h.service.list(5,false,1,20);assertEquals(5,h.actor);assertTrue(h.committed);assertTrue(h.closed);}
    @Test void inactiveUserRollsBackBeforeListing(){var h=new H();h.active=false;assertThrows(SecurityException.class,()->h.service.list(5,false,1,20));assertTrue(h.rolledBack);assertFalse(h.committed);assertTrue(h.closed);}
    @Test void sqlFailureRollsBackAndCloses(){var h=new H();h.fail=true;assertThrows(SQLException.class,()->h.service.list(5,false,1,20));assertTrue(h.rolledBack);assertFalse(h.committed);assertTrue(h.closed);}
    @Test void unavailableNotificationRollsBack(){var h=new H();h.found=false;assertThrows(InAppNotificationService.NotFound.class,()->h.service.markRead(5,12));assertTrue(h.rolledBack);assertEquals(5,h.actor);assertEquals(12,h.id);}
    @Test void successfulMarkReadCommits()throws Exception{var h=new H();h.service.markRead(5,12);assertTrue(h.committed);assertFalse(h.rolledBack);}
    @Test void disabledPublisherDoesNotTouchConnection()throws Exception{new NotificationPublisher(false,new InAppNotificationDAO(){@Override public void publish(Connection c,long u,NotificationEvent e){throw new AssertionError();}}).publish(null,5,null);}
    @Test void enabledPublisherUsesCallerConnection()throws Exception{var h=new H();var event=new NotificationEvent("task:1:1","TASK","TASK",1,1,"Task","Body",java.time.Instant.EPOCH);boolean[] called={false};new NotificationPublisher(true,new InAppNotificationDAO(){@Override public void publish(Connection c,long u,NotificationEvent e){assertSame(h.connection,c);assertSame(event,e);assertEquals(5,u);called[0]=true;}}).publish(h.connection,5,event);assertTrue(called[0]);assertFalse(h.committed);assertFalse(h.closed);}
}
