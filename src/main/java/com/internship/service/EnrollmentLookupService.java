package com.internship.service;

import com.internship.dao.ScheduleEnrollmentDAO;
import com.internship.dao.ScheduleEnrollmentDAO.Choice;
import com.internship.model.User;
import com.internship.model.UserStatus;
import com.internship.util.ConnectionProvider;
import com.internship.util.DBContext;
import com.internship.util.UserRoleChecks;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Objects;

/** Lookup only; mutations continue using numeric enrollment IDs and their existing authorization. */
public class EnrollmentLookupService {
    private final ScheduleEnrollmentDAO dao;
    private final ConnectionProvider connections;
    public EnrollmentLookupService() { this(new ScheduleEnrollmentDAO(), DBContext::getConnection); }
    public EnrollmentLookupService(ScheduleEnrollmentDAO dao, ConnectionProvider connections) {
        this.dao = Objects.requireNonNull(dao); this.connections = Objects.requireNonNull(connections);
    }
    public List<Choice> lookup(User actor, String email) throws SQLException {
        if (actor == null || actor.getUserId() <= 0 || actor.getStatus() != UserStatus.ACTIVE)
            throw new ScheduleLeaveException(403, "FORBIDDEN", "Không có quyền truy cập.");
        Long owner;
        if (UserRoleChecks.isIntern(actor)) {
            if (email != null && !email.isBlank())
                throw new ScheduleLeaveException(403, "FORBIDDEN", "TTS chỉ được xem ghi danh của mình.");
            owner = actor.getUserId(); email = null;
        } else if (UserRoleChecks.canManageHr(actor)) {
            owner = null;
            email = email == null ? "" : email.trim();
            if (email.length() > 254 || !email.matches("[^\\s@]+@[^\\s@]+\\.[^\\s@]+"))
                throw new ScheduleLeaveException(400, "VALIDATION_ERROR", "Nhập email đầy đủ hợp lệ để tìm ghi danh.");
        } else throw new ScheduleLeaveException(403, "FORBIDDEN", "Không có quyền truy cập.");
        try (Connection c = connections.getConnection()) { return dao.choices(owner, email, c); }
    }

    /** HR-only metadata for the email cards; Intern routes never receive this directory. */
    public List<ScheduleEnrollmentDAO.InternDetail> internDirectory(User actor) throws SQLException {
        if (actor == null || actor.getStatus()!=UserStatus.ACTIVE || !UserRoleChecks.canManageHr(actor))
            throw new ScheduleLeaveException(403,"FORBIDDEN","Chỉ HR/Admin được xem danh sách TTS.");
        try(Connection c=connections.getConnection()) {return dao.listEnrolledInterns(c);}
    }

    public int activeByEmail(User actor, String email, Integer programId) throws SQLException {
        var matches=lookup(actor,email).stream()
                .filter(c -> "ACTIVE".equals(c.enrollmentStatus()) && "ACTIVE".equals(c.programStatus()))
                .filter(c -> programId==null || c.programId()==programId).toList();
        if(matches.size()!=1) throw new ScheduleLeaveException(400,"VALIDATION_ERROR",
                matches.isEmpty()?"Email chưa có ghi danh đang hoạt động phù hợp.":"Email có nhiều ghi danh; hãy chọn đúng chương trình.");
        return matches.get(0).enrollmentId();
    }
}
