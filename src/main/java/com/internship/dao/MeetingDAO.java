package com.internship.dao;

import com.internship.model.Meeting;
import com.internship.service.MeetingRequest;
import java.sql.*;
import java.util.*;

/** All methods use the caller's transaction; this DAO never commits or opens connections. */
public class MeetingDAO {
    public void recordAudit(Connection c,long actor,String action,long meeting) throws SQLException {
        new AuditLogDAO().insertInTransaction(c,actor,action,"MEETING",meeting);
    }

    private static Calendar utc() { return Calendar.getInstance(TimeZone.getTimeZone("UTC")); }

    /** SQL applies visibility before pagination; caller rechecks ACTIVE actor in transaction. */
    public List<Long> visibleIds(Connection c, long actor, String role, Integer program,
                                 String status, int page, int size) throws SQLException {
        String scope = switch (role) {
            case "ADMIN", "HR" -> "1=1";
            case "INTERN" -> """
                EXISTS (SELECT 1 FROM meeting_participants mp
                JOIN program_enrollments e ON e.enrollment_id=mp.enrollment_id
                JOIN intern_profiles ip ON ip.profile_id=e.intern_profile_id
                WHERE mp.meeting_id=m.meeting_id AND ip.user_id=?)
                """;
            case "MENTOR" -> """
                EXISTS (SELECT 1 FROM meeting_participants mp WHERE mp.meeting_id=m.meeting_id)
                AND NOT EXISTS (SELECT 1 FROM meeting_participants mp
                WHERE mp.meeting_id=m.meeting_id AND NOT EXISTS (
                    SELECT 1 FROM program_enrollments e
                    JOIN intern_profiles ip ON ip.profile_id=e.intern_profile_id
                    JOIN users iu ON iu.user_id=ip.user_id
                    JOIN roles ir ON ir.role_id=iu.role_id
                    JOIN mentor_assignments ma ON ma.enrollment_id=e.enrollment_id AND ma.ended_at IS NULL
                    JOIN mentors mentor ON mentor.mentor_id=ma.mentor_id
                    WHERE e.enrollment_id=mp.enrollment_id AND e.status='ACTIVE'
                    AND e.program_id=m.program_id AND iu.status='ACTIVE' AND ir.role_name='INTERN'
                    AND mentor.status='ACTIVE' AND mentor.user_id=?))
                """;
            default -> throw new SecurityException("Invalid role");
        };
        String sql = "SELECT m.meeting_id FROM meetings m WHERE ("+scope+")"
                +(program==null?"":" AND m.program_id=?")
                +(status==null?"":" AND m.status=?")
                +" ORDER BY m.starts_at,m.meeting_id LIMIT ? OFFSET ?";
        try (PreparedStatement s = c.prepareStatement(sql)) {
            int i=1;
            if (!Set.of("ADMIN","HR").contains(role)) s.setLong(i++,actor);
            if (program!=null) s.setInt(i++,program);
            if (status!=null) s.setString(i++,status);
            s.setInt(i++,size); s.setLong(i,(long)(page-1)*size);
            List<Long> ids=new ArrayList<>();
            try (ResultSet r=s.executeQuery()) { while(r.next()) ids.add(r.getLong(1)); }
            return ids;
        }
    }

    public boolean canRead(Connection c, long actor, String role, Meeting m) throws SQLException {
        if (Set.of("ADMIN","HR").contains(role)) return true;
        if ("MENTOR".equals(role)) {
            if(m.enrollmentIds().isEmpty()) return false;
            for (int enrollment : m.enrollmentIds())
                if (!lockParticipant(c,m.programId(),enrollment,actor)) return false;
            return true;
        }
        if (!"INTERN".equals(role)) return false;
        try (PreparedStatement s=c.prepareStatement("""
                SELECT 1 FROM meeting_participants mp
                JOIN program_enrollments e ON e.enrollment_id=mp.enrollment_id
                JOIN intern_profiles ip ON ip.profile_id=e.intern_profile_id
                WHERE mp.meeting_id=? AND ip.user_id=?
                """)) {
            s.setLong(1,m.id());s.setLong(2,actor);
            try(ResultSet r=s.executeQuery()) {return r.next();}
        }
    }

    public String lockActor(Connection c, long actor) throws SQLException {
        try (PreparedStatement s = c.prepareStatement("""
                SELECT r.role_name FROM users u JOIN roles r ON r.role_id=u.role_id
                WHERE u.user_id=? AND u.status='ACTIVE' FOR UPDATE
                """)) {
            s.setLong(1, actor);
            try (ResultSet r = s.executeQuery()) { return r.next() ? r.getString(1) : null; }
        }
    }

    public boolean lockProgram(Connection c, int program) throws SQLException {
        try (PreparedStatement s = c.prepareStatement("""
                SELECT status FROM internship_programs WHERE program_id=? FOR UPDATE
                """)) {
            s.setInt(1, program);
            try (ResultSet r = s.executeQuery()) {
                return r.next() && Set.of("PLANNED", "ACTIVE").contains(r.getString(1));
            }
        }
    }

