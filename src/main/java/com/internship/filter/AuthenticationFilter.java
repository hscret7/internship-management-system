package com.internship.filter;

import com.internship.dao.UserDAO;
import com.internship.model.User;
import com.internship.model.UserStatus;
import java.sql.SQLException;
import java.util.Objects;

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

    private final UserDAO userDAO;

    public AuthenticationFilter() {
        this(new UserDAO());
    }

    public AuthenticationFilter(UserDAO userDAO) {
        this.userDAO = Objects.requireNonNull(userDAO);
    }

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
        String path = com.internship.util.RequestPath.mapped(request);

        if (path.equals("/uploads") || path.startsWith("/uploads/")) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        boolean publicRequest =
                path.equals("/auth/login")
                || path.equals("/auth/register")
                || path.startsWith("/assets/")
                || path.equals("/favicon.ico");

        // Static resources never need an account lookup.
        if (path.startsWith("/assets/") || path.equals("/favicon.ico")) {
            chain.doFilter(request, response);
            return;
        }

        HttpSession session = request.getSession(false);
        Object attribute = session == null ? null : session.getAttribute("currentUser");
        boolean authenticated = false;
        if (attribute instanceof User currentUser) {
            User freshUser;
            try {
                freshUser = userDAO.findById(currentUser.getUserId());
            } catch (SQLException e) {
                // A database outage must not grant access or log every user out.
                response.sendError(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
                return;
            }
            authenticated = freshUser != null
                    && freshUser.getStatus() == UserStatus.ACTIVE
                    && freshUser.getRole() != null
                    && freshUser.getRole().getRoleName() != null
                    && currentUser.getRole() != null
                    && Objects.equals(freshUser.getRole().getRoleName(),
                            currentUser.getRole().getRoleName());
            if (authenticated) {
                session.setAttribute("currentUser", freshUser);
                session.setAttribute("currentRole", freshUser.getRole().getRoleName());
            } else {
                session.invalidate();
            }
        } else if (attribute != null) {
            session.invalidate();
        }

        if (!authenticated && !publicRequest) {
            response.sendRedirect(contextPath + "/auth/login");
            return;
        }

        chain.doFilter(request, response);
    }
}
