package com.internship.controller;

import com.internship.dao.*;
import com.internship.model.*;
import com.internship.service.*;
import com.internship.util.CsrfToken;
import jakarta.servlet.http.*;
import java.lang.reflect.Proxy;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class AdminAccountAuditTest {
 @Test void formCreatesAccountAndRecordsAuthenticatedActorExactlyOnce() throws Exception {
  H h=new H();h.run(false);assertEquals(1,h.auditCalls);assertEquals(99L,h.actor);assertEquals(20L,h.target);assertNotNull(h.sessionData.get("successMessage"));assertEquals("/ims/admin/accounts",h.redirect);
 }
 @Test void validationFailureDoesNotRecordSuccess() throws Exception {
  H h=new H();h.run(true);assertEquals(0,h.auditCalls);assertNotNull(h.sessionData.get("errorMessage"));
 }
 @Test void missingCsrfDoesNotCreateAccountOrRecordSuccess() throws Exception {
  H h=new H();h.token=false;h.run(false);assertEquals(403,h.status);assertEquals(0,h.createCalls);assertEquals(0,h.auditCalls);
 }
 static class H {
  Map<String,Object>sessionData=new HashMap<>(),attributes=new HashMap<>();Map<String,String>params=new HashMap<>();
  boolean token=true;int auditCalls,createCalls,status;Long actor,target;String redirect;
  H(){User admin=new User();admin.setUserId(99);Role r=new Role();r.setRoleName("ADMIN");admin.setRole(r);sessionData.put("currentUser",admin);}
  void run(boolean invalid)throws Exception {
   HttpSession session=p(HttpSession.class,(m,a)->switch(m){case "getAttribute"->sessionData.get(a[0]);case "setAttribute"->{sessionData.put((String)a[0],a[1]);yield null;}default->null;});
   HttpServletRequest req=p(HttpServletRequest.class,(m,a)->switch(m){case "getSession"->session;case "getParameter"->params.get(a[0]);case "getContextPath"->"/ims";case "setAttribute"->{attributes.put((String)a[0],a[1]);yield null;}default->null;});
   HttpServletResponse res=p(HttpServletResponse.class,(m,a)->switch(m){case "sendError"->{status=(int)a[0];yield null;}case "sendRedirect"->{redirect=(String)a[0];yield null;}default->null;});
   if(token)params.put("csrfToken",CsrfToken.expose(req));
   UserService users=new UserService(new UserDAO(),new RoleDAO()){
    @Override public CreateUserResult createUser(String a,String b,String c,String d){createCalls++;if(invalid)throw new IllegalArgumentException("Username already exists.");User u=new User();u.setUserId(20);u.setUsername("qa_s3_audit_create01");Role role=new Role();role.setRoleName("INTERN");u.setRole(role);return new CreateUserResult(u,"secret",true);}
   };
   AuditLogService audit=new AuditLogService(new AuditLogDAO(()->{throw new AssertionError("No real DB in unit tests");})){
    @Override public boolean recordBestEffort(Long a,String action,String type,Long object,String result){auditCalls++;actor=a;target=object;assertEquals("ACCOUNT_CREATE",action);assertEquals("USER",type);assertEquals("SUCCESS",result);return true;}
   };
   new AdminAccountServlet(users,audit).doPost(req,res);
  }
 }
 interface Call{Object call(String m,Object[]a);}
 @SuppressWarnings("unchecked") static<T>T p(Class<T>t,Call c){return(T)Proxy.newProxyInstance(t.getClassLoader(),new Class<?>[]{t},(p,m,a)->c.call(m.getName(),a));}
}
