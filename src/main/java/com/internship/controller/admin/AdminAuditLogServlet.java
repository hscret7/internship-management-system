package com.internship.controller.admin;

import com.internship.model.*;
import com.internship.service.AuditLogService;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.sql.SQLException;
import java.time.Instant;

@WebServlet("/admin/audit-logs")
public class AdminAuditLogServlet extends HttpServlet {
    private final AuditLogService service;
    public AdminAuditLogServlet() { this(new AuditLogService()); }
    public AdminAuditLogServlet(AuditLogService service) { this.service=service; }
    @Override protected void doGet(HttpServletRequest req,HttpServletResponse res) throws IOException {
        res.setContentType("application/json;charset=UTF-8");
        res.setHeader("Cache-Control","no-store");
        HttpSession session=req.getSession(false);
        Object value=session==null?null:session.getAttribute("currentUser");
        if(!(value instanceof User user)) { res.setStatus(401); res.getWriter().write("{\"error\":\"Authentication required\"}"); return; }
        // Check before parsing parameters to avoid revealing validation details to non-admins.
        if(user.getStatus()!=UserStatus.ACTIVE || user.getRole()==null || !"ADMIN".equals(user.getRole().getRoleName())) {
            res.setStatus(403); res.getWriter().write("{\"error\":\"Admin required\"}"); return;
        }
        try {
            var q=new AuditLogService.Query(id(req,"actorUserId"),text(req,"action"),text(req,"objectType"),
                    text(req,"result"),instant(req,"from"),instant(req,"to"),number(req,"page",1),number(req,"size",20),text(req,"actorEmail"));
            var page=service.search(user,q);
            StringBuilder json=new StringBuilder("{\"items\":[");
            boolean first=true;
            for(AuditLog log:page.items()) {
                if(!first) json.append(','); first=false;
                json.append("{\"id\":").append(log.id()).append(",\"actorUserId\":").append(log.actorUserId())
                    .append(",\"actorEmail\":").append(quote(log.actorEmail())).append(",\"actorName\":").append(quote(log.actorName()))
                    .append(",\"action\":").append(quote(log.action())).append(",\"objectType\":").append(quote(log.objectType()))
                    .append(",\"objectId\":").append(log.objectId()).append(",\"occurredAt\":").append(quote(log.occurredAt().toString()))
                    .append(",\"result\":").append(quote(log.result())).append('}');
            }
            json.append("],\"total\":").append(page.total()).append(",\"page\":").append(page.page())
                .append(",\"size\":").append(page.size()).append('}');
            res.getWriter().write(json.toString());
        } catch(IllegalArgumentException e) {
            res.setStatus(400); res.getWriter().write("{\"error\":\"Invalid filters or pagination\"}");
        } catch(SecurityException e) {
            res.setStatus(403); res.getWriter().write("{\"error\":\"Admin required\"}");
        } catch(SQLException e) {
            res.setStatus(503); res.getWriter().write("{\"error\":\"Audit store unavailable\"}");
        }
    }
    private static String text(HttpServletRequest r,String k) { String v=r.getParameter(k); return v==null||v.isBlank()?null:v.trim(); }
    private static Long id(HttpServletRequest r,String k) { String v=text(r,k); return v==null?null:Long.valueOf(v); }
    private static int number(HttpServletRequest r,String k,int fallback) { String v=text(r,k); return v==null?fallback:Integer.parseInt(v); }
    private static Instant instant(HttpServletRequest r,String k) {
        String v=text(r,k);
        try { return v==null?null:Instant.parse(v); }
        catch(java.time.format.DateTimeParseException e) { throw new IllegalArgumentException("Invalid instant",e); }
    }
    private static String quote(String value) {
        if (value == null) return "null";
        StringBuilder out=new StringBuilder("\"");
        for(char c:value.toCharArray()) {
            if(c=='"'||c=='\\') out.append('\\').append(c);
            else if(c<32||c=='<'||c=='>'||c=='&') out.append(String.format("\\u%04x",(int)c));
            else out.append(c);
        }
        return out.append('"').toString();
    }
}
