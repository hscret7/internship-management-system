package com.internship.dao;

import com.internship.model.AuditLog;
import com.internship.service.AuditLogService.Query;
import com.internship.service.AuditLogService.Page;
import com.internship.util.DBConnection;
import java.sql.*;
import java.util.*;

public class AuditLogDAO {
    @FunctionalInterface
    public interface ConnectionFactory { Connection open() throws SQLException; }
    private final ConnectionFactory connections;
    public AuditLogDAO() { this(DBConnection::getConnection); }
    public AuditLogDAO(ConnectionFactory connections) { this.connections = Objects.requireNonNull(connections); }
    private Connection openUtcConnection() throws SQLException {
        Connection c = connections.open();
        try (Statement statement = c.createStatement()) {
            // This dedicated connection is closed after the audit operation.
            statement.execute("SET SESSION time_zone = '+00:00'");
        } catch (SQLException e) {
            try { c.close(); } catch (SQLException closeError) { e.addSuppressed(closeError); }
            throw e;
        }
        return c;
    }
    private static Calendar utcCalendar() {
        return Calendar.getInstance(TimeZone.getTimeZone("UTC"));
    }

    public void insert(Long actor, String action, String type, Long object, String result) throws SQLException {
        try (Connection c = openUtcConnection(); PreparedStatement s = c.prepareStatement(
                "INSERT INTO audit_logs(actor_user_id,action,object_type,object_id,result) VALUES(?,?,?,?,?)")) {
            s.setObject(1, actor, Types.BIGINT); s.setString(2, action); s.setString(3, type);
            s.setObject(4, object, Types.BIGINT); s.setString(5, result); s.executeUpdate();
        }
    }
    /** Shares the caller's transaction; never opens, commits, or closes its connection. */
    public void insertInTransaction(Connection c, Long actor, String action, String type, Long object) throws SQLException {
        if (c.getAutoCommit()) throw new IllegalArgumentException("Caller transaction required");
        if (action == null || object == null || object <= 0 || actor != null && actor <= 0
                || !Set.of("REGISTER", "MEETING_CREATE", "MEETING_UPDATE", "MEETING_CANCEL").contains(action)
                || !("REGISTER".equals(action) ? "USER" : "MEETING").equals(type))
            throw new IllegalArgumentException("Invalid business audit event");
        try (PreparedStatement s=c.prepareStatement(
                "INSERT INTO audit_logs(actor_user_id,action,object_type,object_id,result) VALUES(?,?,?,?,'SUCCESS')")) {
            s.setObject(1,actor,Types.BIGINT);s.setString(2,action);s.setString(3,type);s.setLong(4,object);
            if (s.executeUpdate()!=1) throw new SQLException("Audit insert failed");
        }
    }
    public Page search(Query q) throws SQLException {
        List<Object> args = new ArrayList<>();
        StringBuilder where = new StringBuilder(" WHERE 1=1");
        add(where,args,"a.actor_user_id = ?",q.actorUserId());
        add(where,args,"a.action = ?",q.action());
        add(where,args,"a.object_type = ?",q.objectType());
        add(where,args,"a.result = ?",q.result());
        add(where,args,"a.occurred_at >= ?",q.from() == null ? null : Timestamp.from(q.from()));
        add(where,args,"a.occurred_at < ?",q.to() == null ? null : Timestamp.from(q.to()));
        add(where,args,"LOWER(TRIM(u.email)) = LOWER(?)",q.actorEmail());
        String from = " FROM audit_logs a LEFT JOIN users u ON u.user_id=a.actor_user_id";
        try (Connection c = openUtcConnection()) {
            long total;
            try (PreparedStatement s = c.prepareStatement("SELECT COUNT(*)" + from + where)) {
                bind(s,args); try (ResultSet r = s.executeQuery()) { r.next(); total = r.getLong(1); }
            }
            List<AuditLog> logs = new ArrayList<>();
            try (PreparedStatement s = c.prepareStatement("SELECT a.*, u.email AS actor_email, u.full_name AS actor_name" + from + where
                    + " ORDER BY a.occurred_at DESC,a.audit_id DESC LIMIT ? OFFSET ?")) {
                bind(s,args); s.setInt(args.size()+1,q.size());
                s.setLong(args.size()+2,(long)(q.page()-1)*q.size());
                try (ResultSet r = s.executeQuery()) {
                    while(r.next()) logs.add(new AuditLog(r.getLong("audit_id"),
                            nullable(r,"actor_user_id"),r.getString("action"),r.getString("object_type"),
                            nullable(r,"object_id"),r.getTimestamp("occurred_at", utcCalendar()).toInstant(),r.getString("result"),r.getString("actor_email"),r.getString("actor_name")));
                }
            }
            return new Page(List.copyOf(logs),total,q.page(),q.size());
        }
    }
    private static Long nullable(ResultSet r,String column) throws SQLException {
        long value=r.getLong(column); return r.wasNull()?null:value;
    }
    private static void add(StringBuilder w,List<Object> a,String clause,Object value) {
        if(value!=null) { w.append(" AND ").append(clause); a.add(value); }
    }
    private static void bind(PreparedStatement s,List<Object> args) throws SQLException {
        for(int i=0;i<args.size();i++) {
            Object value = args.get(i);
            if (value instanceof Timestamp timestamp) s.setTimestamp(i+1, timestamp, utcCalendar());
            else s.setObject(i+1,value);
        }
    }
}
