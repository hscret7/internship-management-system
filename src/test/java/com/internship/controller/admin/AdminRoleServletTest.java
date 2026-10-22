package com.internship.controller.admin;

import com.internship.dao.RoleAssignmentDAO;
import com.internship.dao.UserDAO;
import com.internship.model.*;
import com.internship.util.CsrfToken;
import jakarta.servlet.http.*;
import java.lang.reflect.Proxy;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class AdminRoleServletTest {
    static Object fallback(Class<?> t) {
        if (t==boolean.class) return false;
        if (t==int.class) return 0;
        if (t==long.class) return 0L;
        return null;
    }
    static <T> T proxy(Class<T> t, java.lang.reflect.InvocationHandler h) {
        return t.cast(Proxy.newProxyInstance(t.getClassLoader(),new Class<?>[]{t},h));
    }
    static class Fixture {
        Map<String,Object> attributes = new HashMap<>();
        Map<String,String> params = new HashMap<>();
        int status; boolean changed; String redirect;
        HttpSession session = proxy(HttpSession.class,(p,m,a)->{
            if (m.getName().equals("getAttribute")) return attributes.get(a[0]);
            if (m.getName().equals("setAttribute")) attributes.put((String)a[0],a[1]);
            return fallback(m.getReturnType());
        });
        HttpServletRequest request = proxy(HttpServletRequest.class,(p,m,a)->{
            return switch(m.getName()) {
                case "getSession" -> session;
                case "getParameter" -> params.get(a[0]);
                case "getContextPath" -> "/app";
                default -> fallback(m.getReturnType());
            };
        });
        HttpServletResponse response = proxy(HttpServletResponse.class,(p,m,a)->{
            if (m.getName().equals("sendError")) status = (int)a[0];
            if (m.getName().equals("sendRedirect")) redirect = (String)a[0];
            return fallback(m.getReturnType());
        });
        AdminRoleServlet servlet = new AdminRoleServlet(new UserDAO(),new RoleAssignmentDAO(){
            @Override public boolean change(long actor,long target,String expected,String role) {
                changed=true; assertEquals(1,actor); assertEquals(2,target);
                assertEquals("INTERN",expected); assertEquals("HR",role); return true;
            }
        });
        Fixture(String role) {
            User u=new User(); u.setUserId(1); u.setStatus(UserStatus.ACTIVE);
            Role r=new Role();r.setRoleName(role);u.setRole(r);
            attributes.put("currentUser",u);
            params.put("userId","2");params.put("expectedRole","INTERN");params.put("role","HR");
        }
        void token() { params.put("csrfToken",CsrfToken.expose(request)); }
    }
    @Test void nonAdminCannotChangeRoles() throws Exception {
        for(String role:List.of("HR","MENTOR","INTERN")) {
            Fixture f=new Fixture(role);f.token();f.servlet.doPost(f.request,f.response);
            assertEquals(403,f.status);assertFalse(f.changed);
        }
    }
    @Test void adminNeedsValidCsrf() throws Exception {
        Fixture f=new Fixture("ADMIN");f.servlet.doPost(f.request,f.response);
        assertEquals(403,f.status);assertFalse(f.changed);
        f.token();f.params.put("csrfToken","forged");f.servlet.doPost(f.request,f.response);
        assertEquals(403,f.status);assertFalse(f.changed);
    }
    @Test void validAdminRequestChangesRoleAndRedirects() throws Exception {
        Fixture f=new Fixture("ADMIN");f.token();f.servlet.doPost(f.request,f.response);
        assertTrue(f.changed);assertEquals("/app/admin/roles",f.redirect);
    }
    @Test void malformedUserIdDoesNotReachDao() throws Exception {
        Fixture f=new Fixture("ADMIN");f.token();f.params.put("userId","abc");
        f.servlet.doPost(f.request,f.response);
        assertFalse(f.changed);assertNotNull(f.attributes.get("role.errorMessage"));
    }
    @Test void nonAdminCannotReadPage() throws Exception {
        Fixture f=new Fixture("INTERN");f.servlet.doGet(f.request,f.response);
        assertEquals(403,f.status);
    }
}
