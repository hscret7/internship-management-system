package com.internship.service;

import com.internship.dao.MeetingDAO;
import com.internship.dao.MeetingInboxDAO;
import com.internship.dao.MeetingNotificationDAO;
import com.internship.service.meeting.*;
import com.internship.model.Meeting;
import com.internship.util.DBConnection;
import java.sql.*;
import java.time.*;
import java.util.*;

/** BE1 scheduling source. Optional outbox hooks use this transaction; SMTP never runs here. */
public class MeetingService {
    @FunctionalInterface public interface ConnectionFactory { Connection open() throws SQLException; }
    private final ConnectionFactory connections;
    private final MeetingDAO dao;
    private final Clock clock;
    private final MeetingNotifications notifications;
    private final MeetingNotifications inbox;
    public MeetingService() { this(DBConnection::getConnection, new MeetingDAO(), Clock.systemUTC(), configuredNotifications(),
        InAppNotificationService.configured()?new MeetingInboxDAO():MeetingNotifications.disabled()); }
    private static MeetingNotifications configuredNotifications() {
        var settings=MeetingNotificationSettings.system();
        return settings.enqueue()?new MeetingNotificationDAO(Clock.systemUTC(),settings.reminderMinutes()):MeetingNotifications.disabled();
    }
    public MeetingService(ConnectionFactory connections, MeetingDAO dao, Clock clock) {
        this(connections,dao,clock,MeetingNotifications.disabled());
    }
    public MeetingService(ConnectionFactory connections,MeetingDAO dao,Clock clock,MeetingNotifications notifications) {
        this(connections,dao,clock,notifications,MeetingNotifications.disabled());
    }
    public MeetingService(ConnectionFactory connections,MeetingDAO dao,Clock clock,MeetingNotifications notifications,MeetingNotifications inbox) {
        this.inbox=Objects.requireNonNull(inbox);
        this.notifications=Objects.requireNonNull(notifications);
        this.connections = Objects.requireNonNull(connections);
        this.dao = Objects.requireNonNull(dao); this.clock = Objects.requireNonNull(clock);
    }

    public static class NotFound extends IllegalArgumentException {
        public NotFound() { super("Meeting unavailable"); }
    }
    public record MeetingPage(List<Meeting> items, int page, int size) {
        public MeetingPage {items=List.copyOf(items);}
    }
    public Meeting get(long actor,long meetingId) throws SQLException {
        positive(actor);positive(meetingId);
        return transaction(c -> {
            String role=reader(c,actor);
            Meeting m=dao.lockMeeting(c,meetingId);
            if(m==null || !dao.canRead(c,actor,role,m)) throw new NotFound();
            return redact(role,actor,c,m);
        });
    }
    public MeetingPage list(long actor,Integer program,String status,int page,int size) throws SQLException {
        positive(actor);
        if(program!=null && program<=0 || page<1 || page>1000000 || size<1 || size>100)
            throw new IllegalArgumentException("Invalid filters or pagination");
        if(status!=null && !Set.of("SCHEDULED","CANCELLED").contains(status))
            throw new IllegalArgumentException("Invalid status");
        return transaction(c -> {
            String role=reader(c,actor);
            List<Meeting> items=new ArrayList<>();
            for(long id:dao.visibleIds(c,actor,role,program,status,page,size)) {
                Meeting m=dao.lockMeeting(c,id);
                // Recheck scope under locks in case participants/assignment changed after listing.
                if(m!=null && dao.canRead(c,actor,role,m)) items.add(redact(role,actor,c,m));
            }
            return new MeetingPage(items,page,size);
        });
    }
    private String reader(Connection c,long actor) throws SQLException {
        String role=dao.lockActor(c,actor);
        if(role==null || !Set.of("ADMIN","HR","MENTOR","INTERN").contains(role))
            throw new SecurityException("Active authenticated role required");
        return role;
    }
    private Meeting redact(String role,long actor,Connection c,Meeting m) throws SQLException {
        // Intern readers receive meeting details, not another Intern's enrollment identifiers.
        if(!"INTERN".equals(role)) return m;
        return new Meeting(m.id(),m.programId(),m.organizerUserId(),m.title(),m.description(),m.location(),
                m.startsAt(),m.endsAt(),m.status(),m.revision(),List.of());
    }

