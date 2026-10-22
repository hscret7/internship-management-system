package com.internship.filter;

import com.internship.model.*;
import jakarta.servlet.http.*;
import org.junit.jupiter.api.Test;
import java.lang.reflect.Proxy;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class AuthorizationMatrixTest {
    @Test void rolesAreCheckedAcrossAllFourAreas()throws Exception {
        for(String role:List.of("ADMIN","HR","MENTOR","INTERN")) {
            for(String area:List.of("admin","hr","mentor","intern")) {
                for(String suffix:List.of("","/dashboard")) {
                    Harness h=new Harness(role,"/"+area+suffix);h.run();
                    boolean expected=role.equals("ADMIN")||role.equalsIgnoreCase(area);
                    assertEquals(expected,h.allowed,role+" -> "+area+suffix);
                    assertEquals(expected?0:403,h.error);
                }
            }
        }
    }
    @Test void anonymousRoleAreaRedirectsToLogin()throws Exception {
        Harness h=new Harness(null,"/admin/accounts");h.hasSession=false;h.run();
        assertFalse(h.allowed);assertEquals("/ims/auth/login",h.redirect);
    }
    @Test void malformedRoleDoesNotGrantAccess()throws Exception {
        for(Object attribute:new Object[]{"invalid",new User()}) {
            Harness h=new Harness("ADMIN","/hr/dashboard");h.attribute=attribute;h.run();
            assertFalse(h.allowed);assertEquals("/ims/auth/login",h.redirect);
        }
    }
    @Test void unrelatedPrefixesAreLeftToAuthenticationAndEndpointChecks()throws Exception {
        for(String path:List.of("/administrator","/hr-extra","/mentorship","/internship","/meetings","/notifications")) {
            Harness h=new Harness("INTERN",path);h.run();assertTrue(h.allowed,path);
        }
    }
    private static class Harness {
        Object attribute;String path,redirect;boolean hasSession=true,allowed;int error;
        Harness(String role,String path){this.path=path;User u=new User();Role r=new Role();r.setRoleName(role);u.setRole(r);attribute=u;}
        void run()throws Exception {
            HttpSession session=proxy(HttpSession.class,(m,a)->m.equals("getAttribute")?attribute:null);
            HttpServletRequest request=proxy(HttpServletRequest.class,(m,a)->switch(m){
                case "getContextPath"->"/ims";case "getRequestURI"->"/ims"+path;case "getSession"->hasSession?session:null;default->null;});
            HttpServletResponse response=proxy(HttpServletResponse.class,(m,a)->switch(m){
                case "sendRedirect"->{redirect=(String)a[0];yield null;}
                case "sendError"->{error=(Integer)a[0];yield null;}default->null;});
            new AuthorizationFilter().doFilter(request,response,(req,res)->allowed=true);
        }
    }
    interface Call{Object invoke(String method,Object[] args)throws Throwable;}
    @SuppressWarnings("unchecked") private static <T>T proxy(Class<T> type,Call call){
        return (T)Proxy.newProxyInstance(type.getClassLoader(),new Class<?>[]{type},(p,m,a)->call.invoke(m.getName(),a));
    }
}
