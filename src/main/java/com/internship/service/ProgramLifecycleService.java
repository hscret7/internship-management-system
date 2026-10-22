package com.internship.service;

import com.internship.dao.ProgramLifecycleDAO;
import com.internship.dao.TaskAccessDAO;
import com.internship.util.ConnectionProvider;
import com.internship.util.DBContext;
import java.sql.*;
import java.util.Set;

/** Explicit HR decisions, never automatic completion from dates or evaluation scores. */
public class ProgramLifecycleService {
    private final ProgramLifecycleDAO dao;
    private final TaskAccessDAO access;
    private final ConnectionProvider connections;
    public ProgramLifecycleService() { this(new ProgramLifecycleDAO(), new TaskAccessDAO(), DBContext::getConnection); }
    public ProgramLifecycleService(ProgramLifecycleDAO dao, TaskAccessDAO access, ConnectionProvider connections) {
        this.dao = dao; this.access = access; this.connections = connections;
    }
    public int closeProgram(long actor, int programId, String target, String reason) throws SQLException {
        validate(programId, target, reason);
        return transaction(actor, c -> {
            String from = dao.lockProgram(c, programId);
            if (from == null) throw new IllegalArgumentException("Không tìm thấy chương trình.");
            if (!("ACTIVE".equals(from) || ("PLANNED".equals(from) && "CANCELLED".equals(target))))
                throw new IllegalStateException("Trạng thái chương trình không cho phép thao tác này.");
            var enrollments = dao.lockActiveEnrollments(c, programId);
            for (var e : enrollments) dao.closeEnrollment(c, e, target, actor, reason.strip());
            dao.closeProgram(c, programId, from, target, actor, reason.strip());
            return enrollments.size();
        });
    }
    public void closeEnrollment(long actor, int enrollmentId, String target, String reason) throws SQLException {
        validate(enrollmentId, target, reason);
        transaction(actor, c -> {
            var linked = dao.enrollment(c, enrollmentId, false);
            if (linked == null) throw new IllegalArgumentException("Không tìm thấy ghi danh.");
            String program = dao.lockProgram(c, linked.programId());
            var e = dao.enrollment(c, enrollmentId, true);
            if (!"ACTIVE".equals(program) || e == null || !"ACTIVE".equals(e.status()))
                throw new IllegalStateException("Chỉ ghi danh thuộc chương trình ACTIVE được kết thúc.");
            dao.closeEnrollment(c, e, target, actor, reason.strip());
            return 1;
        });
    }
    private static void validate(int id, String target, String reason) {
        if (id <= 0 || target == null || !Set.of("COMPLETED", "CANCELLED").contains(target))
            throw new IllegalArgumentException("Mã hoặc trạng thái kết thúc không hợp lệ.");
        if (reason == null || reason.isBlank() || reason.strip().length() > 1000)
            throw new IllegalArgumentException("Cần lý do/xác nhận kết thúc từ 1 đến 1000 ký tự.");
    }
    private int transaction(long actor, Work work) throws SQLException {
        try (var c = connections.getConnection()) {
            c.setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED); c.setAutoCommit(false);
            try {
                if (actor <= 0 || !access.isActiveHrOrAdmin(c, actor)) throw new SecurityException("Chỉ HR/Admin được kết thúc chương trình.");
                int result = work.run(c); c.commit(); return result;
            } catch (SQLException | RuntimeException e) {
                try { c.rollback(); } catch (SQLException failure) { e.addSuppressed(failure); }
                throw e;
            }
        }
    }
    @FunctionalInterface private interface Work { int run(Connection c) throws SQLException; }
}
