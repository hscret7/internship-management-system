package com.internship.controller;

import com.internship.dao.ScheduleEnrollmentDAO.Choice;
import com.internship.model.*;
import com.internship.service.*;
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.*;
import org.junit.jupiter.api.Test;
import java.io.*;
import java.lang.reflect.*;
import java.sql.SQLException;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class AttendanceEnrollmentSelectionTest {
    final Map<String,Object> attributes=new HashMap<>();
    final Map<String,String> parameters=new HashMap<>();
    int status=200; boolean forwarded; boolean reportRead;
    final StringWriter output=new StringWriter();
    static <T>T proxy(Class<T> type,InvocationHandler handler) {
        return type.cast(Proxy.newProxyInstance(type.getClassLoader(),new Class<?>[]{type},handler));
    }
    User user() { User u=new User();u.setUserId(42L);u.setStatus(UserStatus.ACTIVE);
        Role role=new Role();role.setRoleName("INTERN");u.setRole(role);return u; }
    HttpServletRequest request() {
        HttpSession session=proxy(HttpSession.class,(p,m,a)->m.getName().equals("getAttribute")?user():null);
        RequestDispatcher dispatcher=proxy(RequestDispatcher.class,(p,m,a)->{forwarded=true;return null;});
        return proxy(HttpServletRequest.class,(p,m,a)->switch(m.getName()){
            case "getSession"->session;
            case "getServletPath"->"/intern/attendance";
            case "getParameter"->parameters.get(a[0]);
            case "getHeader"->null;
            case "setAttribute"->{attributes.put((String)a[0],a[1]);yield null;}
            case "getRequestDispatcher"->dispatcher;
            default->null;
        });
    }
    HttpServletResponse response() {
        return proxy(HttpServletResponse.class,(p,m,a)->switch(m.getName()){
            case "setStatus","sendError"->{status=(int)a[0];yield null;}
            case "getWriter"->new PrintWriter(output);
            default->null;
        });
    }
    AttendanceServlet servlet(List<Choice> choices) {
        return new AttendanceServlet(new AttendanceService(){
            @Override public List<AttendanceReportRow> getReport(User u,Integer id,Integer program,java.time.LocalDate from,java.time.LocalDate to){reportRead=true;return List.of();}
        },new EnrollmentLookupService(){
            @Override public List<Choice> lookup(User u,String email){assertEquals(42L,u.getUserId());assertNull(email);return choices;}
        });
    }
    Choice choice(int id) { return new Choice(id,10,"Program","ACTIVE","ACTIVE","Intern","intern@example.com"); }
    @Test void forgedEnrollmentIsRejectedBeforeReadingReportOrForwarding() throws Exception {
        parameters.put("enrollmentId","999");servlet(List.of(choice(1))).doGet(request(),response());
        assertEquals(403,status);assertFalse(reportRead);assertFalse(forwarded);
    }
    @Test void multipleOwnEnrollmentsAreNotSilentlyResolved() throws Exception {
        servlet(List.of(choice(1),choice(2))).doGet(request(),response());
        assertNull(attributes.get("enrollmentId"));assertEquals(2,((List<?>)attributes.get("enrollmentChoices")).size());
        assertTrue(forwarded);
    }
    @Test void databaseFailureIs503WithoutSqlDetails() throws Exception {
        AttendanceServlet servlet=new AttendanceServlet(new AttendanceService(),new EnrollmentLookupService(){
            @Override public List<Choice> lookup(User u,String email)throws SQLException{throw new SQLException("private SQL detail");}
        });
        parameters.put("format","json");servlet.doGet(request(),response());
        assertEquals(503,status);assertFalse(forwarded);assertFalse(output.toString().contains("private SQL detail"));
    }
}
