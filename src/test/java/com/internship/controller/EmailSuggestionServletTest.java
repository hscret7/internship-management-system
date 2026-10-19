package com.internship.controller;
import com.internship.dao.EmailSuggestionDAO.*;
import com.internship.model.*;
import com.internship.service.EmailSuggestionService;
import jakarta.servlet.http.*;
import org.junit.jupiter.api.Test;
import java.lang.reflect.Proxy;
import java.io.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
class EmailSuggestionServletTest {
    interface Call{Object run(String name,Object[] args);}
    @SuppressWarnings("unchecked")static<T>T p(Class<T> type,Call call){return(T)Proxy.newProxyInstance(type.getClassLoader(),new Class[]{type},(o,m,a)->call.run(m.getName(),a));}
    static class H {
        User user=new User();String route="/api/hr/email-suggestions";Map<String,String> params=new HashMap<>();
        int status=200;boolean called;Scope scope;StringWriter body=new StringWriter();String cache;
        H(){Role role=new Role();role.setRoleName("HR");user.setRole(role);user.setUserId(3);user.setStatus(UserStatus.ACTIVE);}
        void run()throws Exception{
            HttpSession session=p(HttpSession.class,(n,a)->user);
            HttpServletRequest req=p(HttpServletRequest.class,(n,a)->switch(n){case "getSession"->user==null?null:session;case "getServletPath"->route;case "getParameter"->params.get(a[0]);default->null;});
            HttpServletResponse res=p(HttpServletResponse.class,(n,a)->switch(n){case "setStatus"->{status=(int)a[0];yield null;}case "setHeader"->{cache=(String)a[1];yield null;}case "getWriter"->new PrintWriter(body);default->null;});
            EmailSuggestionService service=new EmailSuggestionService(){@Override public List<Suggestion> search(User actor,Scope selected,String query){called=true;scope=selected;return List.of(new Suggestion("qa@example.com","<script>"));}};
            new EmailSuggestionServlet(service).doGet(req,res);
        }
    }
    @Test void anonymousIs401()throws Exception{H h=new H();h.user=null;h.run();assertEquals(401,h.status);assertFalse(h.called);}
    @Test void internIsRejectedBeforeInvalidScopeParsing()throws Exception{H h=new H();h.user.getRole().setRoleName("INTERN");h.params.put("scope","wrong");h.run();assertEquals(403,h.status);assertFalse(h.called);}
    @Test void hrCannotRequestAuditActors()throws Exception{H h=new H();h.route="/admin/email-suggestions";h.run();assertEquals(403,h.status);assertFalse(h.called);}
    @Test void adminActorRouteCannotBeRetargetedByScopeParameter()throws Exception{H h=new H();h.user.getRole().setRoleName("ADMIN");h.route="/admin/email-suggestions";h.params.put("scope","application");h.run();assertEquals(Scope.ACTOR,h.scope);}
    @Test void applicationScopeResponseHasOnlySafeIdentityFields()throws Exception{H h=new H();h.params.put("scope","application");h.run();assertEquals(Scope.APPLICATION,h.scope);assertEquals("no-store",h.cache);assertTrue(h.body.toString().contains("qa@example.com"));assertFalse(h.body.toString().contains("<script>"));assertFalse(h.body.toString().contains("phone"));}
}
