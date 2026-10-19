package com.internship.service;

import com.internship.dao.EmailSuggestionDAO;
import com.internship.dao.EmailSuggestionDAO.Scope;
import com.internship.dao.EmailSuggestionDAO.Suggestion;
import com.internship.model.*;
import com.internship.util.*;
import java.sql.*;
import java.util.*;

public class EmailSuggestionService {
    private final EmailSuggestionDAO dao;
    private final ConnectionProvider connections;
    public EmailSuggestionService() { this(new EmailSuggestionDAO(),DBContext::getConnection); }
    public EmailSuggestionService(EmailSuggestionDAO dao,ConnectionProvider connections) {
        this.dao=Objects.requireNonNull(dao);this.connections=Objects.requireNonNull(connections);
    }
    public List<Suggestion> search(User actor,Scope scope,String query) throws SQLException {
        if(actor==null || actor.getStatus()!=UserStatus.ACTIVE || actor.getRole()==null
                || (scope==Scope.ACTOR ? !"ADMIN".equals(actor.getRole().getRoleName()) : !UserRoleChecks.canManageHr(actor)))
            throw new SecurityException("Không có quyền tra cứu email.");
        if(scope==null)throw new IllegalArgumentException("Thiếu phạm vi tra cứu.");
        query=query==null?"":query.trim();
        if(query.length()>254)throw new IllegalArgumentException("Chuỗi tìm kiếm quá dài.");
        if(query.length()<2)return List.of();
        try(Connection connection=connections.getConnection()) { return dao.search(connection,scope,query); }
    }
}
