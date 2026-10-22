package com.internship.controller;

import com.internship.dao.EmailSuggestionDAO.Scope;
import com.internship.model.User;
import com.internship.service.EmailSuggestionService;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.sql.SQLException;

@WebServlet({"/api/hr/email-suggestions","/admin/email-suggestions"})
public class EmailSuggestionServlet extends HttpServlet {
    private final EmailSuggestionService service;
    public EmailSuggestionServlet(){this(new EmailSuggestionService());}
    EmailSuggestionServlet(EmailSuggestionService service){this.service=service;}
    @Override protected void doGet(HttpServletRequest request,HttpServletResponse response)throws IOException {
        response.setContentType("application/json;charset=UTF-8");response.setHeader("Cache-Control","no-store");
        HttpSession session=request.getSession(false);
        if(session==null || !(session.getAttribute("currentUser") instanceof User)) {error(response,401,"Vui lòng đăng nhập.");return;}
        User actor=(User)session.getAttribute("currentUser");
        boolean adminRoute="/admin/email-suggestions".equals(request.getServletPath());
        if(actor.getStatus()!=com.internship.model.UserStatus.ACTIVE || actor.getRole()==null
                || (adminRoute ? !"ADMIN".equals(actor.getRole().getRoleName()) : !com.internship.util.UserRoleChecks.canManageHr(actor))) {
            error(response,403,"Không có quyền tra cứu email.");return;
        }
        try {
            Scope scope;
            if("/admin/email-suggestions".equals(request.getServletPath()))scope=Scope.ACTOR;
            else scope=switch(String.valueOf(request.getParameter("scope"))) {
                case "application" -> Scope.APPLICATION;
                case "enrollment","null" -> Scope.ENROLLMENT;
                default -> throw new IllegalArgumentException("Phạm vi tra cứu không hợp lệ.");
            };
            var data=service.search(actor,scope,request.getParameter("q")).stream()
                    .map(item->Be4Json.fields("email",item.email(),"fullName",item.fullName())).toList();
            response.getWriter().write(Be4Json.stringify(Be4Json.fields("success",true,"data",data)));
        }catch(SecurityException e){error(response,403,"Không có quyền tra cứu email.");}
        catch(IllegalArgumentException e){error(response,400,"Thông tin tìm kiếm không hợp lệ.");}
        catch(SQLException e){log("Email suggestions unavailable",e);error(response,503,"Chưa thể tải gợi ý email.");}
    }
    private static void error(HttpServletResponse response,int status,String message)throws IOException {
        response.setStatus(status);response.getWriter().write(Be4Json.stringify(Be4Json.fields("success",false,"error",message)));
    }
}
