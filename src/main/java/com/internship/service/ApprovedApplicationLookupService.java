package com.internship.service;

import com.internship.dao.ApprovedApplicationLookupDAO;
import com.internship.dao.ApprovedApplicationLookupDAO.Choice;
import com.internship.model.User;
import com.internship.model.UserStatus;
import com.internship.util.ConnectionProvider;
import com.internship.util.DBContext;
import com.internship.util.UserRoleChecks;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Objects;

public class ApprovedApplicationLookupService {
    private final ApprovedApplicationLookupDAO dao;
    private final ConnectionProvider connections;
    public ApprovedApplicationLookupService() { this(new ApprovedApplicationLookupDAO(),DBContext::getConnection); }
    public ApprovedApplicationLookupService(ApprovedApplicationLookupDAO dao,ConnectionProvider connections) {
        this.dao=Objects.requireNonNull(dao);this.connections=Objects.requireNonNull(connections);
    }
    public List<Choice> find(User actor,String email,Integer applicationId) throws SQLException {
        if(actor==null || actor.getUserId()<=0 || actor.getStatus()!=UserStatus.ACTIVE || !UserRoleChecks.canManageHr(actor))
            throw new ScheduleLeaveException(403,"FORBIDDEN","Chỉ HR/Admin được tìm đơn để ghi danh.");
        if(applicationId!=null) {
            if(applicationId<=0 || email!=null&&!email.isBlank())
                throw new IllegalArgumentException("Thông tin tìm đơn không hợp lệ.");
            email=null;
        } else {
            email=email==null?"":email.trim();
            if(email.length()>254 || !email.matches("[^\\s@]+@[^\\s@]+\\.[^\\s@]+"))
                throw new IllegalArgumentException("Nhập email đầy đủ hợp lệ để tìm đơn đã duyệt.");
        }
        try(Connection connection=connections.getConnection()) {return dao.find(email,applicationId,connection);}
    }
}
