package com.internship.controller.admin;
import com.internship.model.*;
import com.internship.dao.AuditLogDAO;
import com.internship.service.AuditLogService;
import jakarta.servlet.http.*;
import java.lang.reflect.Proxy;
import java.io.*;
import java.sql.SQLException;
import java.time.Instant;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class AdminAuditLogServletTest {
 @Test void anonymousRejectedBeforeQuery()throws Exception{var h=new H();h.user=null;h.run();assertEquals(401,h.status);assertFalse(h.called);}
 @Test void otherRoleRejectedBeforeParsing()throws Exception{var h=new H();h.user.getRole().setRoleName("HR");h.params.put("page","bad");h.run();assertEquals(403,h.status);assertFalse(h.called);}
 @Test void invalidDateReturns400()throws Exception{var h=new H();h.params.put("from","bad");h.run();assertEquals(400,h.status);assertFalse(h.called);}
 @Test void paginatedResponseHasUtcAndNoSecrets()throws Exception{var h=new H();h.run();assertTrue(h.called);assertTrue(h.body.toString().contains("\"actorUserId\":null"));assertTrue(h.body.toString().contains("2026-10-07T09:00:00Z"));assertFalse(h.body.toString().contains("password"));assertEquals("no-store",h.cache);}
 @Test void dbErrorDoesNotLeakSql()throws Exception{var h=new H();h.failure=true;h.run();assertEquals(503,h.status);assertFalse(h.body.toString().contains("SECRET"));}
 @Test void actorEmailFilterReachesServiceAndIdentityIsEscaped()throws Exception{
  var h=new H();h.params.put("actorEmail","Admin@example.com");h.identity=true;h.run();
  assertEquals("Admin@example.com",h.query.actorEmail());
  assertTrue(h.body.toString().contains("\"actorEmail\":\"Admin@example.com\""));
  assertFalse(h.body.toString().contains("<script>"));
 }
 @Test void invalidActorEmailRejectedBeforeSearch()throws Exception{
  var h=new H();h.params.put("actorEmail","bad");h.run();assertEquals(400,h.status);assertFalse(h.called);
 }
 static class H {
  User user=new User();Map<String,String>params=new HashMap<>();int status=200;String cache;boolean called,failure,identity;AuditLogService.Query query;StringWriter body=new StringWriter();
  H(){Role r=new Role();r.setRoleName("ADMIN");user.setRole(r);user.setStatus(UserStatus.ACTIVE);}
  void run()throws Exception{
   HttpSession session=p(HttpSession.class,(m,a)->m.equals("getAttribute")?user:null);
   HttpServletRequest req=p(HttpServletRequest.class,(m,a)->switch(m){case "getSession"->session;case "getParameter"->params.get(a[0]);default->null;});
   HttpServletResponse res=p(HttpServletResponse.class,(m,a)->switch(m){case "getWriter"->new PrintWriter(body);case "setStatus"->{status=(int)a[0];yield null;}case "setHeader"->{if(a[0].equals("Cache-Control"))cache=(String)a[1];yield null;}default->null;});
   var service=new AuditLogService(new AuditLogDAO(){@Override public AuditLogService.Page search(AuditLogService.Query q)throws SQLException{called=true;query=q;if(failure)throw new SQLException("SECRET SQL");return new AuditLogService.Page(List.of(identity?new AuditLog(1,2L,"LOGIN","USER",2L,Instant.parse("2026-10-07T09:00:00Z"),"SUCCESS","Admin@example.com","<script>"):new AuditLog(1,null,"LOGIN","USER",null,Instant.parse("2026-10-07T09:00:00Z"),"FAILURE")),1,q.page(),q.size());}});
   new AdminAuditLogServlet(service).doGet(req,res);
  }
 }
 interface Call{Object call(String m,Object[] a);}
 @SuppressWarnings("unchecked")static<T>T p(Class<T>t,Call c){return(T)Proxy.newProxyInstance(t.getClassLoader(),new Class<?>[]{t},(p,m,a)->c.call(m.getName(),a));}
}
