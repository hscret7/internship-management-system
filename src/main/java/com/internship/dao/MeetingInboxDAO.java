package com.internship.dao;

import com.internship.model.Meeting;
import com.internship.service.NotificationEvent;
import com.internship.service.meeting.MeetingNotifications;
import java.sql.*;
import java.time.*;
import java.util.*;

/** In-app events are independent of SMTP settings and email addresses. */
public class MeetingInboxDAO implements MeetingNotifications {
    private final InAppNotificationDAO inbox;
    private final Clock clock;
    public MeetingInboxDAO(){this(new InAppNotificationDAO(),Clock.systemUTC());}
    public MeetingInboxDAO(InAppNotificationDAO inbox,Clock clock){this.inbox=Objects.requireNonNull(inbox);this.clock=Objects.requireNonNull(clock);}
    @Override public Map<Long,String> beforeChange(Connection c,Meeting m)throws SQLException {
        Map<Long,String> ids=new TreeMap<>();
        for(int enrollment:m.enrollmentIds()) {
            try(PreparedStatement s=c.prepareStatement("""
                SELECT u.user_id FROM program_enrollments e
                JOIN intern_profiles ip ON ip.profile_id=e.intern_profile_id
                JOIN users u ON u.user_id=ip.user_id
                WHERE e.enrollment_id=? AND e.program_id=? AND u.status='ACTIVE' LOCK IN SHARE MODE
                """)) {
                s.setInt(1,enrollment);s.setInt(2,m.programId());
                try(ResultSet r=s.executeQuery()){while(r.next())ids.put(r.getLong(1),"");}
            }
        } return Map.copyOf(ids);
    }
    @Override public void record(Connection c,Meeting before,Map<Long,String> old,Meeting after)throws SQLException {
        Map<Long,String> current=beforeChange(c,after);
        String kind="CANCELLED".equals(after.status())?"CANCEL":before==null?"CREATE":"CHANGE";
        Set<Long> recipients=new TreeSet<>(current.keySet());recipients.addAll(old.keySet());
        inbox.supersedeReminders(c,after.id(),after.revision());
        Instant now=clock.instant();
        for(long user:recipients) {
            String body=after.title()+"\n"+after.startsAt()+" – "+after.endsAt()
                +("CHANGE".equals(kind)&&!current.containsKey(user)?"\nBạn không còn tham gia cuộc họp này.":"");
            inbox.publish(c,user,event(after,kind,body,now));
        }
        if("SCHEDULED".equals(after.status())) {
            Instant due=after.startsAt().minus(Duration.ofMinutes(30));if(due.isBefore(now))due=now;
            for(long user:current.keySet())inbox.publish(c,user,event(after,"REMINDER",after.title()+"\n"+after.startsAt(),due));
            inbox.expireReminders(c,after.id(),after.revision(),after.startsAt());
        }
    }
    private static NotificationEvent event(Meeting m,String kind,String body,Instant due) {
        String title=switch(kind){case "CREATE"->"Cuộc họp mới";case "CHANGE"->"Cuộc họp thay đổi";case "CANCEL"->"Cuộc họp đã hủy";default->"Nhắc lịch họp";};
        return new NotificationEvent("meeting:"+m.id()+":"+m.revision()+":"+kind,kind,"MEETING",m.id(),m.revision(),title,body,due);
    }
}
