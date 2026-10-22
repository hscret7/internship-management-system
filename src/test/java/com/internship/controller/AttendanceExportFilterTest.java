package com.internship.controller;

import jakarta.servlet.http.HttpServletRequest;
import java.lang.reflect.Proxy;
import java.time.LocalDate;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class AttendanceExportFilterTest {
    Map<String, String[]> params = new HashMap<>(Map.of("from", new String[]{"2026-10-01"}, "to", new String[]{"2026-10-10"}));
    HttpServletRequest request() { return (HttpServletRequest)Proxy.newProxyInstance(HttpServletRequest.class.getClassLoader(), new Class<?>[]{HttpServletRequest.class}, (p, m, a) -> switch(m.getName()) {
        case "getParameterNames" -> Collections.enumeration(params.keySet());
        case "getParameterValues" -> params.get(a[0]);
        case "getParameter" -> params.containsKey(a[0]) ? params.get(a[0])[0] : null;
        default -> null;
    }); }
    @Test void acceptsExactBe2ParameterNamesAndInclusiveDates() { params.put("programId", new String[]{"3"}); var filter = Be4ApiServlet.attendanceFilter(request()); assertEquals(3, filter.programId()); assertEquals(LocalDate.of(2026, 10, 10), filter.to()); }
    @Test void rejectsIgnoredFilterRatherThanExportingWrongSubset() { params.put("status", new String[]{"PRESENT"}); assertThrows(IllegalArgumentException.class, () -> Be4ApiServlet.attendanceFilter(request())); }
    @Test void rejectsRepeatedFilterPollution() { params.put("programId", new String[]{"1", "2"}); assertThrows(IllegalArgumentException.class, () -> Be4ApiServlet.attendanceFilter(request())); }
    @Test void missingAndInvalidDatesReturnValidationFailure() { params.remove("from"); assertThrows(IllegalArgumentException.class, () -> Be4ApiServlet.attendanceFilter(request())); params.put("from", new String[]{"2026-02-30"}); assertThrows(IllegalArgumentException.class, () -> Be4ApiServlet.attendanceFilter(request())); }
}
