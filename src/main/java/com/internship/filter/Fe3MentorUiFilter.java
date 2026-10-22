package com.internship.filter;

import jakarta.servlet.*;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.*;

import java.io.IOException;

/** FE3 presentation-only redirects. Auth, CSRF and mutations remain in BE1/BE3. */
@WebFilter(urlPatterns = {"/hr/mentors", "/hr/enrollments"}, dispatcherTypes = DispatcherType.REQUEST)
public class Fe3MentorUiFilter implements Filter {
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest http = (HttpServletRequest) request;
        HttpServletResponse output = (HttpServletResponse) response;
        boolean mentors = "/hr/mentors".equals(http.getServletPath());
        boolean assignments = "/hr/enrollments".equals(http.getServletPath())
                && "mentor".equals(http.getParameter("ui"));
        if (mentors || assignments) output.setHeader("Cache-Control", "no-store");
        if (!"POST".equals(http.getMethod()) || !(mentors || assignments)) {
            chain.doFilter(request, response);
            return;
        }
        chain.doFilter(request, new HttpServletResponseWrapper(output) {
            @Override public void sendRedirect(String location) throws IOException {
                String context = http.getContextPath();
                if (mentors && (location.equals(context + "/hr/catalog")
                        || location.equals(context + "/hr/mentors"))) {
                    super.sendRedirect(context + "/hr/mentors");
                    return;
                }
                String action = http.getParameter("action");
                if (assignments && ("assign-mentor".equals(action) || "change-mentor".equals(action))
                        && (location.equals(context + "/hr/enrollments")
                        || location.startsWith(context + "/hr/enrollments?"))) {
                    Integer id = positiveId(http.getParameter("enrollmentId"));
                    super.sendRedirect(context + "/hr/enrollments?ui=mentor"
                            + (id == null ? "" : "&action=detail&id=" + id));
                    return;
                }
                // Never rewrite auth redirects, unrelated locations or supplied return URLs.
                super.sendRedirect(location);
            }
        });
    }

    static Integer positiveId(String value) {
        try {
            int parsed = Integer.parseInt(value);
            return parsed > 0 ? parsed : null;
        } catch (NumberFormatException exception) { return null; }
    }
}
