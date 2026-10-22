package com.internship.controller;

import com.internship.dao.ScheduleEnrollmentDAO.Choice;
import com.internship.model.*;
import com.internship.service.*;
import jakarta.servlet.http.*;
import org.junit.jupiter.api.Test;
import java.lang.reflect.Proxy;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class AllowanceEmailSelectionTest {
    interface Call { Object run(String name,Object[] args) throws Throwable; }
    @SuppressWarnings("unchecked") static <T>T proxy(Class<T> type,Call call) {
        return (T)Proxy.newProxyInstance(type.getClassLoader(),new Class[]{type},(p,m,a)->call.run(m.getName(),a));
    }
    static class Harness {
        Map<String,String> params=new HashMap<>();Map<String,Object> attributes=new HashMap<>();
        int status;Integer selected;boolean queried;
        User user=new User();
        Harness(){Role role=new Role();role.setRoleName("HR");user.setRole(role);user.setUserId(3);user.setStatus(UserStatus.ACTIVE);}
        void run() throws Exception {
            HttpSession session=proxy(HttpSession.class,(n,a)->user);
            HttpServletRequest req=proxy(HttpServletRequest.class,(n,a)->switch(n){
                case "getSession"->session;case "getServletPath"->"/hr/allowances";case "getParameter"->params.get(a[0]);
                case "setAttribute"->{attributes.put((String)a[0],a[1]);yield null;}
                case "getRequestDispatcher"->(jakarta.servlet.RequestDispatcher)new jakarta.servlet.RequestDispatcher(){
                    public void forward(jakarta.servlet.ServletRequest r,jakarta.servlet.ServletResponse s){}
                    public void include(jakarta.servlet.ServletRequest r,jakarta.servlet.ServletResponse s){}
                };
                default->null;
            });
            HttpServletResponse res=proxy(HttpServletResponse.class,(n,a)->{if(n.equals("sendError"))status=(int)a[0];return null;});
            var lookup=new EnrollmentLookupService(){@Override public List<Choice> lookup(User actor,String email){
                if("bad".equals(email))throw new ScheduleLeaveException(400,"VALIDATION_ERROR","Email không hợp lệ.");
                return List.of(new Choice(12,1,"Old","COMPLETED","COMPLETED","A",email),new Choice(13,2,"Current","ACTIVE","ACTIVE","A",email));
            }};
            var service=new AllowanceService(){@Override public List<Allowance> getAllowances(User actor,Integer enrollment){queried=true;selected=enrollment;return List.of();}};
            new AllowanceServlet(service,lookup){@Override public void log(String message,Throwable error){}}.doGet(req,res);
        }
    }
    @Test void historicalSelectionMustMatchEmailAndIsPreserved()throws Exception{
        Harness h=new Harness();h.params.put("email","a@example.com");h.params.put("enrollmentId","12");h.run();
        assertTrue(h.queried);assertEquals(12,h.selected);
    }
    @Test void wrongHiddenIdCannotSelectAnotherPerson()throws Exception{
        Harness h=new Harness();h.params.put("email","a@example.com");h.params.put("enrollmentId","99");h.run();
        assertEquals(400,h.status);assertFalse(h.queried);
    }
    @Test void ambiguousEmailWithoutSelectionDoesNotLoadEveryonesData()throws Exception{
        Harness h=new Harness();h.params.put("email","a@example.com");h.run();assertEquals(400,h.status);assertFalse(h.queried);
    }
    @Test void malformedEmailReturns400WithoutLoadingAllAllowances()throws Exception{
        Harness h=new Harness();h.params.put("email","bad");h.run();assertEquals(400,h.status);assertFalse(h.queried);
    }
}
