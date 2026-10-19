package com.internship.service;

import com.internship.dao.AuditLogDAO;
import com.internship.model.*;
import java.sql.SQLException;
import java.time.Instant;
import java.util.*;
import java.util.logging.*;

public class AuditLogService {
    private static final Logger LOG=Logger.getLogger(AuditLogService.class.getName());
    private final AuditLogDAO dao;
    public AuditLogService() { this(new AuditLogDAO()); }
    public AuditLogService(AuditLogDAO dao) { this.dao=Objects.requireNonNull(dao); }
    public void record(Long actor,String action,String type,Long object,String result) throws SQLException {
        positive(actor); positive(object); code(action); code(type);
        if(result == null || !Set.of("SUCCESS","FAILURE").contains(result)) throw new IllegalArgumentException("Invalid result");
        dao.insert(actor,action,type,object,result);
    }
    /** Explicit best effort for nontransactional auth/account hooks. Never logs request secrets. */
    public boolean recordBestEffort(Long actor,String action,String type,Long object,String result) {
        try { record(actor,action,type,object,result); return true; }
        catch(SQLException e) { LOG.log(Level.WARNING,"Audit persistence failed (SQLState={0})",e.getSQLState()); return false; }
    }
    public Page search(User actor,Query query) throws SQLException {
        if(actor==null || actor.getStatus()!=UserStatus.ACTIVE || actor.getRole()==null
                || !"ADMIN".equals(actor.getRole().getRoleName())) throw new SecurityException("Admin required");
        return dao.search(query);
    }
    private static void positive(Long id) { if(id!=null && id<=0) throw new IllegalArgumentException("ID must be positive"); }
    private static void code(String value) {
        if(value==null || !value.matches("[A-Z][A-Z0-9_]{0,63}")) throw new IllegalArgumentException("Invalid audit code");
    }
    public record Query(Long actorUserId,String action,String objectType,String result,
                        Instant from,Instant to,int page,int size,String actorEmail) {
        public Query(Long actorUserId,String action,String objectType,String result,
                     Instant from,Instant to,int page,int size) {
            this(actorUserId,action,objectType,result,from,to,page,size,null);
        }
        public Query {
            if (actorEmail != null) {
                actorEmail = actorEmail.trim();
                if (actorEmail.length() > 254 || !actorEmail.matches("[^\\s@]+@[^\\s@]+\\.[^\\s@]+"))
                    throw new IllegalArgumentException("Invalid actor email");
            }
            positive(actorUserId); if(action!=null) code(action); if(objectType!=null) code(objectType);
            if(result!=null && !Set.of("SUCCESS","FAILURE").contains(result)) throw new IllegalArgumentException("Invalid result");
            if(from!=null && to!=null && !from.isBefore(to)) throw new IllegalArgumentException("Invalid date range");
            if(page<1 || page>1000000 || size<1 || size>100) throw new IllegalArgumentException("Invalid pagination");
        }
    }
    public record Page(List<AuditLog> items,long total,int page,int size) {}
}
