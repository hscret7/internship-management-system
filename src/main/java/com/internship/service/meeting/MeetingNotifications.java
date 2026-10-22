package com.internship.service.meeting;

import com.internship.model.Meeting;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Map;

/** Hooks run in the scheduling transaction; implementations must not send mail. */
public interface MeetingNotifications {
    Map<Long,String> beforeChange(Connection c, Meeting before) throws SQLException;
    void record(Connection c, Meeting before, Map<Long,String> oldRecipients, Meeting after) throws SQLException;
    static MeetingNotifications disabled() {
        return new MeetingNotifications() {
            public Map<Long,String> beforeChange(Connection c,Meeting m) { return Map.of(); }
            public void record(Connection c,Meeting b,Map<Long,String> old,Meeting a) { }
        };
    }
}
