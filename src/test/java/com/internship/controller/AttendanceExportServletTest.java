package com.internship.controller;

import com.internship.model.*;
import com.internship.service.*;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.io.*;
import java.lang.reflect.*;
import java.sql.SQLException;
import java.time.*;
import java.util.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

class AttendanceExportServletTest {
    Map<String, String> params = new HashMap<>(); Map<String, Object> attributes = new HashMap<>();
    Map<String, String> headers = new HashMap<>(); int status = 200; String type; StringWriter json = new StringWriter();
    ByteArrayOutputStream file = new ByteArrayOutputStream(); boolean loaded, recorded, sqlFailure;
    HttpServletRequest request; HttpServletResponse response; Be4ApiServlet servlet;
    @BeforeEach void setup() throws Exception {
        params.putAll(Map.of("format", "xlsx", "from", "2026-10-01", "to", "2026-10-10"));
        var session = proxy(HttpSession.class, (p, m, a) -> m.getName().equals("getAttribute") ? attributes.get(a[0]) : null);
        request = proxy(HttpServletRequest.class, (p, m, a) -> switch(m.getName()) {
            case "getSession" -> session; case "getServletPath" -> "/hr/be4"; case "getPathInfo" -> "/attendance/export";
            case "getParameter" -> params.get(a[0]); case "getParameterNames" -> Collections.enumeration(params.keySet());
            case "getParameterValues" -> params.containsKey(a[0]) ? new String[]{params.get(a[0])} : null;
            default -> null;
        });
        ServletOutputStream output = new ServletOutputStream() { public boolean isReady() { return true; } public void setWriteListener(WriteListener listener) {} public void write(int value) { file.write(value); } };
        response = proxy(HttpServletResponse.class, (p, m, a) -> switch(m.getName()) {
            case "getWriter" -> new PrintWriter(json); case "getOutputStream" -> output;
            case "setStatus" -> { status = (int)a[0]; yield null; }
            case "setContentType" -> { type = (String)a[0]; yield null; }
            case "setHeader" -> { headers.put((String)a[0], (String)a[1]); yield null; }
            default -> null;
        });
        var context = proxy(ServletContext.class, (p, m, a) -> null);
        var config = proxy(ServletConfig.class, (p, m, a) -> m.getName().equals("getServletContext") ? context : null);
        servlet = new Be4ApiServlet(); servlet.init(config);
        var service = new AttendanceReportService() {
            @Override public Snapshot snapshot(long actor, Filter filter) throws SQLException { loaded = true; assertEquals(10, actor); if (sqlFailure) throw new SQLException("source mismatch", "42S22", 1054); return new Snapshot(filter, List.of(), Instant.now()); }
            @Override public void recordExport(long actor, Snapshot snapshot, String format) { recorded = true; }
        };
        Field field = Be4ApiServlet.class.getDeclaredField("attendanceExports"); field.setAccessible(true); field.set(servlet, service);
    }
    void login(String roleName) { User user = new User(); user.setUserId(10L); Role role = new Role(); role.setRoleName(roleName); user.setRole(role); attributes.put("currentUser", user); }
    @Test void anonymousReceives401WithoutDataRead() throws Exception { servlet.doGet(request, response); assertEquals(401, status); assertFalse(loaded); assertEquals(0, file.size()); }
    @Test void internReceives403WithoutDataRead() throws Exception { login("INTERN"); servlet.doGet(request, response); assertEquals(403, status); assertFalse(loaded); }
    @Test void hrReceivesRealAttachmentAndSuccessfulGenerationEvidence() throws Exception { login("HR"); servlet.doGet(request, response); assertEquals(200, status); assertTrue(loaded); assertTrue(recorded); assertEquals("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", type); assertTrue(headers.get("Content-Disposition").contains("chuyen-can.xlsx")); assertEquals("no-store", headers.get("Cache-Control")); try(var book = new XSSFWorkbook(new ByteArrayInputStream(file.toByteArray()))) { assertEquals("Chuyên cần", book.getSheetName(0)); } assertEquals("", json.toString()); }
    @Test void invalidFormatAndPeriodFailBeforeSourceCall() throws Exception { login("ADMIN"); params.put("format", "csv"); servlet.doGet(request, response); assertEquals(400, status); assertFalse(loaded); params.put("format", "xlsx"); params.remove("from"); servlet.doGet(request, response); assertEquals(400, status); assertFalse(loaded); }
    @Test void sourceFailureIsJson500WithNoMisleadingFileOrGenerationRecord() throws Exception { login("HR"); sqlFailure = true; servlet.doGet(request, response); assertEquals(500, status); assertTrue(json.toString().contains("Lỗi dữ liệu")); assertEquals(0, file.size()); assertFalse(recorded); assertFalse(headers.containsKey("Content-Disposition")); }
    @SuppressWarnings("unchecked") static <T> T proxy(Class<T> type, InvocationHandler handler) { return (T)Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type}, handler); }
}
