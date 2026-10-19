package com.internship.controller.auth;

import com.internship.dao.RoleDAO;
import com.internship.dao.UserDAO;
import com.internship.model.User;
import com.internship.service.UserService;
import com.internship.util.CsrfToken;
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.*;
import java.lang.reflect.Proxy;
import java.sql.SQLException;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ChangePasswordServletTest {
    static class Users extends UserService {
        int calls; long actor; boolean dbFailure;
        Users(){super(new UserDAO(),new RoleDAO());}
        @Override public void changePassword(long id,String old,String next,String confirm)throws SQLException {
            calls++;actor=id;assertEquals("old",old);assertEquals("new",next);assertEquals("new",confirm);
            if(dbFailure)throw new SQLException("SECRET SQL");
        }
    }
    static class Http {
        Map<String,Object> sessionAttributes=new HashMap<>(),requestAttributes=new HashMap<>();
        java.io.StringWriter body=new java.io.StringWriter();
        boolean jsonRequest;
        Map<String,String> params=new HashMap<>(); boolean sessionExists=true, invalidated;int status;String redirect,view;
        final HttpSession session=(HttpSession)Proxy.newProxyInstance(HttpSession.class.getClassLoader(),new Class[]{HttpSession.class},(p,m,a)->switch(m.getName()){
            case "getAttribute"->sessionAttributes.get(a[0]);
            case "setAttribute"->{sessionAttributes.put((String)a[0],a[1]);yield null;}
            case "invalidate"->{invalidated=true;sessionAttributes.clear();yield null;}
            default->null;
        });
        final RequestDispatcher dispatcher=(RequestDispatcher)Proxy.newProxyInstance(RequestDispatcher.class.getClassLoader(),new Class[]{RequestDispatcher.class},(p,m,a)->null);
        final HttpServletRequest request=(HttpServletRequest)Proxy.newProxyInstance(HttpServletRequest.class.getClassLoader(),new Class[]{HttpServletRequest.class},(p,m,a)->switch(m.getName()){
            case "getSession"->sessionExists?session:null;
            case "getParameter"->params.get(a[0]);
            case "getHeader"->jsonRequest && "X-Requested-With".equals(a[0])?"XMLHttpRequest":null;
            case "getContextPath"->"/test";
            case "setAttribute"->{requestAttributes.put((String)a[0],a[1]);yield null;}
            case "getRequestDispatcher"->{view=(String)a[0];yield dispatcher;}
            default->null;
        });
        final HttpServletResponse response=(HttpServletResponse)Proxy.newProxyInstance(HttpServletResponse.class.getClassLoader(),new Class[]{HttpServletResponse.class},(p,m,a)->switch(m.getName()){
            case "getWriter"->new java.io.PrintWriter(body);
            case "sendError","setStatus"->{status=(int)a[0];yield null;}
            case "sendRedirect"->{redirect=(String)a[0];yield null;}
            default->null;
        });
        Http(){User user=new User();user.setUserId(42);sessionAttributes.put("currentUser",user);params.put("currentPassword","old");params.put("newPassword","new");params.put("confirmPassword","new");}
        void token(){params.put("csrfToken",CsrfToken.expose(request));}
    }
    @Test void unauthenticatedPostDoesNotChangePassword()throws Exception{
        Http h=new Http();h.sessionExists=false;Users u=new Users();new ChangePasswordServlet(u).doPost(h.request,h.response);
        assertEquals(401,h.status);assertEquals(0,u.calls);
    }
    @Test void missingAndCrossSessionTokensAreRejected()throws Exception{
        for(boolean cross:new boolean[]{false,true}){
            Http h=new Http();h.token();if(cross){Http other=new Http();other.token();h.params.put("csrfToken",other.params.get("csrfToken"));}else h.params.remove("csrfToken");
            Users u=new Users();new ChangePasswordServlet(u).doPost(h.request,h.response);assertEquals(403,h.status);assertEquals(0,u.calls);assertFalse(h.invalidated);
        }
    }
    @Test void ignoresSubmittedActorAndInvalidatesCurrentSessionOnSuccess()throws Exception{
        Http h=new Http();h.token();h.params.put("userId","999");Users u=new Users();new ChangePasswordServlet(u).doPost(h.request,h.response);
        assertEquals(42,u.actor);assertTrue(h.invalidated);assertEquals("/test/auth/login",h.redirect);assertNull(h.sessionAttributes.get("currentUser"));
    }
    @Test void sqlFailureKeepsSessionAndDoesNotExposeDetails()throws Exception{
        Http h=new Http();h.token();Users u=new Users();u.dbFailure=true;new ChangePasswordServlet(u).doPost(h.request,h.response);
        assertEquals(503,h.status);assertFalse(h.invalidated);assertFalse(h.requestAttributes.get("errorMessage").toString().contains("SECRET"));
        assertEquals("/WEB-INF/views/auth/change-password.jsp",h.view);
    }
    @Test void getRendersOnlyForAuthenticatedUser()throws Exception{
        Http h=new Http();Users u=new Users();new ChangePasswordServlet(u).doGet(h.request,h.response);assertEquals("/WEB-INF/views/auth/change-password.jsp",h.view);assertEquals(0,u.calls);
        h.sessionExists=false;new ChangePasswordServlet(u).doGet(h.request,h.response);assertEquals("/test/auth/login",h.redirect);
    }

    @Test void modalJsonSuccessInvalidatesSessionAndUsesSessionActor()throws Exception{
        Http h=new Http();h.jsonRequest=true;h.token();h.params.put("userId","999");Users u=new Users();
        new ChangePasswordServlet(u).doPost(h.request,h.response);
        assertEquals(200,h.status);assertEquals(42,u.actor);assertTrue(h.invalidated);
        assertNull(h.redirect);assertTrue(h.body.toString().contains("\"success\":true"));
    }
    @Test void modalJsonCsrfFailureNeverChangesPassword()throws Exception{
        Http h=new Http();h.jsonRequest=true;Users u=new Users();new ChangePasswordServlet(u).doPost(h.request,h.response);
        assertEquals(403,h.status);assertEquals(0,u.calls);assertTrue(h.body.toString().contains("\"success\":false"));
    }
    @Test void modalJsonSqlFailureDoesNotExposeSqlOrInvalidateSession()throws Exception{
        Http h=new Http();h.jsonRequest=true;h.token();Users u=new Users();u.dbFailure=true;
        new ChangePasswordServlet(u).doPost(h.request,h.response);
        assertEquals(503,h.status);assertFalse(h.invalidated);assertFalse(h.body.toString().contains("SECRET"));
    }

}
