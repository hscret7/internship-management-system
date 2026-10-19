package com.internship.controller;

import com.internship.model.*;
import com.internship.service.*;
import com.internship.util.CsrfToken;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.sql.SQLException;
import java.time.Instant;
import java.util.*;

/** Shared BE1 endpoint; auth filter handles sessions, service checks current DB role/scope. */
@WebServlet("/meetings")
public class MeetingServlet extends HttpServlet {
    private final MeetingService meetings;
    private final EnrollmentLookupService lookup;
    public MeetingServlet() { this(new MeetingService()); }
    public MeetingServlet(MeetingService meetings) {this(meetings,new EnrollmentLookupService());}
    public MeetingServlet(MeetingService meetings,EnrollmentLookupService lookup) {
        this.meetings=Objects.requireNonNull(meetings);this.lookup=Objects.requireNonNull(lookup);
    }

    @Override protected void doGet(HttpServletRequest req,HttpServletResponse res) throws IOException {
        headers(res);
        User actor=actor(req);
        if(actor==null) {error(res,401,"Authentication required");return;}
        try {
            String id=text(req,"id");String json;
            if(id!=null) {
                Meeting m=meetings.get(actor.getUserId(),Long.parseLong(id));
                json="{\"item\":"+json(m)+",\"csrfToken\":"+quote(CsrfToken.expose(req))+"}";
            } else {
                String program=text(req,"programId");
                var page=meetings.list(actor.getUserId(),program==null?null:Integer.valueOf(program),
                        text(req,"status"),number(req,"page",1),number(req,"size",20));
                StringJoiner items=new StringJoiner(",");for(Meeting m:page.items())items.add(json(m));
                json="{\"items\":["+items+"],\"page\":"+page.page()+",\"size\":"+page.size()
                        +",\"csrfToken\":"+quote(CsrfToken.expose(req))+"}";
            }
            res.getWriter().write(json);
        } catch(ScheduleLeaveException e) {error(res,e.status(),e.getMessage());}
        catch(MeetingService.NotFound e) {error(res,404,"Meeting unavailable");}
        catch(IllegalArgumentException e) {error(res,400,"Invalid meeting request");}
        catch(SecurityException e) {error(res,403,"Meeting access denied");}
        catch(SQLException e) {error(res,503,"Meeting store unavailable");}
    }

    @Override protected void doPost(HttpServletRequest req,HttpServletResponse res) throws IOException {
        req.setCharacterEncoding("UTF-8");headers(res);
        User actor=actor(req);
        if(actor==null) {error(res,401,"Authentication required");return;}
        if(actor.getStatus()!=UserStatus.ACTIVE || actor.getRole()==null
                || actor.getRole().getRoleName()==null
                || !Set.of("ADMIN","HR","MENTOR").contains(actor.getRole().getRoleName())) {
            error(res,403,"Meeting write denied");return;
        }
        if(!CsrfToken.valid(req)) {error(res,403,"Invalid CSRF token");return;}
        String contentType=req.getContentType();
        if(contentType==null || !contentType.split(";",2)[0].trim().equalsIgnoreCase("application/x-www-form-urlencoded")) {
            error(res,415,"Form encoding required");return;
        }
        try {
            String action=text(req,"action");long id;
            if("create".equals(action)) {
                id=meetings.create(actor.getUserId(),request(req,actor));res.setStatus(201);
            } else if("update".equals(action)) {
                id=Long.parseLong(text(req,"id"));
                meetings.update(actor.getUserId(),id,number(req,"revision",0),request(req,actor));
            } else if("cancel".equals(action)) {
                id=Long.parseLong(text(req,"id"));
                meetings.cancel(actor.getUserId(),id,number(req,"revision",0));
            } else throw new IllegalArgumentException("Unknown action");
            res.getWriter().write("{\"id\":"+id+"}");
        } catch(ScheduleLeaveException e) {error(res,e.status(),e.getMessage());}
        catch(MeetingService.NotFound e) {error(res,404,"Meeting unavailable");}
        catch(IllegalArgumentException e) {error(res,400,"Invalid meeting request");}
        catch(IllegalStateException e) {error(res,409,"Meeting changed or cancelled; reload first");}
        catch(SecurityException e) {error(res,403,"Meeting write denied");}
        catch(SQLException e) {error(res,503,"Meeting store unavailable");}
    }

    private MeetingRequest request(HttpServletRequest req,User actor) throws SQLException {
        String ids=text(req,"enrollmentIds");
        if(ids==null || ids.length()>2400)throw new IllegalArgumentException("Invalid participants");
        List<Integer> participants=new ArrayList<>();
        int program=number(req,"programId",0);
        for(String token:ids.split(",",-1)) {
            String value=token.trim();
            participants.add(value.contains("@")?lookup.activeByEmail(actor,value,program):Integer.valueOf(value));
        }
        return new MeetingRequest(number(req,"programId",0),text(req,"title"),text(req,"description"),
                text(req,"location"),instant(req,"startsAt"),instant(req,"endsAt"),participants);
    }
    private static Instant instant(HttpServletRequest r,String name) {
        String value=text(r,name);if(value==null)throw new IllegalArgumentException("Missing time");
        try {return Instant.parse(value);}catch(java.time.DateTimeException e){throw new IllegalArgumentException("Invalid time",e);}
    }
    private static User actor(HttpServletRequest req) {
        HttpSession s=req.getSession(false);
        return s!=null && s.getAttribute("currentUser") instanceof User u ? u : null;
    }
    private static String text(HttpServletRequest req,String name) {
        String s=req.getParameter(name);return s==null || s.isBlank()?null:s.trim();
    }
    private static int number(HttpServletRequest req,String name,int fallback) {
        String s=text(req,name);return s==null?fallback:Integer.parseInt(s);
    }
    private static void headers(HttpServletResponse res) {
        res.setContentType("application/json;charset=UTF-8");res.setHeader("Cache-Control","no-store");
        res.setHeader("X-Content-Type-Options","nosniff");
    }
    private static void error(HttpServletResponse res,int status,String message) throws IOException {
        res.setStatus(status);res.getWriter().write("{\"error\":"+quote(message)+"}");
    }
    private static String json(Meeting m) {
        return "{\"id\":"+m.id()+",\"programId\":"+m.programId()+",\"organizerUserId\":"+m.organizerUserId()
                +",\"title\":"+quote(m.title())+",\"description\":"+quote(m.description())
                +",\"location\":"+quote(m.location())+",\"startsAt\":"+quote(m.startsAt().toString())
                +",\"endsAt\":"+quote(m.endsAt().toString())+",\"status\":"+quote(m.status())
                +",\"revision\":"+m.revision()+",\"enrollmentIds\":"+m.enrollmentIds()+"}";
    }
    private static String quote(String s) {
        if(s==null)return "null";
        StringBuilder b=new StringBuilder("\"");
        for(char c:s.toCharArray()) {
            if(c=='"' || c=='\\')b.append('\\').append(c);
            else if(c<32 || c=='<' || c=='>' || c=='&')b.append(String.format("\\u%04x",(int)c));
            else b.append(c);
        }
        return b.append('"').toString();
    }
}
