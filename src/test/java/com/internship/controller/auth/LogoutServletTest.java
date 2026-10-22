package com.internship.controller.auth;

import com.internship.model.User;
import com.internship.service.AuditLogService;
import com.internship.util.CsrfToken;
import jakarta.servlet.Servlet;
import jakarta.servlet.http.*;
import java.lang.reflect.Proxy;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class LogoutServletTest {
    static class Audit extends AuditLogService {
        int calls; Long actor; boolean available=true;
        @Override public boolean recordBestEffort(Long actor,String action,String type,Long object,String result) {
            calls++;this.actor=actor;
            assertEquals("LOGOUT",action);assertEquals("USER",type);
            assertEquals(actor,object);assertEquals("SUCCESS",result);
            return available;
        }
    }
    static class Http {
        Map<String,Object> attributes=new HashMap<>();
        String token,method="POST",redirect; int status;boolean exists=true,invalidated;
        final HttpSession session=(HttpSession)Proxy.newProxyInstance(HttpSession.class.getClassLoader(),new Class[]{HttpSession.class},(p,m,a)->switch(m.getName()) {
            case "getAttribute"->attributes.get(a[0]);
            case "setAttribute"->{attributes.put((String)a[0],a[1]);yield null;}
            case "invalidate"->{invalidated=true;yield null;}
            default->null;
        });
        final HttpServletRequest request=(HttpServletRequest)Proxy.newProxyInstance(HttpServletRequest.class.getClassLoader(),new Class[]{HttpServletRequest.class},(p,m,a)->switch(m.getName()) {
            case "getSession"->exists?session:null;
            case "getParameter"->"csrfToken".equals(a[0])?token:null;
            case "getContextPath"->"/test";
            case "getMethod"->method;
            case "getProtocol"->"HTTP/1.1";
            default->null;
        });
        final HttpServletResponse response=(HttpServletResponse)Proxy.newProxyInstance(HttpServletResponse.class.getClassLoader(),new Class[]{HttpServletResponse.class},(p,m,a)->switch(m.getName()) {
            case "sendError"->{status=(int)a[0];yield null;}
            case "sendRedirect"->{redirect=(String)a[0];yield null;}
            default->null;
        });
        Http() {User user=new User();user.setUserId(5);attributes.put("currentUser",user);}
        void issueToken(){token=CsrfToken.expose(request);}
    }
    private void denied(Http h)throws Exception {
        var audit=new Audit();new LogoutServlet(audit).doPost(h.request,h.response);
        assertEquals(403,h.status);assertFalse(h.invalidated);assertEquals(0,audit.calls);assertNull(h.redirect);
    }
    @Test void missingTokenPreservesSessionAndDoesNotAuditSuccess()throws Exception {var h=new Http();h.issueToken();h.token=null;denied(h);}
    @Test void wrongTokenPreservesSession()throws Exception {var h=new Http();h.issueToken();h.token="wrong";denied(h);}
    @Test void tokenFromAnotherSessionIsRejected()throws Exception {var a=new Http();var b=new Http();a.issueToken();b.issueToken();a.token=b.token;denied(a);}
    @Test void postedTokenCannotBootstrapMissingSessionToken()throws Exception {var h=new Http();h.token="arbitrary";denied(h);}
    @Test void validTokenInvalidatesAndAuditsActor()throws Exception {
        var h=new Http();h.issueToken();var audit=new Audit();new LogoutServlet(audit).doPost(h.request,h.response);
        assertTrue(h.invalidated);assertEquals("/test/auth/login",h.redirect);assertEquals(1,audit.calls);assertEquals(5L,audit.actor);
    }
    @Test void auditOutageDoesNotPreventValidLogout()throws Exception {
        var h=new Http();h.issueToken();var audit=new Audit();audit.available=false;
        new LogoutServlet(audit).doPost(h.request,h.response);assertTrue(h.invalidated);assertEquals("/test/auth/login",h.redirect);
    }
    @Test void noSessionRedirectsWithoutCreatingOrAuditingSession()throws Exception {
        var h=new Http();h.exists=false;var audit=new Audit();new LogoutServlet(audit).doPost(h.request,h.response);
        assertFalse(h.invalidated);assertEquals(0,audit.calls);assertEquals("/test/auth/login",h.redirect);
    }
    @Test void getCannotLogoutOrAudit()throws Exception {
        var h=new Http();h.method="GET";var audit=new Audit();((Servlet)new LogoutServlet(audit)).service(h.request,h.response);
        assertEquals(405,h.status);assertFalse(h.invalidated);assertEquals(0,audit.calls);
    }
}
