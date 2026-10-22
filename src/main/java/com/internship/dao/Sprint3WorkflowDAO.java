package com.internship.dao;

import java.sql.*;
import java.time.*;
import java.util.*;

/** Criteria history in evaluations, report sections and progress author evidence. */
public class Sprint3WorkflowDAO {
    public record Dates(LocalDate start, LocalDate end, String enrollmentStatus, String programStatus) { }
    public record Sections(String results, String difficulties, String nextPlan) {
        public String content() { return "KẾT QUẢ\n"+results+"\n\nKHÓ KHĂN\n"+difficulties+"\n\nKẾ HOẠCH TIẾP THEO\n"+nextPlan; }
    }
    public record Assessment(long assessmentId, int enrollmentId, int mentorId, String mentorName,
                             int skillsScore, int attitudeScore, String comment, Instant createdAt) { }
    public Dates dates(Connection c, int enrollment) throws SQLException {
        try(var s=c.prepareStatement("SELECT p.start_date,p.end_date,pe.status,p.status FROM program_enrollments pe JOIN internship_programs p ON p.program_id=pe.program_id WHERE pe.enrollment_id=?")) {
            s.setInt(1,enrollment);try(var r=s.executeQuery()) { return r.next()?new Dates(r.getDate(1).toLocalDate(),r.getDate(2).toLocalDate(),r.getString(3),r.getString(4)):null; }
        }
    }
    public void sections(Connection c, long report, Sections v) throws SQLException {
        try(var s=c.prepareStatement("INSERT INTO weekly_report_sections(report_id,results,difficulties,next_plan) VALUES(?,?,?,?) ON DUPLICATE KEY UPDATE results=?,difficulties=?,next_plan=?")) {
            s.setLong(1,report);s.setString(2,v.results());s.setString(3,v.difficulties());s.setString(4,v.nextPlan());s.setString(5,v.results());s.setString(6,v.difficulties());s.setString(7,v.nextPlan());s.executeUpdate();
        }
    }
    public Sections sections(Connection c, long report) throws SQLException {
        try(var s=c.prepareStatement("SELECT results,difficulties,next_plan FROM weekly_report_sections WHERE report_id=?")) {
            s.setLong(1,report);try(var r=s.executeQuery()) { return r.next()?new Sections(r.getString(1),r.getString(2),r.getString(3)):null; }
        }
    }
    public void progressActor(Connection c, long progress, long actor) throws SQLException {
        try(var s=c.prepareStatement("INSERT INTO task_progress_actors(progress_id,actor_user_id) VALUES(?,?)")) { s.setLong(1,progress);s.setLong(2,actor);s.executeUpdate(); }
    }
    public Long progressActor(Connection c, long progress) throws SQLException {
        try(var s=c.prepareStatement("SELECT actor_user_id FROM task_progress_actors WHERE progress_id=?")) { s.setLong(1,progress);try(var r=s.executeQuery()) { return r.next()?r.getLong(1):null; } }
    }
    public Assessment createAssessment(Connection c, int enrollment, int mentor, int skills, int attitude, String comment) throws SQLException {
        long id;
        try(var s=c.prepareStatement("INSERT INTO evaluations(enrollment_id,mentor_id,skill_score,attitude_score,comment) VALUES(?,?,?,?,?)",Statement.RETURN_GENERATED_KEYS)) {
            s.setInt(1,enrollment);s.setInt(2,mentor);s.setInt(3,skills);s.setInt(4,attitude);s.setString(5,comment);s.executeUpdate();try(var r=s.getGeneratedKeys()) { if(!r.next())throw new SQLException("Missing assessment ID");id=r.getLong(1); }
        }
        return history(c,enrollment).stream().filter(a->a.assessmentId()==id).findFirst().orElseThrow(()->new SQLException("Assessment was not persisted"));
    }
    public List<Assessment> history(Connection c, int enrollment) throws SQLException {
        try(var s=c.prepareStatement("SELECT a.*,UNIX_TIMESTAMP(a.created_at) AS created_at_epoch,u.full_name AS mentor_name FROM evaluations a JOIN mentors m ON m.mentor_id=a.mentor_id JOIN users u ON u.user_id=m.user_id WHERE a.enrollment_id=? AND a.score IS NULL ORDER BY a.evaluation_id DESC")) {
            s.setInt(1,enrollment);List<Assessment> rows=new ArrayList<>();try(var r=s.executeQuery()) { while(r.next())rows.add(new Assessment(r.getLong("evaluation_id"),r.getInt("enrollment_id"),r.getInt("mentor_id"),r.getString("mentor_name"),r.getInt("skill_score"),r.getInt("attitude_score"),r.getString("comment"),com.internship.util.SqlTime.instant(r,"created_at"))); }return rows;
        }
    }
}
