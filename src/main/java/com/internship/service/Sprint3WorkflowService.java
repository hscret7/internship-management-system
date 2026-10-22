package com.internship.service;

import com.internship.dao.*;
import com.internship.model.*;
import com.internship.util.DBConnection;
import java.sql.*;
import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.*;

/** Sprint 3 mutations all lock enrollment before any child record or permission check. */
public class Sprint3WorkflowService {
    private final Be4ConnectionFactory connections;
    private final TaskAccessDAO access;
    private final Sprint3WorkflowDAO extra;
    private final TaskDAO tasks = new TaskDAO();
    private final TaskAssignmentDAO assignments = new TaskAssignmentDAO();
    private final TaskProgressDAO progress = new TaskProgressDAO();
    private final WeeklyReportDAO reports = new WeeklyReportDAO();
    public Sprint3WorkflowService() { this(DBConnection::getConnection,new TaskAccessDAO(),new Sprint3WorkflowDAO()); }
    public Sprint3WorkflowService(Be4ConnectionFactory connections, TaskAccessDAO access, Sprint3WorkflowDAO extra) { this.connections=connections;this.access=access;this.extra=extra; }
    public record CreatedTask(Task task, TaskAssignment assignment) { }
    public CreatedTask createAssignedTask(long actor, int enrollment, String title, String description, LocalDate due) throws SQLException {
        String cleanTitle=text(title,255,true), cleanDescription=text(description,10000,true);
        if(due==null)throw new IllegalArgumentException("Cần hạn hoàn thành task.");
        return write(enrollment,c->{
            requireMentor(c,actor,enrollment);var dates=requireActive(c,enrollment);
            if(due.isBefore(dates.start())||due.isAfter(dates.end()))throw new IllegalArgumentException("Hạn task phải nằm trong thời gian chương trình.");
            Task t=new Task();t.setTitle(cleanTitle);t.setDescription(cleanDescription);t.setDueDate(due);t.setCreatedByUserId(actor);t.setStatus(TaskStatus.OPEN);
            t=tasks.create(c,t);var assignment=assignments.create(c,t.getTaskId(),enrollment);return new CreatedTask(t,assignment);
        });
    }
    public TaskProgress updateProgress(long actor, long assignmentId, int percentage, String note) throws SQLException {
        if(assignmentId<=0||percentage<0||percentage>100)throw new IllegalArgumentException("Mã task/tiến độ không hợp lệ.");
        String clean=text(note,10000,false);
        try(var c=connections.open()) {
            var linked=assignments.findById(c,assignmentId);
            if(linked==null)throw new IllegalArgumentException("Không tìm thấy task được giao.");
            c.setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);c.setAutoCommit(false);
            try {
                access.lockEnrollmentForMentorWrite(c,linked.getEnrollmentId());requireIntern(c,actor,linked.getEnrollmentId());requireActive(c,linked.getEnrollmentId());
                var a=assignments.findByIdForUpdate(c,assignmentId);var t=tasks.findById(c,a.getTaskId());
                if(t==null||t.getStatus()!=TaskStatus.OPEN||a.getStatus()==TaskAssignmentStatus.DONE)throw new IllegalStateException("Task không còn cho cập nhật.");
                Integer previous=progress.findLatestPercentage(c,assignmentId);
                if(previous!=null&&percentage<previous)throw new IllegalArgumentException("Không được giảm tiến độ đã ghi.");
                var result=progress.append(c,assignmentId,percentage,clean);extra.progressActor(c,result.getProgressId(),actor);
                assignments.updateStatus(c,assignmentId,TaskService.statusFor(percentage));c.commit();return result;
            }catch(SQLException|RuntimeException e){rollback(c,e);throw e;}
        }
    }
    public TaskAssignment assignExistingTask(long actor,long taskId,int enrollment)throws SQLException {
        if(taskId<=0)throw new IllegalArgumentException("Mã task không hợp lệ.");
        return write(enrollment,c->{requireMentor(c,actor,enrollment);var dates=requireActive(c,enrollment);var t=tasks.findById(c,taskId);
            if(t==null||t.getCreatedByUserId()!=actor||t.getStatus()!=TaskStatus.OPEN)throw new IllegalArgumentException("Task không thuộc mentor hoặc không còn mở.");
            text(t.getTitle(),255,true);text(t.getDescription(),10000,true);
            if(t.getDueDate()==null||t.getDueDate().isBefore(dates.start())||t.getDueDate().isAfter(dates.end()))throw new IllegalArgumentException("Hạn task phải nằm trong thời gian chương trình.");
            try{return assignments.create(c,taskId,enrollment);}catch(SQLIntegrityConstraintViolationException e){if(e.getErrorCode()==1062)throw new IllegalStateException("Task đã được giao.",e);throw e;}
        });
    }
    public List<Map<String,Object>> meetingParticipants(long actor)throws SQLException {
        try(var c=connections.open()) {
            if(actor<=0||!access.isActiveMentor(c,actor))throw new SecurityException("Chỉ mentor ACTIVE được xem TTS phụ trách.");
            String sql="""
                SELECT pe.enrollment_id,pe.program_id,ip.full_name,p.program_name
                FROM mentor_assignments ma JOIN mentors m ON m.mentor_id=ma.mentor_id
                JOIN program_enrollments pe ON pe.enrollment_id=ma.enrollment_id
                JOIN internship_programs p ON p.program_id=pe.program_id
                JOIN intern_profiles ip ON ip.profile_id=pe.intern_profile_id
                JOIN users u ON u.user_id=ip.user_id JOIN roles r ON r.role_id=u.role_id
                WHERE m.user_id=? AND ma.ended_at IS NULL AND pe.status='ACTIVE' AND p.status='ACTIVE'
                  AND ip.status='ACTIVE' AND u.status='ACTIVE' AND r.role_name='INTERN'
                ORDER BY pe.program_id,pe.enrollment_id
                """;
            try(var s=c.prepareStatement(sql)){s.setLong(1,actor);List<Map<String,Object>> rows=new ArrayList<>();try(var r=s.executeQuery()){while(r.next())rows.add(Map.of("enrollmentId",r.getInt(1),"programId",r.getInt(2),"internName",r.getString(3),"programName",r.getString(4)));}return rows;}
        }
    }
    public WeeklyReport createDraft(long actor,int enrollment,short week,String results,String difficulties,String plan) throws SQLException {
        var sections=sections(results,difficulties,plan,false);
        return write(enrollment,c->{
            requireIntern(c,actor,enrollment);var dates=requireActive(c,enrollment);validateWeek(week,dates.start(),dates.end());
            try {
                var report=reports.createDraft(c,enrollment,week,sections.content());extra.sections(c,report.getReportId(),sections);return report;
            }catch(SQLIntegrityConstraintViolationException e){if(e.getErrorCode()==1062)throw new IllegalStateException("Tuần này đã có báo cáo.",e);throw e;}
        });
    }
    public WeeklyReport updateDraft(long actor,long reportId,String results,String difficulties,String plan) throws SQLException {
        var sections=sections(results,difficulties,plan,false);
        return reportWrite(actor,reportId,(c,r)->{ if(r.getStatus()!=WeeklyReportStatus.DRAFT)throw new IllegalStateException("Chỉ được sửa báo cáo nháp.");reports.updateDraft(c,reportId,sections.content());extra.sections(c,reportId,sections);return reports.findById(c,reportId); });
    }
    public WeeklyReport submit(long actor,long reportId) throws SQLException {
        return reportWrite(actor,reportId,(c,r)->{
            if(r.getStatus()!=WeeklyReportStatus.DRAFT)throw new IllegalStateException("Báo cáo đã nộp.");
            var sections=extra.sections(c,reportId);
            if(sections==null)text(r.getContent(),32000,true); // Readable legacy report; never invent sections.
            else sections(sections.results(),sections.difficulties(),sections.nextPlan(),true);
            reports.markSubmitted(c,reportId);return reports.findById(c,reportId);
        });
    }
    private WeeklyReport reportWrite(long actor,long id,ReportWork work)throws SQLException {
        if(id<=0)throw new IllegalArgumentException("Mã báo cáo không hợp lệ.");
        int enrollment;try(var c=connections.open()){var linked=reports.findById(c,id);if(linked==null)throw new IllegalArgumentException("Không tìm thấy báo cáo.");enrollment=linked.getEnrollmentId();}
        return write(enrollment,c->{requireIntern(c,actor,enrollment);requireActive(c,enrollment);var r=reports.findByIdForUpdate(c,id);return work.run(c,r);});
    }
    public Sprint3WorkflowDAO.Assessment assess(long actor,int enrollment,int skills,int attitude,String comment)throws SQLException {
        if(skills<1||skills>5||attitude<1||attitude>5)throw new IllegalArgumentException("Mỗi tiêu chí phải là số nguyên từ 1 đến 5.");
        String clean=text(comment,10000,true);
        return write(enrollment,c->{Integer mentor=access.findCurrentMentorId(c,actor,enrollment);if(mentor==null)throw new SecurityException("Chỉ mentor hiện tại được đánh giá.");requireActive(c,enrollment);return extra.createAssessment(c,enrollment,mentor,skills,attitude,clean);});
    }
    public List<Sprint3WorkflowDAO.Assessment> assessments(long actor,int enrollment)throws SQLException {
        if(enrollment<=0)throw new IllegalArgumentException("Mã ghi danh không hợp lệ.");
        try(var c=connections.open()){if(!access.isInternOwner(c,actor,enrollment)&&!access.isAssignedMentor(c,actor,enrollment)&&!access.isActiveHrOrAdmin(c,actor))throw new SecurityException("Không có quyền xem đánh giá.");return extra.history(c,enrollment);}
    }
    /** Enrichment is only called after the existing read service has checked authorization. */
    public Map<Long,Sprint3WorkflowDAO.Sections> reportSections(List<WeeklyReport> rows)throws SQLException {
        Map<Long,Sprint3WorkflowDAO.Sections> values=new HashMap<>();try(var c=connections.open()){for(var r:rows)values.put(r.getReportId(),extra.sections(c,r.getReportId()));}return values;
    }
    public Map<Long,Long> progressActors(List<TaskProgress> rows)throws SQLException {
        Map<Long,Long> values=new HashMap<>();try(var c=connections.open()){for(var r:rows)values.put(r.getProgressId(),extra.progressActor(c,r.getProgressId()));}return values;
    }
    private void requireMentor(Connection c,long actor,int enrollment)throws SQLException { if(actor<=0||!access.isCurrentMentor(c,actor,enrollment))throw new SecurityException("Mentor không phụ trách đợt này."); }
    private void requireIntern(Connection c,long actor,int enrollment)throws SQLException { if(actor<=0||!access.isActiveInternOwner(c,actor,enrollment))throw new SecurityException("TTS không sở hữu đợt ACTIVE này."); }
    private Sprint3WorkflowDAO.Dates requireActive(Connection c,int enrollment)throws SQLException {var d=extra.dates(c,enrollment);if(d==null||!"ACTIVE".equals(d.enrollmentStatus())||!"ACTIVE".equals(d.programStatus()))throw new IllegalStateException("Đợt/chương trình đã kết thúc, chỉ được xem lịch sử.");return d;}
    public static void validateWeek(int week,LocalDate start,LocalDate end) {if(start==null||end==null||end.isBefore(start)||week<1||week>(ChronoUnit.DAYS.between(start,end)/7+1))throw new IllegalArgumentException("Tuần phải nằm trong thời gian chương trình, tuần 1 bắt đầu ngày khai giảng.");}
    public static String text(String value,int max,boolean required) {String clean=value==null?"":value.strip();if(clean.length()>max||(required&&clean.isEmpty()))throw new IllegalArgumentException("Nội dung không hợp lệ hoặc vượt quá "+max+" ký tự.");return clean;}
    private static Sprint3WorkflowDAO.Sections sections(String results,String difficulties,String plan,boolean required) {return new Sprint3WorkflowDAO.Sections(text(results,10000,required),text(difficulties,10000,required),text(plan,10000,required));}
    private <T>T write(int enrollment,Work<T> work)throws SQLException {
        if(enrollment<=0)throw new IllegalArgumentException("Mã ghi danh không hợp lệ.");
        try(var c=connections.open()){c.setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);c.setAutoCommit(false);try{access.lockEnrollmentForMentorWrite(c,enrollment);T result=work.run(c);c.commit();return result;}catch(SQLException|RuntimeException e){rollback(c,e);throw e;}}
    }
    private static void rollback(Connection c,Exception e){try{c.rollback();}catch(SQLException f){e.addSuppressed(f);}}
    @FunctionalInterface private interface Work<T>{T run(Connection c)throws SQLException;}
    @FunctionalInterface private interface ReportWork{WeeklyReport run(Connection c,WeeklyReport r)throws SQLException;}
}
