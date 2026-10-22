package com.internship.controller;

import jakarta.el.ELContext;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import jakarta.servlet.jsp.*;
import jakarta.servlet.jsp.el.*;
import org.apache.taglibs.standard.tag.rt.fmt.FormatDateTag;
import org.apache.taglibs.standard.tag.rt.fmt.SetLocaleTag;
import org.apache.taglibs.standard.tag.rt.fmt.SetTimeZoneTag;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.nio.file.*;
import java.time.Instant;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Màn "Đơn ứng tuyển của tôi" phải hiện giờ Việt Nam bất kể múi giờ mặc định của JVM.
 *
 * <p>Lỗi gốc: {@code fmt:formatDate} không có thuộc tính {@code timeZone} thì JSTL rơi về
 * {@code TimeZone.getDefault()} của JVM. Máy dev là Windows UTC+7 nên tình cờ đúng và che mất
 * lỗi; host chạy UTC hiện sai 7 tiếng. Đã tái hiện được end-to-end trên Tomcat:
 * cùng dữ liệu, chỉ đổi {@code -Duser.timezone=UTC} là "01/09/2026 08:30" thành
 * "01/09/2026 01:30".
 *
 * <p>Đường JDBC không phải thủ phạm: {@code db.url} đã ghim {@code serverTimezone=Asia/Ho_Chi_Minh}
 * nên instant đọc lên vẫn đúng ở mọi múi giờ JVM — chỉ khâu hiển thị trôi.
 */
class InternApplicationDateTest {

    /** Bản ghi seed #1: DB là TIMESTAMP, hiển thị ở +07 là 2026-09-01 08:30. */
    private static final Date SEED_SUBMITTED = Date.from(Instant.parse("2026-09-01T01:30:00Z"));
    private static final String EXPECTED = "01/09/2026 08:30";
    private static final String SHIFTED_WITHOUT_PIN = "01/09/2026 01:30";

    @Test
    void internApplicationsPagePinsVietnamTimeZone() throws Exception {
        String jsp = Files.readString(
                Path.of("src/main/webapp/WEB-INF/views/intern/applications.jsp"));
        assertTrue(
                jsp.matches("(?s).*<fmt:setTimeZone\\s+value=\"Asia/Ho_Chi_Minh\".*"),
                "intern/applications.jsp phải ghim Asia/Ho_Chi_Minh; thiếu nó thì ngày nộp/ngày "
                        + "duyệt trôi theo múi giờ mặc định của JVM.");
    }

    @Test
    void pinnedTimeZoneRendersSeedInstantTheSameUnderAnyJvmDefault() throws Exception {
        TimeZone original = TimeZone.getDefault();
        try {
            for (String zone : List.of("UTC", "Asia/Ho_Chi_Minh", "America/New_York")) {
                TimeZone.setDefault(TimeZone.getTimeZone(zone));
                assertEquals(EXPECTED, render(SEED_SUBMITTED, true),
                        "Đổi múi giờ JVM sang " + zone + " không được làm đổi ngày hiển thị");
            }
        } finally {
            TimeZone.setDefault(original);
        }
    }

    @Test
    void withoutThePinTheSameInstantShiftsSevenHours() throws Exception {
        TimeZone original = TimeZone.getDefault();
        try {
            TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
            assertEquals(SHIFTED_WITHOUT_PIN, render(SEED_SUBMITTED, false),
                    "Đây là lỗi gốc: bỏ ghim múi giờ thì host chạy UTC hiện lùi 7 tiếng");
        } finally {
            TimeZone.setDefault(original);
        }
    }

    /** Chạy thật hai tag JSTL như trang làm, không có header Accept-Language. */
    private static String render(Date value, boolean pinTimeZone) throws Exception {
        Fixture context = new Fixture();
        SetLocaleTag locale = new SetLocaleTag();
        locale.setPageContext(context);
        locale.setValue("vi_VN");
        locale.doEndTag();

        if (pinTimeZone) {
            SetTimeZoneTag zone = new SetTimeZoneTag();
            zone.setPageContext(context);
            zone.setValue("Asia/Ho_Chi_Minh");
            zone.doEndTag();
        }

        FormatDateTag date = new FormatDateTag();
        date.setPageContext(context);
        date.setVar("formatted");
        date.setValue(value);
        date.setPattern("dd/MM/yyyy HH:mm");
        date.doEndTag();
        return String.valueOf(context.getAttribute("formatted"));
    }

    /** PageContext tối thiểu, sao theo lối của MentorDateLocaleTest. */
    static class Fixture extends PageContext {
        final Map<String, Object> values = new HashMap<>();
        final ServletRequest request = (ServletRequest) Proxy.newProxyInstance(
                ServletRequest.class.getClassLoader(), new Class<?>[]{HttpServletRequest.class},
                (p, m, a) -> switch (m.getName()) {
                    case "getLocales" -> Collections.emptyEnumeration();
                    case "getLocale" -> Locale.US;
                    default -> null;
                });
        final ServletResponse response = (ServletResponse) Proxy.newProxyInstance(
                ServletResponse.class.getClassLoader(), new Class<?>[]{HttpServletResponse.class},
                (p, m, a) -> "getCharacterEncoding".equals(m.getName()) ? "UTF-8" : null);
        final ServletContext servletContext = (ServletContext) Proxy.newProxyInstance(
                ServletContext.class.getClassLoader(), new Class<?>[]{ServletContext.class},
                (p, m, a) -> null);

        @Override public void initialize(Servlet s, ServletRequest r, ServletResponse p, String e,
                                         boolean session, int buffer, boolean flush) { }
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
