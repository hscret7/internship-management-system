package com.internship.service;

import com.internship.dao.InAppNotificationDAO;
import java.sql.*;
import java.util.Objects;

/** BE3/BE4 integration seam. The caller authorizes recipients and owns commit/rollback. */
public final class NotificationPublisher {
    private final boolean enabled;
    private final InAppNotificationDAO dao;
    public NotificationPublisher(){this(InAppNotificationService.configured(),new InAppNotificationDAO());}
    public NotificationPublisher(boolean enabled,InAppNotificationDAO dao){this.enabled=enabled;this.dao=Objects.requireNonNull(dao);}
    public void publish(Connection c,long recipient,NotificationEvent event)throws SQLException {
        if(enabled)dao.publish(c,recipient,Objects.requireNonNull(event));
    }
}
