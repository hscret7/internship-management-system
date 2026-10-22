package com.internship.filter;

import com.internship.util.CsrfToken;
import jakarta.servlet.*;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.*;
import java.io.IOException;

/** Protect BE2 HTML/multipart mutations, including aliases outside role areas. */
@WebFilter(urlPatterns = {"/intern/profile", "/hr/interns", "/hr/interns/search",
        "/hr/intern-detail", "/hr/interns/edit", "/document/upload", "/contract/upload",
        "/document/review", "/hr/document/review", "/hr/documents/review", "/intern/contract",
        "/contract/view", "/contract/confirm", "/hr/contracts"})
public class ProfileDocumentCsrfFilter implements Filter {
    @Override public void doFilter(ServletRequest input, ServletResponse output, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest request = (HttpServletRequest) input;
        request.setCharacterEncoding("UTF-8");
        if ("POST".equals(request.getMethod()) && !CsrfToken.valid(request)) {
            ((HttpServletResponse) output).sendError(403, "Phiên biểu mẫu hết hạn. Tải lại trang và thử lại.");
            return;
        }
        chain.doFilter(input, output);
    }
}
