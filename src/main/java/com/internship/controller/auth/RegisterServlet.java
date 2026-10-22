package com.internship.controller.auth;

import java.io.IOException;

import com.internship.dao.RoleDAO;
import com.internship.dao.UserDAO;
import com.internship.service.UserService;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@WebServlet("/auth/register")
public class RegisterServlet extends HttpServlet {

    private UserService userService;

    public RegisterServlet() {
    }

    public RegisterServlet(UserService userService) {
        this.userService = userService;
    }

    @Override
    public void init() {
        if (userService == null) {
            userService = new UserService(
                    new UserDAO(),
                    new RoleDAO()
            );
        }
    }

    @Override
    protected void doGet(
            HttpServletRequest request,
            HttpServletResponse response
    ) throws ServletException, IOException {

        request.getRequestDispatcher("/register.jsp")
                .forward(request, response);
    }

    @Override
    protected void doPost(
            HttpServletRequest request,
            HttpServletResponse response
    ) throws ServletException, IOException {

        request.setCharacterEncoding("UTF-8");

        String username = request.getParameter("username");
        String email = request.getParameter("email");
        String fullName = request.getParameter("fullName");
        String password = request.getParameter("password");
        String confirmPassword =
                request.getParameter("confirmPassword");
        try {
        userService.registerIntern(
                username,
                email,
                fullName,
                password,
                confirmPassword,
                request.getParameter("phone"),
                request.getParameter("university"),
                request.getParameter("major"),
                request.getParameter("gpa")
        );

        request.getSession(true).setAttribute(
                "flashSuccess",
                "Đăng ký tài khoản thành công. Vui lòng đăng nhập."
        );

        response.sendRedirect(
                request.getContextPath() + "/auth/login"
        );

        } catch (IllegalArgumentException e) {

        request.setAttribute(
                "errorMessage",
                e.getMessage()
        );

        request.getRequestDispatcher("/register.jsp")
                .forward(request, response);

        } catch (IllegalStateException e) {

        throw new ServletException(
                "Unable to register intern.",
                e
        );
        }
    }
}
