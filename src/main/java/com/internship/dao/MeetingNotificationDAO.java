package com.internship.dao;

import com.internship.model.Meeting;
import com.internship.service.meeting.*;
import java.sql.*;
import java.time.*;
import java.util.*;

/** Caller owns the scheduling transaction. Immutable email/content snapshots. */
public class MeetingNotificationDAO implements MeetingNotifications {
    private final Clock clock;
    private final int leadMinutes;
    public MeetingNotificationDAO(Clock clock,int leadMinutes) {
        this.clock=Objects.requireNonNull(clock);
        if(leadMinutes<1 || leadMinutes>10080)throw new IllegalArgumentException("Invalid lead time");
        this.leadMinutes=leadMinutes;
    }
    private static Calendar utc(){return Calendar.getInstance(TimeZone.getTimeZone("UTC"));}
    @Override public Map<Long,String> beforeChange(Connection c,Meeting before)throws SQLException {
        Map<Long,String> recipients=new TreeMap<>();
        try(PreparedStatement s=c.prepareStatement("SELECT recipient_user_id,recipient_email FROM meeting_recipient_snapshots WHERE meeting_id=? AND revision=? ORDER BY recipient_user_id")) {
            s.setLong(1,before.id());s.setInt(2,before.revision());
            try(ResultSet r=s.executeQuery()){while(r.next())recipients.put(r.getLong(1),r.getString(2));}
        }
        // Legacy meetings predating enqueue have no snapshot. Capture before replacement.
        return recipients.isEmpty()?resolve(c,before):Map.copyOf(recipients);
    }
    public Map<Long,String> resolve(Connection c,Meeting m)throws SQLException {
        Map<Long,String> result=new TreeMap<>();
        for(int enrollment:m.enrollmentIds()) {
            try(PreparedStatement s=c.prepareStatement("""
                SELECT u.user_id,u.email FROM program_enrollments e
                JOIN intern_profiles ip ON ip.profile_id=e.intern_profile_id
                JOIN users u ON u.user_id=ip.user_id
                WHERE e.enrollment_id=? AND e.program_id=? FOR UPDATE
                """)) {
                s.setInt(1,enrollment);s.setInt(2,m.programId());
                try(ResultSet r=s.executeQuery()) {
                    if(!r.next())throw new IllegalArgumentException("Meeting recipient unavailable");
                    result.put(r.getLong(1),MeetingMail.mailbox(r.getString(2)));
                }
            }
        }
        return Map.copyOf(result);
    }
    @Override public void record(Connection c,Meeting before,Map<Long,String> old,Meeting after)throws SQLException {
        if(before!=null && before.revision()==after.revision())return;
        String event=before==null?"CREATE":"CANCELLED".equals(after.status())?"CANCEL":"CHANGE";
        Map<Long,String> current="CANCEL".equals(event)?old:resolve(c,after);
        snapshot(c,after,current);
        if(before!=null) {
            try(PreparedStatement s=c.prepareStatement("""
                UPDATE meeting_notification_outbox SET state='SUPERSEDED',claim_token=NULL,lease_until=NULL
                WHERE meeting_id=? AND event_type='REMINDER' AND state IN ('PENDING','IN_FLIGHT')
                """)) {s.setLong(1,after.id());s.executeUpdate();}
        }
        Map<Long,String> all=new TreeMap<>(old);all.putAll(current);
        Instant now=clock.instant();
        for(var recipient:all.entrySet()) {
            boolean removed=!current.containsKey(recipient.getKey());
            boolean added=before!=null && !old.containsKey(recipient.getKey());
            insert(c,after,event,recipient.getKey(),recipient.getValue(),now,removed,added);
        }
        if(!"CANCELLED".equals(after.status()) && after.startsAt().isAfter(now)) {
            Instant due=after.startsAt().minusSeconds(leadMinutes*60L);
            if(due.isBefore(now))due=now;
            for(var r:current.entrySet())insert(c,after,"REMINDER",r.getKey(),r.getValue(),due,false,false);
        }
    }
    private void snapshot(Connection c,Meeting m,Map<Long,String> recipients)throws SQLException {
        try(PreparedStatement s=c.prepareStatement("INSERT INTO meeting_recipient_snapshots(meeting_id,revision,recipient_user_id,recipient_email) VALUES(?,?,?,?)")) {
            for(var r:new TreeMap<>(recipients).entrySet()) {
                s.setLong(1,m.id());s.setInt(2,m.revision());s.setLong(3,r.getKey());s.setString(4,MeetingMail.mailbox(r.getValue()));s.addBatch();
            }
            s.executeBatch();
        }
    }
    private void insert(Connection c,Meeting m,String event,long user,String email,Instant due,boolean removed,boolean added)throws SQLException {
        MeetingMail.Text text=MeetingMail.render(event,m,removed,added);
        try(PreparedStatement s=c.prepareStatement("""
            INSERT INTO meeting_notification_outbox(meeting_id,revision,event_type,recipient_user_id,recipient_email,subject,body,due_at)
            VALUES(?,?,?,?,?,?,?,?)
            """)) {
            s.setLong(1,m.id());s.setInt(2,m.revision());s.setString(3,event);s.setLong(4,user);s.setString(5,MeetingMail.mailbox(email));
            s.setString(6,text.subject());s.setString(7,text.body());s.setTimestamp(8,Timestamp.from(due),utc());s.executeUpdate();
        }
    }
}
