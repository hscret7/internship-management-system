package com.internship.service;

import com.internship.dao.ProgramStatisticsDAO;
import com.internship.dao.TaskAccessDAO;
import com.internship.util.DBConnection;
import java.sql.*;
import java.time.Instant;

/** US34 adapter reuses the exact US32/33 aggregator instead of inventing export counts. */
public class ProgramStatisticsReportService {
    public record Snapshot(ProgramStatisticsDAO.Filter filter,ProgramStatisticsDAO.Summary summary,Instant generatedAt) { }
    private final Be4ConnectionFactory connections;
    public ProgramStatisticsReportService(){this(DBConnection::getConnection);}
    public ProgramStatisticsReportService(Be4ConnectionFactory connections){this.connections=connections;}
    private void authorize(Connection c,long actor)throws SQLException {
        if(actor<=0||!new TaskAccessDAO().isActiveHrOrAdmin(c,actor))throw new SecurityException("Chỉ HR/Admin được xuất thống kê.");
    }
    public Snapshot snapshot(long actor,ProgramStatisticsDAO.Filter filter)throws SQLException {
        try(var c=connections.open()) {
            authorize(c,actor);c.setReadOnly(true);c.setTransactionIsolation(Connection.TRANSACTION_REPEATABLE_READ);c.setAutoCommit(false);
            try {var summary=new ProgramStatisticsDAO().summarize(c,filter);
                if(summary.universities().size()+summary.majors().size()>2000)throw new IllegalStateException("Quá 2000 nhóm thống kê, hãy thu hẹp bộ lọc.");
                c.commit();return new Snapshot(filter,summary,Instant.now());
            }catch(SQLException|RuntimeException e){c.rollback();throw e;}
        }
    }
    public void recordExport(long actor,Snapshot snapshot,String format)throws SQLException {
        try(var c=connections.open()){authorize(c,actor);
            try(var s=c.prepareStatement("INSERT INTO report_export_events(actor_user_id,program_id,enrollment_status,format,report_kind,cohort_from,cohort_to,row_count) VALUES(?,?,?,?,'PROGRAM_STATISTICS',?,?,?)")) {
                s.setLong(1,actor);s.setObject(2,snapshot.filter().programId());s.setString(3,snapshot.filter().status());s.setString(4,format);s.setObject(5,snapshot.filter().cohortFrom());s.setObject(6,snapshot.filter().cohortTo());s.setLong(7,snapshot.summary().enrollments());s.executeUpdate();
            }
        }
    }
}
