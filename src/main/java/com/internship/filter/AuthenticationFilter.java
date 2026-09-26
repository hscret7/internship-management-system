package com.internship.filter;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;

@WebFilter("/*")
public class AuthenticationFilter implements Filter {

    @Override
    public void doFilter(
            ServletRequest servletRequest,
            ServletResponse servletResponse,
            FilterChain chain
    ) throws IOException, ServletException {

        HttpServletRequest request =
                (HttpServletRequest) servletRequest;

        HttpServletResponse response =
                (HttpServletResponse) servletResponse;

        String contextPath = request.getContextPath();
        String requestURI = request.getRequestURI();

        String path = requestURI.substring(
                contextPath.length()
        );

        boolean publicRequest =
                path.equals("/auth/login")
                || path.startsWith("/assets/")
                || path.equals("/favicon.ico");

        if (publicRequest) {
            chain.doFilter(request, response);
            return;
        }

        HttpSession session =
                request.getSession(false);

        boolean authenticated =
                session != null
                && session.getAttribute(
                        "currentUser"
                ) != null;

        if (!authenticated) {
            response.sendRedirect(
                    contextPath + "/auth/login"
            );
            return;
        }

        chain.doFilter(request, response);
    }
}