package com.internship.dao;

import com.internship.model.InAppNotification;
import com.internship.service.NotificationEvent;
import java.sql.*;
import java.util.*;

/** Never commits: publishing belongs to the business transaction. */
public class InAppNotificationDAO {
    // DATETIME columns contain UTC wall time, regardless of JVM/connection timezone.
    private static Calendar utc() { return Calendar.getInstance(TimeZone.getTimeZone("UTC")); }
    public void publish(Connection c, long recipient, NotificationEvent e) throws SQLException {
        if (recipient <= 0 || c.getAutoCommit()) throw new IllegalArgumentException("Recipient and caller transaction required");
        try (PreparedStatement s = c.prepareStatement("""
                INSERT INTO inapp_notifications(recipient_user_id,event_key,kind,source_type,source_id,
                    source_revision,title,body,available_at)
                SELECT user_id,?,?,?,?,?,?,?,? FROM users WHERE user_id=? AND status='ACTIVE'
                ON DUPLICATE KEY UPDATE notification_id=notification_id
                """)) {
            s.setString(1,e.key()); s.setString(2,e.kind()); s.setString(3,e.sourceType());
            s.setLong(4,e.sourceId()); s.setInt(5,e.sourceRevision()); s.setString(6,e.title());
            s.setString(7,e.body()); s.setTimestamp(8,Timestamp.from(e.availableAt()),utc()); s.setLong(9,recipient);
            s.executeUpdate();
        }
    }
    public boolean active(Connection c,long actor) throws SQLException {
        try (PreparedStatement s=c.prepareStatement("SELECT user_id FROM users WHERE user_id=? AND status='ACTIVE' LOCK IN SHARE MODE")) {
            s.setLong(1,actor); try(ResultSet r=s.executeQuery()) {return r.next();}
        }
    }
    private static final String VISIBLE="recipient_user_id=? AND superseded_at IS NULL AND available_at<=UTC_TIMESTAMP(6) AND (expires_at IS NULL OR expires_at>UTC_TIMESTAMP(6))";
    public List<InAppNotification> list(Connection c,long actor,boolean unread,int page,int size) throws SQLException {
        try(PreparedStatement s=c.prepareStatement("SELECT * FROM inapp_notifications WHERE "+VISIBLE
                +(unread?" AND read_at IS NULL":"")+" ORDER BY available_at DESC,notification_id DESC LIMIT ? OFFSET ?")) {
            s.setLong(1,actor);s.setInt(2,size);s.setInt(3,(page-1)*size);
            List<InAppNotification> rows=new ArrayList<>();
            try(ResultSet r=s.executeQuery()) {while(r.next()) {
                Timestamp read=r.getTimestamp("read_at",utc());
                rows.add(new InAppNotification(r.getLong("notification_id"),r.getString("kind"),r.getString("source_type"),
                    r.getLong("source_id"),r.getInt("source_revision"),r.getString("title"),r.getString("body"),
                    r.getTimestamp("available_at",utc()).toInstant(),read==null?null:read.toInstant()));
            }} return List.copyOf(rows);
        }
    }
    public long unread(Connection c,long actor) throws SQLException {
        try(PreparedStatement s=c.prepareStatement("SELECT COUNT(*) FROM inapp_notifications WHERE "+VISIBLE+" AND read_at IS NULL")) {
            s.setLong(1,actor);try(ResultSet r=s.executeQuery()) {r.next();return r.getLong(1);}
        }
    }
    public boolean markRead(Connection c,long actor,long id) throws SQLException {
        // Lock by owner first; repeated requests preserve the original timestamp.
        try(PreparedStatement s=c.prepareStatement("SELECT notification_id FROM inapp_notifications WHERE "+VISIBLE+" AND notification_id=? FOR UPDATE")) {
            s.setLong(1,actor);s.setLong(2,id);try(ResultSet r=s.executeQuery()) {if(!r.next())return false;}
        }
        try(PreparedStatement s=c.prepareStatement("UPDATE inapp_notifications SET read_at=COALESCE(read_at,UTC_TIMESTAMP(6)) WHERE recipient_user_id=? AND notification_id=?")) {
            s.setLong(1,actor);s.setLong(2,id);s.executeUpdate();return true;
        }
    }
    public void supersedeReminders(Connection c,long meeting,int revision) throws SQLException {
        try(PreparedStatement s=c.prepareStatement("UPDATE inapp_notifications SET superseded_at=UTC_TIMESTAMP(6) WHERE source_type='MEETING' AND source_id=? AND kind='REMINDER' AND source_revision<? AND superseded_at IS NULL")) {
            s.setLong(1,meeting);s.setInt(2,revision);s.executeUpdate();
        }
    }
    public void expireReminders(Connection c,long meeting,int revision,java.time.Instant start) throws SQLException {
        try(PreparedStatement s=c.prepareStatement("UPDATE inapp_notifications SET expires_at=? WHERE source_type='MEETING' AND source_id=? AND kind='REMINDER' AND source_revision=?")) {
            s.setTimestamp(1,Timestamp.from(start),utc());s.setLong(2,meeting);s.setInt(3,revision);s.executeUpdate();
        }
    }
}
