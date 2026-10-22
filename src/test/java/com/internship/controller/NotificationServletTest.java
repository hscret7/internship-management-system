package com.internship.controller;

import com.internship.dao.InAppNotificationDAO;
import com.internship.model.*;
import com.internship.service.InAppNotificationService;
import com.internship.util.CsrfToken;
import jakarta.servlet.http.*;
import java.io.*;
import java.lang.reflect.Proxy;
import java.sql.SQLException;
import java.time.Instant;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class NotificationServletTest {
    @Test void anonymousCannotRead()throws Exception{var h=new H();h.session.clear();h.get();assertEquals(401,h.status);assertFalse(h.called);}
    @Test void sessionActorOverridesClientRecipient()throws Exception{var h=new H();h.params.put("userId","999");h.get();assertEquals(5,h.actor);assertEquals(200,h.status);assertTrue(h.body.toString().contains("\"unreadCount\":1"));}
    @Test void unreadFilterAndPaginationReachService()throws Exception{var h=new H();h.params.putAll(Map.of("unreadOnly","true","page","2","size","7"));h.get();assertTrue(h.unread);assertEquals(2,h.page);assertEquals(7,h.size);}
    @Test void invalidFilterNeverCallsService()throws Exception{var h=new H();h.params.put("unreadOnly","anything");h.get();assertEquals(400,h.status);assertFalse(h.called);}
    @Test void textEscapesScriptAndControlCharacters()throws Exception{var h=new H();h.get();assertTrue(h.body.toString().contains("\\u003cscript\\u003e\\u000a"));assertEquals("no-store",h.cache);}
    @Test void csrfFailureDoesNotWrite()throws Exception{var h=new H();h.post();assertEquals(403,h.status);assertFalse(h.called);}
    @Test void markReadUsesSessionOwner()throws Exception{var h=new H();h.token();h.post();assertEquals(200,h.status);assertEquals(5,h.actor);assertEquals(12,h.id);}
    @Test void foreignNotificationReturns404()throws Exception{var h=new H();h.token();h.missing=true;h.post();assertEquals(404,h.status);}
    @Test void databaseDetailsAreNotExposed()throws Exception{var h=new H();h.fail=true;h.get();assertEquals(503,h.status);assertFalse(h.body.toString().contains("SECRET"));}
    @Test void unsupportedEncodingCannotWrite()throws Exception{var h=new H();h.token();h.type="application/json";h.post();assertEquals(415,h.status);assertFalse(h.called);}
    @Test void endpointCannotPublishEvents()throws Exception{var h=new H();h.token();h.params.put("action","create");h.post();assertEquals(400,h.status);assertFalse(h.called);}
    static class H {
        Map<String,Object> session=new HashMap<>();Map<String,String>params=new HashMap<>();
        int status=200,page,size;long actor,id;boolean called,unread,missing,fail;String cache,type="application/x-www-form-urlencoded";
        StringWriter body=new StringWriter();
        H(){User u=new User();u.setUserId(5);session.put("currentUser",u);params.putAll(Map.of("action","read","id","12"));}
        HttpSession hs=proxy(HttpSession.class,(m,a)->switch(m){case "getAttribute"->session.get(a[0]);case "setAttribute"->{session.put((String)a[0],a[1]);yield null;}default->null;});
        HttpServletRequest req=proxy(HttpServletRequest.class,(m,a)->switch(m){case "getSession"->hs;case "getParameter"->params.get(a[0]);case "getContentType"->type;default->null;});
        HttpServletResponse res=proxy(HttpServletResponse.class,(m,a)->switch(m){case "getWriter"->new PrintWriter(body);case "setStatus"->{status=(int)a[0];yield null;}case "setHeader"->{if(a[0].equals("Cache-Control"))cache=(String)a[1];yield null;}default->null;});
        InAppNotificationService svc=new InAppNotificationService(()->{throw new AssertionError();},new InAppNotificationDAO(),true){
            private void call(long user)throws SQLException{called=true;actor=user;if(fail)throw new SQLException("SECRET");}
            @Override public Page list(long u,boolean only,int p,int s)throws SQLException{call(u);unread=only;page=p;size=s;return new Page(List.of(new InAppNotification(12,"TASK","TASK",3,1,"<script>\n","Body",Instant.EPOCH,null)),1,p,s);}
            @Override public void markRead(long u,long n)throws SQLException{call(u);id=n;if(missing)throw new NotFound();}
        };
        NotificationServlet servlet=new NotificationServlet(svc);
        void token(){params.put("csrfToken",CsrfToken.expose(req));}
        void get()throws IOException{servlet.doGet(req,res);}void post()throws IOException{servlet.doPost(req,res);}
    }
    interface Call{Object run(String m,Object[] args)throws Throwable;}
    @SuppressWarnings("unchecked") static <T>T proxy(Class<T> type,Call call){return (T)Proxy.newProxyInstance(type.getClassLoader(),new Class<?>[]{type},(p,m,a)->call.run(m.getName(),a==null?new Object[0]:a));}
}