    public boolean lockParticipant(Connection c, int program, int enrollment, Long mentorUser)
            throws SQLException {
        try (PreparedStatement s = c.prepareStatement("""
                SELECT e.enrollment_id FROM program_enrollments e
                JOIN intern_profiles p ON p.profile_id=e.intern_profile_id
                JOIN users u ON u.user_id=p.user_id JOIN roles r ON r.role_id=u.role_id
                WHERE e.enrollment_id=? AND e.program_id=? AND e.status='ACTIVE'
                AND u.status='ACTIVE' AND r.role_name='INTERN' FOR UPDATE
                """)) {
            s.setInt(1, enrollment); s.setInt(2, program);
            try (ResultSet r = s.executeQuery()) { if (!r.next()) return false; }
        }
        if (mentorUser == null) return true;
        try (PreparedStatement s = c.prepareStatement("""
                SELECT ma.assignment_id FROM mentor_assignments ma
                JOIN mentors m ON m.mentor_id=ma.mentor_id
                WHERE ma.enrollment_id=? AND ma.ended_at IS NULL
                AND m.user_id=? AND m.status='ACTIVE' FOR UPDATE
                """)) {
            s.setInt(1, enrollment); s.setLong(2, mentorUser);
            try (ResultSet r = s.executeQuery()) { return r.next(); }
        }
    }

    public long create(Connection c, long actor, MeetingRequest q) throws SQLException {
        try (PreparedStatement s = c.prepareStatement("""
                INSERT INTO meetings(program_id,organizer_user_id,title,description,location,starts_at,ends_at)
                VALUES(?,?,?,?,?,?,?)
                """, Statement.RETURN_GENERATED_KEYS)) {
            s.setInt(1, q.programId()); s.setLong(2, actor); fields(s, q, 3);
            s.executeUpdate();
            try (ResultSet r = s.getGeneratedKeys()) {
                if (!r.next()) throw new SQLException("Missing generated meeting ID");
                long id = r.getLong(1); participants(c, id, q.enrollmentIds()); return id;
            }
        }
    }

    public Meeting lockMeeting(Connection c, long id) throws SQLException {
        try (PreparedStatement s = c.prepareStatement("SELECT * FROM meetings WHERE meeting_id=? FOR UPDATE")) {
            s.setLong(1, id);
            try (ResultSet r = s.executeQuery()) {
                if (!r.next()) return null;
                List<Integer> ids = new ArrayList<>();
                try (PreparedStatement p = c.prepareStatement("""
                        SELECT enrollment_id FROM meeting_participants WHERE meeting_id=?
                        ORDER BY enrollment_id FOR UPDATE
                        """)) {
                    p.setLong(1, id);
                    try (ResultSet pr = p.executeQuery()) { while (pr.next()) ids.add(pr.getInt(1)); }
                }
                return new Meeting(id, r.getInt("program_id"), r.getLong("organizer_user_id"),
                        r.getString("title"), r.getString("description"), r.getString("location"),
                        r.getTimestamp("starts_at", utc()).toInstant(),
                        r.getTimestamp("ends_at", utc()).toInstant(), r.getString("status"),
                        r.getInt("revision"), ids);
            }
        }
    }

    public void update(Connection c, long id, MeetingRequest q) throws SQLException {
        try (PreparedStatement s = c.prepareStatement("""
                UPDATE meetings SET title=?,description=?,location=?,starts_at=?,ends_at=?,revision=revision+1
                WHERE meeting_id=? AND status='SCHEDULED'
                """)) {
            fields(s, q, 1); s.setLong(6, id);
            if (s.executeUpdate() != 1) throw new SQLException("Meeting update lost");
        }
        try (PreparedStatement s = c.prepareStatement("DELETE FROM meeting_participants WHERE meeting_id=?")) {
            s.setLong(1, id); s.executeUpdate();
        }
        participants(c, id, q.enrollmentIds());
    }

    public void cancel(Connection c, long id) throws SQLException {
        try (PreparedStatement s = c.prepareStatement("""
                UPDATE meetings SET status='CANCELLED',revision=revision+1
                WHERE meeting_id=? AND status='SCHEDULED'
                """)) {
            s.setLong(1, id);
            if (s.executeUpdate() != 1) throw new SQLException("Meeting cancellation lost");
        }
    }

    private void participants(Connection c, long id, List<Integer> ids) throws SQLException {
        try (PreparedStatement s = c.prepareStatement("INSERT INTO meeting_participants(meeting_id,enrollment_id) VALUES(?,?)")) {
            for (int enrollment : ids) { s.setLong(1, id); s.setInt(2, enrollment); s.addBatch(); }
            s.executeBatch();
        }
    }
    private void fields(PreparedStatement s, MeetingRequest q, int offset) throws SQLException {
        s.setString(offset, q.title()); s.setString(offset+1, q.description()); s.setString(offset+2, q.location());
        s.setTimestamp(offset+3, Timestamp.from(q.startsAt()), utc());
        s.setTimestamp(offset+4, Timestamp.from(q.endsAt()), utc());
    }
}
