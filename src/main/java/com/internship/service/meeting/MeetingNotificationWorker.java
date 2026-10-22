package com.internship.service.meeting;

import com.internship.service.MeetingService.ConnectionFactory;
import com.internship.service.email.EmailService;
import java.sql.*;
import java.time.*;
import java.util.*;

/** One claimed delivery per poll. Commits/closes before invoking EmailService. */
public class MeetingNotificationWorker {
    public record Delivery(long id,String token,String email,String subject,String body,int attempt) { }
    private final ConnectionFactory connections;
    private final EmailService email;
    private final Clock clock;
    private final MeetingNotificationSettings settings;
    public MeetingNotificationWorker(ConnectionFactory connections,EmailService email,Clock clock,MeetingNotificationSettings settings) {
        this.connections=Objects.requireNonNull(connections);this.email=Objects.requireNonNull(email);
        this.clock=Objects.requireNonNull(clock);this.settings=Objects.requireNonNull(settings);
    }
    private static Calendar utc(){return Calendar.getInstance(TimeZone.getTimeZone("UTC"));}
    private static void time(PreparedStatement s,int i,Instant at)throws SQLException{s.setTimestamp(i,Timestamp.from(at),utc());}
    public boolean runOnce()throws SQLException {
        if(!settings.send())return false;
        Delivery item=claim();
        if(item==null)return false;
        try {
            email.sendEmail(item.email(),item.subject(),item.body());
        } catch(IllegalArgumentException e) {
            finish(item,false,"INVALID_INPUT",true);return true;
        } catch(RuntimeException e) {
            finish(item,false,"EMAIL_FAILURE",false);return true;
        }
        // Keep this outside send's catch: DB acknowledgment failure is not an SMTP failure.
        finish(item,true,null,false);return true;
    }
    public Delivery claim()throws SQLException {
        Instant now=clock.instant();
        try(Connection c=connections.open()) {
            c.setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);c.setAutoCommit(false);
            try {
                try(Statement s=c.createStatement()){s.execute("SET SESSION time_zone = '+00:00'");}
                try(PreparedStatement s=c.prepareStatement("""
                    UPDATE meeting_notification_outbox SET state=CASE WHEN attempts>=? THEN 'FAILED' ELSE 'PENDING' END,
                    claim_token=NULL,lease_until=NULL,error_code='LEASE_EXPIRED'
                    WHERE state='IN_FLIGHT' AND lease_until<=?
                    """)){s.setInt(1,settings.maxAttempts());time(s,2,now);s.executeUpdate();}
                try(PreparedStatement s=c.prepareStatement("""
                    UPDATE meeting_notification_outbox n JOIN meetings m ON m.meeting_id=n.meeting_id
                    SET n.state='SUPERSEDED',n.claim_token=NULL,n.lease_until=NULL
                    WHERE n.event_type='REMINDER' AND n.state='PENDING'
                    AND (m.status<>'SCHEDULED' OR m.revision<>n.revision OR m.starts_at<=?)
                    """)){time(s,1,now);s.executeUpdate();}
                Delivery item=null;
                try(PreparedStatement s=c.prepareStatement("""
                    SELECT n.notification_id,n.recipient_email,n.subject,n.body,n.attempts
                    FROM meeting_notification_outbox n JOIN meetings m ON m.meeting_id=n.meeting_id
                    WHERE n.state='PENDING' AND n.due_at<=? AND n.attempts<?
                    AND (n.event_type<>'REMINDER' OR (m.status='SCHEDULED' AND m.revision=n.revision AND m.starts_at>?))
                    AND NOT EXISTS (SELECT 1 FROM meeting_notification_outbox earlier
                      WHERE earlier.meeting_id=n.meeting_id AND earlier.recipient_user_id=n.recipient_user_id
                      AND earlier.notification_id<n.notification_id AND earlier.event_type<>'REMINDER'
                      AND earlier.state IN ('PENDING','IN_FLIGHT'))
                    ORDER BY n.notification_id LIMIT 1 FOR UPDATE SKIP LOCKED
                    """)) {
                    time(s,1,now);s.setInt(2,settings.maxAttempts());time(s,3,now);
                    try(ResultSet r=s.executeQuery()) {if(r.next())item=new Delivery(r.getLong(1),UUID.randomUUID().toString(),r.getString(2),r.getString(3),r.getString(4),r.getInt(5)+1);}
                }
                if(item!=null) {
                    try(PreparedStatement s=c.prepareStatement("""
                        UPDATE meeting_notification_outbox SET state='IN_FLIGHT',claim_token=?,lease_until=?,attempts=attempts+1
                        WHERE notification_id=? AND state='PENDING'
                        """)) {
                        s.setString(1,item.token());time(s,2,now.plusSeconds(settings.leaseSeconds()));s.setLong(3,item.id());
                        if(s.executeUpdate()!=1)throw new SQLException("Claim lost");
                    }
                }
                c.commit();return item;
            } catch(SQLException|RuntimeException e){try{c.rollback();}catch(SQLException x){e.addSuppressed(x);}throw e;}
        }
    }
    public boolean finish(Delivery item,boolean sent,String code,boolean terminal)throws SQLException {
        Instant now=clock.instant();
        String state=sent?"SENT":terminal||item.attempt()>=settings.maxAttempts()?"FAILED":"PENDING";
        long backoff=Math.min(3600,30L*(1L<<Math.min(10,item.attempt()-1)));
        try(Connection c=connections.open();PreparedStatement s=c.prepareStatement("""
            UPDATE meeting_notification_outbox SET state=?,error_code=?,due_at=?,sent_at=?,claim_token=NULL,lease_until=NULL
            WHERE notification_id=? AND state='IN_FLIGHT' AND claim_token=? AND lease_until>?
            """)) {
            s.setString(1,state);s.setString(2,code);time(s,3,now.plusSeconds(sent?0:backoff));
            if(sent)time(s,4,now);else s.setNull(4,Types.TIMESTAMP);
            s.setLong(5,item.id());s.setString(6,item.token());time(s,7,now);
            return s.executeUpdate()==1;
        }
    }
}