    public long create(long actor, MeetingRequest q) throws SQLException {
        positive(actor); Objects.requireNonNull(q); future(q);
        return transaction(c -> {
            String role = writer(c, actor);
            validateParticipants(c, actor, role, q.programId(), q.enrollmentIds());
            long id=dao.create(c, actor, q);
            Meeting created=dao.lockMeeting(c,id);
            notifications.record(c,null,Map.of(),created);
            inbox.record(c,null,Map.of(),created);
            dao.recordAudit(c,actor,"MEETING_CREATE",id);
            return id;
        });
    }

    public void update(long actor, long meetingId, int expectedRevision, MeetingRequest q) throws SQLException {
        positive(actor); positive(meetingId); Objects.requireNonNull(q);
        transaction(c -> {
            String role = writer(c, actor);
            Meeting old = existing(c, meetingId);
            authorizeExisting(c, actor, role, old);
            checkRevision(old, expectedRevision);
            if (!"SCHEDULED".equals(old.status())) throw new IllegalStateException("Meeting cancelled");
            if (old.programId() != q.programId()) throw new IllegalArgumentException("Program cannot change");
            future(q);
            validateParticipants(c, actor, role, q.programId(), q.enrollmentIds());
            Map<Long,String> previous=notifications.beforeChange(c,old);
            Map<Long,String> previousInbox=inbox.beforeChange(c,old);
            dao.update(c, meetingId, q);
            Meeting changed=dao.lockMeeting(c,meetingId);
            notifications.record(c,old,previous,changed);
            inbox.record(c,old,previousInbox,changed);
            dao.recordAudit(c,actor,"MEETING_UPDATE",meetingId);
            return null;
        });
    }

    public void cancel(long actor, long meetingId, int expectedRevision) throws SQLException {
        positive(actor); positive(meetingId);
        transaction(c -> {
            String role = writer(c, actor);
            Meeting old = existing(c, meetingId);
            authorizeExisting(c, actor, role, old);
            checkRevision(old, expectedRevision);
            if ("SCHEDULED".equals(old.status())) {
                Map<Long,String> previous=notifications.beforeChange(c,old);
                Map<Long,String> previousInbox=inbox.beforeChange(c,old);
                dao.cancel(c, meetingId);
                Meeting changed=dao.lockMeeting(c,meetingId);
                notifications.record(c,old,previous,changed);
                inbox.record(c,old,previousInbox,changed);
                dao.recordAudit(c,actor,"MEETING_CANCEL",meetingId);
            }
            return null;
        });
    }

    private Meeting existing(Connection c, long id) throws SQLException {
        Meeting old = dao.lockMeeting(c, id);
        if (old == null) throw new NotFound();
        return old;
    }
    private static void checkRevision(Meeting old, int revision) {
        if (revision < 1) throw new IllegalArgumentException("Invalid revision");
        if (old.revision() != revision) throw new IllegalStateException("Meeting changed; reload first");
    }
    private String writer(Connection c, long actor) throws SQLException {
        String role = dao.lockActor(c, actor);
        if (role == null || !Set.of("ADMIN", "HR", "MENTOR").contains(role))
            throw new SecurityException("Active scheduling role required");
        return role;
    }
    private void authorizeExisting(Connection c, long actor, String role, Meeting old) throws SQLException {
        if ("MENTOR".equals(role)) {
            if (old.organizerUserId() != actor) throw new SecurityException("Only organizing mentor may modify");
            validateParticipants(c, actor, role, old.programId(), old.enrollmentIds());
        }
    }
    private void validateParticipants(Connection c, long actor, String role, int program, List<Integer> ids)
            throws SQLException {
        if (!dao.lockProgram(c, program)) throw new IllegalArgumentException("Program unavailable");
        Long mentor = "MENTOR".equals(role) ? actor : null;
        for (int id : ids)
            if (!dao.lockParticipant(c, program, id, mentor))
                throw new SecurityException("Participant outside permitted active scope");
    }
    private void future(MeetingRequest q) {
        if (!q.startsAt().isAfter(clock.instant())) throw new IllegalArgumentException("Meeting must start in future");
    }
    private static void positive(long id) {
        if (id <= 0) throw new IllegalArgumentException("Invalid ID");
    }
    @FunctionalInterface private interface Work<T> { T run(Connection c) throws SQLException; }
    private <T> T transaction(Work<T> work) throws SQLException {
        try (Connection c = connections.open()) {
            c.setAutoCommit(false);
            try {
                try (Statement s = c.createStatement()) { s.execute("SET SESSION time_zone = '+00:00'"); }
                T result = work.run(c); c.commit(); return result;
            } catch (SQLException | RuntimeException e) {
                try { c.rollback(); } catch (SQLException rollback) { e.addSuppressed(rollback); }
                throw e;
            }
        }
    }
}


