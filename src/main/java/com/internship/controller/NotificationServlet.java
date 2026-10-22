package com.internship.controller;

import com.internship.model.*;
import com.internship.service.InAppNotificationService;
import com.internship.util.CsrfToken;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.sql.SQLException;
import java.util.*;

@WebServlet("/notifications")
public class NotificationServlet extends HttpServlet {
    private final InAppNotificationService service;
    public NotificationServlet(){this(new InAppNotificationService());}
    public NotificationServlet(InAppNotificationService service){this.service=Objects.requireNonNull(service);}
    @Override protected void doGet(HttpServletRequest req,HttpServletResponse res)throws IOException {
        headers(res);User user=actor(req);if(user==null){error(res,401,"Authentication required");return;}
        try {
            String filter=req.getParameter("unreadOnly");
            if(filter!=null&&!Set.of("true","false").contains(filter))throw new IllegalArgumentException();
            var page=service.list(user.getUserId(),"true".equals(filter),number(req,"page",1),number(req,"size",20));
            StringJoiner rows=new StringJoiner(",");for(var n:page.items())rows.add(json(n));
            res.getWriter().write("{\"items\":["+rows+"],\"unreadCount\":"+page.unreadCount()+",\"page\":"+page.page()
                +",\"size\":"+page.size()+",\"csrfToken\":"+quote(CsrfToken.expose(req))+"}");
        }catch(InAppNotificationService.Disabled|SQLException e){error(res,503,"Inbox unavailable");}
        catch(SecurityException e){error(res,403,"Notification access denied");}
        catch(IllegalArgumentException e){error(res,400,"Invalid notification request");}
    }
    @Override protected void doPost(HttpServletRequest req,HttpServletResponse res)throws IOException {
        req.setCharacterEncoding("UTF-8");headers(res);User user=actor(req);
        if(user==null){error(res,401,"Authentication required");return;}
        if(!CsrfToken.valid(req)){error(res,403,"Invalid CSRF token");return;}
        String type=req.getContentType();
        if(type==null||!type.split(";",2)[0].trim().equalsIgnoreCase("application/x-www-form-urlencoded")){error(res,415,"Form encoding required");return;}
        try {
            if(!"read".equals(req.getParameter("action")))throw new IllegalArgumentException();
            long id=Long.parseLong(req.getParameter("id"));service.markRead(user.getUserId(),id);
            res.getWriter().write("{\"id\":"+id+",\"read\":true}");
        }catch(InAppNotificationService.NotFound e){error(res,404,"Notification unavailable");}
        catch(InAppNotificationService.Disabled|SQLException e){error(res,503,"Inbox unavailable");}
        catch(SecurityException e){error(res,403,"Notification access denied");}
        catch(IllegalArgumentException e){error(res,400,"Invalid notification request");}
    }
    private static User actor(HttpServletRequest r){var s=r.getSession(false);return s!=null&&s.getAttribute("currentUser") instanceof User u?u:null;}
    private static int number(HttpServletRequest r,String k,int d){String v=r.getParameter(k);return v==null?d:Integer.parseInt(v);}
    private static void headers(HttpServletResponse r){r.setContentType("application/json");r.setCharacterEncoding("UTF-8");r.setHeader("Cache-Control","no-store");r.setHeader("X-Content-Type-Options","nosniff");}
    private static void error(HttpServletResponse r,int status,String message)throws IOException{r.setStatus(status);r.getWriter().write("{\"error\":"+quote(message)+"}");}
    static String quote(String value){
        if(value==null)return "null";StringBuilder b=new StringBuilder("\"");
        for(char ch:value.toCharArray()){if(ch=='"'||ch=='\\')b.append('\\').append(ch);else if(ch<32||ch=='<'||ch=='>'||ch=='&')b.append(String.format("\\u%04x",(int)ch));else b.append(ch);}return b.append('"').toString();
    }
    private static String json(InAppNotification n){return "{\"id\":"+n.id()+",\"kind\":"+quote(n.kind())+",\"sourceType\":"+quote(n.sourceType())
        +",\"sourceId\":"+n.sourceId()+",\"sourceRevision\":"+n.sourceRevision()+",\"title\":"+quote(n.title())+",\"body\":"+quote(n.body())
        +",\"availableAt\":"+quote(n.availableAt().toString())+",\"readAt\":"+quote(n.readAt()==null?null:n.readAt().toString())+"}";}
}
