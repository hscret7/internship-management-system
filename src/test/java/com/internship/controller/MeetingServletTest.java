package com.internship.controller;

import com.internship.model.*;
import com.internship.service.*;
import com.internship.dao.MeetingDAO;
import com.internship.util.CsrfToken;
import jakarta.servlet.http.*;
import java.lang.reflect.Proxy;
import java.io.*;
import java.sql.SQLException;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class MeetingServletTest {
    @Test void anonymousGetReturns401WithoutService() throws Exception {
        H h=new H();h.session.clear();h.get();assertEquals(401,h.status);assertFalse(h.called);
    }
    @Test void internCannotPostEvenWithCsrf() throws Exception {
        H h=new H();h.user.getRole().setRoleName("INTERN");h.token();h.post();assertEquals(403,h.status);assertFalse(h.called);
    }
    @Test void missingCsrfNeverCreates() throws Exception {
        H h=new H();h.post();assertEquals(403,h.status);assertFalse(h.called);
    }
    @Test void invalidTimeReturns400BeforeService() throws Exception {
        H h=new H();h.token();h.params.put("startsAt","bad");h.post();assertEquals(400,h.status);assertFalse(h.called);
    }
    @Test void creationUsesSessionActorAndFormFields() throws Exception {
        H h=new H();h.token();h.params.put("actorUserId","999");h.post();assertEquals(201,h.status);assertEquals(3,h.actor);assertEquals("{\"id\":31}",h.body.toString());
    }
    @Test void escapedDetailsAndTokenAreReturned() throws Exception {
        H h=new H();h.params.put("id","31");h.get();assertEquals(200,h.status);
        assertTrue(h.body.toString().contains("\\u003cscript\\u003e"));assertTrue(h.body.toString().contains("\"csrfToken\""));assertEquals("no-store",h.cache);
    }
    @Test void inaccessibleDetailReturns404() throws Exception {
        H h=new H();h.notFound=true;h.params.put("id","31");h.get();assertEquals(404,h.status);
    }
    @Test void databaseFailureDoesNotExposeSql() throws Exception {
        H h=new H();h.failDb=true;h.get();assertEquals(503,h.status);assertFalse(h.body.toString().contains("SECRET"));
    }
    @Test void revisionConflictReturns409() throws Exception {
        H h=new H();h.token();h.params.put("action","update");h.params.put("id","31");h.params.put("revision","1");h.conflict=true;h.post();assertEquals(409,h.status);
    }
    @Test void unsupportedBodyReturns415() throws Exception {
        H h=new H();h.token();h.contentType="application/json";h.post();assertEquals(415,h.status);assertFalse(h.called);
    }
    static class H {
        User user=new User();Map<String,Object> session=new HashMap<>();Map<String,String>params=new HashMap<>();
        int status=200;long actor;boolean called,failDb,notFound,conflict;String cache;
        String contentType="application/x-www-form-urlencoded";StringWriter body=new StringWriter();
        H() {
            user.setUserId(3);user.setStatus(UserStatus.ACTIVE);Role role=new Role();role.setRoleName("HR");user.setRole(role);session.put("currentUser",user);
            params.putAll(Map.of("action","create","programId","1","title","Meeting","location","Room","startsAt","2026-10-08T09:00:00Z","endsAt","2026-10-08T10:00:00Z","enrollmentIds","1"));
        }
        HttpSession hs=p(HttpSession.class,(m,a)->switch(m){case "getAttribute"->session.get(a[0]);case "setAttribute"->{session.put((String)a[0],a[1]);yield null;}default->null;});
        HttpServletRequest req=p(HttpServletRequest.class,(m,a)->switch(m){case "getSession"->hs;case "getParameter"->params.get(a[0]);case "getContentType"->contentType;default->null;});
        HttpServletResponse res=p(HttpServletResponse.class,(m,a)->switch(m){case "getWriter"->new PrintWriter(body);case "setStatus"->{status=(int)a[0];yield null;}case "setHeader"->{if(a[0].equals("Cache-Control"))cache=(String)a[1];yield null;}default->null;});
        MeetingService service=new MeetingService(()->{throw new AssertionError("No database in servlet tests");},new MeetingDAO(),Clock.systemUTC()) {
            void call(long id)throws SQLException {called=true;actor=id;if(failDb)throw new SQLException("SECRET SQL");}
            @Override public long create(long a,MeetingRequest q)throws SQLException{call(a);assertEquals(List.of(1),q.enrollmentIds());return 31;}
            @Override public Meeting get(long a,long id)throws SQLException{call(a);if(notFound)throw new NotFound();return new Meeting(31,1,3,"<script>",null,"Room",Instant.parse("2026-10-08T09:00:00Z"),Instant.parse("2026-10-08T10:00:00Z"),"SCHEDULED",1,List.of(1));}
            @Override public MeetingPage list(long a,Integer program,String s,int page,int size)throws SQLException{call(a);return new MeetingPage(List.of(),page,size);}
            @Override public void update(long a,long id,int revision,MeetingRequest q)throws SQLException{call(a);if(conflict)throw new IllegalStateException();}
        };
        void token(){params.put("csrfToken",CsrfToken.expose(req));}
        void get()throws Exception{new MeetingServlet(service).doGet(req,res);}
        void post()throws Exception{new MeetingServlet(service).doPost(req,res);}
    }
    interface Call{Object run(String name,Object[]args);}
    @SuppressWarnings("unchecked")static <T>T p(Class<T> type,Call call) {
        return(T)Proxy.newProxyInstance(type.getClassLoader(),new Class[]{type},(o,m,a)->call.run(m.getName(),a));
    }

    @Test void emailParticipantIsResolvedInSelectedProgramBeforeCreate()throws Exception{
        H h=new H();h.token();h.params.put("enrollmentIds","intern@example.com");
        EnrollmentLookupService lookup=new EnrollmentLookupService(){
            @Override public int activeByEmail(User actor,String email,Integer program){
                assertEquals(3,actor.getUserId());assertEquals("intern@example.com",email);assertEquals(1,program);return 1;
            }
        };
        new MeetingServlet(h.service,lookup).doPost(h.req,h.res);assertEquals(201,h.status);assertTrue(h.called);
    }
    @Test void emailWithoutEnrollmentDoesNotCreateMeeting()throws Exception{
        H h=new H();h.token();h.params.put("enrollmentIds","unknown@example.com");
        EnrollmentLookupService lookup=new EnrollmentLookupService(){
            @Override public int activeByEmail(User actor,String email,Integer program){throw new ScheduleLeaveException(400,"VALIDATION_ERROR","No enrollment");}
        };
        new MeetingServlet(h.service,lookup).doPost(h.req,h.res);assertEquals(400,h.status);assertFalse(h.called);
    }

}
