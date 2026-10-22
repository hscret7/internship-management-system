package com.internship.controller;

import jakarta.el.ELContext;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import jakarta.servlet.jsp.*;
import jakarta.servlet.jsp.el.*;
import org.apache.taglibs.standard.tag.rt.fmt.FormatDateTag;
import org.apache.taglibs.standard.tag.rt.fmt.SetLocaleTag;
import org.junit.jupiter.api.Test;
import java.lang.reflect.Proxy;
import java.nio.file.*;
import java.time.Instant;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

/** Real JSTL tag evaluation, with no Accept-Language header and no SMTP/database access. */
class MentorDateLocaleTest {
    @Test void mentorPageDeclaresItsOwnLocale() throws Exception {
        String jsp = Files.readString(Path.of("src/main/webapp/WEB-INF/views/hr/mentor-management.jsp"));
        assertTrue(jsp.contains("<fmt:setLocale value=\"vi_VN\" scope=\"page\" />"));
    }
    @Test void explicitVietnameseLocaleFormatsDateWithoutAcceptLanguage() throws Exception {
        Fixture context = new Fixture();
        SetLocaleTag locale = new SetLocaleTag(); locale.setPageContext(context); locale.setScope("page");
        locale.setValue("vi_VN"); locale.doEndTag();
        FormatDateTag date = new FormatDateTag(); date.setPageContext(context); date.setVar("formatted");
        date.setValue(Date.from(Instant.parse("2026-09-05T09:00:00Z")));
        date.setTimeZone(TimeZone.getTimeZone("UTC")); date.setPattern("dd/MM/yyyy · HH:mm");
        date.doEndTag(); assertEquals("05/09/2026 · 09:00", context.getAttribute("formatted"));
    }
    static class Fixture extends PageContext {
        final Map<String, Object> values = new HashMap<>();
        final ServletRequest request = (ServletRequest) Proxy.newProxyInstance(ServletRequest.class.getClassLoader(),
                new Class<?>[]{HttpServletRequest.class}, (p, m, a) -> switch (m.getName()) {
                    case "getLocales" -> Collections.emptyEnumeration();
                    case "getLocale" -> Locale.US;
                    default -> null;
                });
        final ServletResponse response = (ServletResponse) Proxy.newProxyInstance(ServletResponse.class.getClassLoader(),
                new Class<?>[]{HttpServletResponse.class}, (p, m, a) -> m.getName().equals("getCharacterEncoding") ? "UTF-8" : null);
        final ServletContext servletContext = (ServletContext) Proxy.newProxyInstance(ServletContext.class.getClassLoader(),
                new Class<?>[]{ServletContext.class}, (p, m, a) -> null);
        @Override public void initialize(Servlet s, ServletRequest r, ServletResponse p, String e, boolean session, int buffer, boolean flush) { }
        @Override public void release() { }
        @Override public HttpSession getSession() { return null; }
        @Override public Object getPage() { return null; }
        @Override public ServletRequest getRequest() { return request; }
        @Override public ServletResponse getResponse() { return response; }
        @Override public Exception getException() { return null; }
        @Override public ServletConfig getServletConfig() { return null; }
        @Override public ServletContext getServletContext() { return servletContext; }
        @Override public void forward(String r) { }
        @Override public void include(String r) { }
        @Override public void include(String r, boolean flush) { }
        @Override public void handlePageException(Exception e) { throw new AssertionError(e); }
        @Override public void handlePageException(Throwable e) { throw new AssertionError(e); }
        @Override public void setAttribute(String key, Object value) { values.put(key, value); }
        @Override public void setAttribute(String key, Object value, int scope) { values.put(key, value); }
        @Override public Object getAttribute(String key) { return values.get(key); }
        @Override public Object getAttribute(String key, int scope) { return scope == PAGE_SCOPE ? values.get(key) : null; }
        @Override public Object findAttribute(String key) { return values.get(key); }
        @Override public void removeAttribute(String key) { values.remove(key); }
        @Override public void removeAttribute(String key, int scope) { values.remove(key); }
        @Override public int getAttributesScope(String key) { return values.containsKey(key) ? PAGE_SCOPE : 0; }
        @Override public Enumeration<String> getAttributeNamesInScope(int scope) { return Collections.enumeration(values.keySet()); }
        @Override public JspWriter getOut() { return null; }
        @Override public ExpressionEvaluator getExpressionEvaluator() { return null; }
        @Override public VariableResolver getVariableResolver() { return null; }
        @Override public ELContext getELContext() { return null; }
    }
}
