package com.internship.service;

import com.internship.dao.*;
import com.internship.util.DBConnection;
import java.sql.*;
import java.time.*;
import java.math.BigDecimal;
import java.util.*;

/** Snapshot from actual data, one enrollment per row. No automatic result/grade is invented. */
public class EndTermReportService {
    public record Filter(Integer programId,Integer enrollmentId,String status) {
        public Filter { if(programId!=null&&programId<=0||enrollmentId!=null&&enrollmentId<=0||status!=null&&!Set.of("ACTIVE","COMPLETED","CANCELLED").contains(status))throw new IllegalArgumentException("Bộ lọc báo cáo không hợp lệ."); }
    }
    public record Row(int enrollmentId,int programId,int internProfileId,String internName,String university,String major,String programName,String status,LocalDate startDate,LocalDate endDate,String mentorName,
                      long taskCount,long tasksDone,long submittedReports,long reviewedReports,List<Sprint3WorkflowDAO.Assessment> assessments,BigDecimal legacyScore) { }
    public record Page(List<Row> items,int page,int size,boolean hasMore,Instant generatedAt,Filter filter) { }
    private final Be4ConnectionFactory connections;
    private final TaskAccessDAO access=new TaskAccessDAO();
    private final Sprint3WorkflowDAO extra=new Sprint3WorkflowDAO();
    public EndTermReportService(){this(DBConnection::getConnection);}
    public EndTermReportService(Be4ConnectionFactory connections){this.connections=connections;}
    public Page list(long actor,Filter filter,int page,int size)throws SQLException {
        if(page<1||page>100000||size<1||size>100)throw new IllegalArgumentException("Phân trang không hợp lệ.");
        try(var c=connections.open()){authorize(c,actor);c.setReadOnly(true);c.setTransactionIsolation(Connection.TRANSACTION_REPEATABLE_READ);c.setAutoCommit(false);
            try{var rows=query(c,filter,size+1,(page-1)*size);boolean more=rows.size()>size;var result=new Page(List.copyOf(rows.subList(0,Math.min(size,rows.size()))),page,size,more,Instant.now(),filter);c.commit();return result;}catch(SQLException|RuntimeException e){c.rollback();throw e;}}
    }
    public Page exportSnapshot(long actor,Filter filter)throws SQLException {
        try(var c=connections.open()){authorize(c,actor);c.setTransactionIsolation(Connection.TRANSACTION_REPEATABLE_READ);c.setAutoCommit(false);
            try{var rows=query(c,filter,1001,0);if(rows.size()>1000)throw new IllegalStateException("Báo cáo vượt 1000 ghi danh; cần chọn chương trình/trạng thái hoặc từng TTS, không xuất thiếu dữ liệu.");var result=new Page(List.copyOf(rows),1,rows.size(),false,Instant.now(),filter);c.commit();return result;}catch(SQLException|RuntimeException e){c.rollback();throw e;}}
    }
    public void recordExport(long actor,Page snapshot,String format)throws SQLException {
        try(var c=connections.open()){authorize(c,actor);try(var s=c.prepareStatement("INSERT INTO report_export_events(actor_user_id,program_id,enrollment_id,enrollment_status,format,row_count) VALUES(?,?,?,?,?,?)")){s.setLong(1,actor);s.setObject(2,snapshot.filter().programId());s.setObject(3,snapshot.filter().enrollmentId());s.setString(4,snapshot.filter().status());s.setString(5,format);s.setInt(6,snapshot.items().size());s.executeUpdate();}}
    }
    private void authorize(Connection c,long actor)throws SQLException{if(actor<=0||!access.isActiveHrOrAdmin(c,actor))throw new SecurityException("Chỉ HR/Admin được xem/xuất báo cáo cuối kỳ.");}
    private List<Row> query(Connection c,Filter f,int limit,int offset)throws SQLException {
        String sql="""
            SELECT pe.enrollment_id,pe.program_id,ip.profile_id,ip.full_name,ip.university,ip.major,p.program_name,pe.status,p.start_date,p.end_date,u.full_name AS mentor_name,
                COALESCE(t.total,0) AS task_count,COALESCE(t.done,0) AS tasks_done,
                COALESCE(w.submitted,0) AS submitted_reports,COALESCE(w.reviewed,0) AS reviewed_reports,e.score
            FROM program_enrollments pe
            JOIN intern_profiles ip ON ip.profile_id=pe.intern_profile_id
            JOIN internship_programs p ON p.program_id=pe.program_id
            LEFT JOIN mentor_assignments ma ON ma.enrollment_id=pe.enrollment_id AND ma.assignment_id=(SELECT MAX(last_ma.assignment_id) FROM mentor_assignments last_ma WHERE last_ma.enrollment_id=pe.enrollment_id)
            LEFT JOIN mentors m ON m.mentor_id=ma.mentor_id LEFT JOIN users u ON u.user_id=m.user_id
            LEFT JOIN (SELECT enrollment_id,COUNT(*) AS total,SUM(status='DONE') AS done FROM task_assignments GROUP BY enrollment_id) t ON t.enrollment_id=pe.enrollment_id
            LEFT JOIN (SELECT enrollment_id,SUM(status IN ('SUBMITTED','REVIEWED')) AS submitted,SUM(status='REVIEWED') AS reviewed FROM weekly_reports GROUP BY enrollment_id) w ON w.enrollment_id=pe.enrollment_id
            LEFT JOIN evaluations e ON e.enrollment_id=pe.enrollment_id AND e.score IS NOT NULL
            WHERE (? IS NULL OR pe.program_id=?) AND (? IS NULL OR pe.enrollment_id=?) AND (? IS NULL OR pe.status=?)
            ORDER BY pe.enrollment_id DESC LIMIT ? OFFSET ?
            """;
        List<Row> rows=new ArrayList<>();
        try(var s=c.prepareStatement(sql)){s.setObject(1,f.programId());s.setObject(2,f.programId());s.setObject(3,f.enrollmentId());s.setObject(4,f.enrollmentId());s.setString(5,f.status());s.setString(6,f.status());s.setInt(7,limit);s.setInt(8,offset);
            try(var r=s.executeQuery()){while(r.next())rows.add(new Row(r.getInt("enrollment_id"),r.getInt("program_id"),r.getInt("profile_id"),r.getString("full_name"),r.getString("university"),r.getString("major"),r.getString("program_name"),r.getString("status"),r.getDate("start_date").toLocalDate(),r.getDate("end_date").toLocalDate(),r.getString("mentor_name"),r.getLong("task_count"),r.getLong("tasks_done"),r.getLong("submitted_reports"),r.getLong("reviewed_reports"),List.of(),r.getBigDecimal("score")));}}
        List<Row> enriched=new ArrayList<>();for(var r:rows)enriched.add(new Row(r.enrollmentId(),r.programId(),r.internProfileId(),r.internName(),r.university(),r.major(),r.programName(),r.status(),r.startDate(),r.endDate(),r.mentorName(),r.taskCount(),r.tasksDone(),r.submittedReports(),r.reviewedReports(),extra.history(c,r.enrollmentId()),r.legacyScore()));return enriched;
    }
}
