package com.internship.dao;

import java.math.*;
import java.sql.*;
import java.time.LocalDate;
import java.util.*;

/** One row per enrollment before aggregation; never joins task/report detail tables. */
public class ProgramStatisticsDAO {
    public record Filter(Integer programId, String status, LocalDate cohortFrom, LocalDate cohortTo) {
        public Filter {
            if (programId != null && programId <= 0) throw new IllegalArgumentException("programId không hợp lệ.");
            if (status != null && !Set.of("ACTIVE", "COMPLETED", "CANCELLED").contains(status)) throw new IllegalArgumentException("status không hợp lệ.");
            if ((cohortFrom == null) != (cohortTo == null) || (cohortFrom != null && cohortTo.isBefore(cohortFrom))) throw new IllegalArgumentException("Khoảng ngày bắt đầu chương trình không hợp lệ.");
        }
    }
    public record Group(String name, long interns, long enrollments) { }
    public record Summary(long interns, long enrollments, long active, long completed, long cancelled,
                          BigDecimal completionRate, List<Group> universities, List<Group> majors) { }
    private record Row(int intern, String status, String university, String major) { }
    public Summary summarize(Connection c, Filter f) throws SQLException {
        String sql = """
            SELECT pe.intern_profile_id,pe.status,ip.university,ip.major
            FROM program_enrollments pe
            JOIN intern_profiles ip ON ip.profile_id=pe.intern_profile_id
            JOIN internship_programs p ON p.program_id=pe.program_id
            WHERE (? IS NULL OR pe.program_id=?) AND (? IS NULL OR pe.status=?)
              AND (? IS NULL OR p.start_date BETWEEN ? AND ?)
            """;
        List<Row> rows = new ArrayList<>();
        try (var s = c.prepareStatement(sql)) {
            s.setObject(1, f.programId()); s.setObject(2, f.programId());
            s.setString(3, f.status()); s.setString(4, f.status());
            s.setObject(5, f.cohortFrom()); s.setObject(6, f.cohortFrom()); s.setObject(7, f.cohortTo());
            try (var r = s.executeQuery()) { while (r.next()) rows.add(new Row(r.getInt(1), r.getString(2), group(r.getString(3)), group(r.getString(4)))); }
        }
        long active = rows.stream().filter(r -> "ACTIVE".equals(r.status)).count();
        long completed = rows.stream().filter(r -> "COMPLETED".equals(r.status)).count();
        long cancelled = rows.stream().filter(r -> "CANCELLED".equals(r.status)).count();
        return new Summary(rows.stream().map(Row::intern).distinct().count(), rows.size(), active, completed, cancelled,
                rate(completed, rows.size()), groups(rows, true), groups(rows, false));
    }
    public static BigDecimal rate(long completed, long total) {
        return total == 0 ? new BigDecimal("0.00") : BigDecimal.valueOf(completed * 100).divide(BigDecimal.valueOf(total), 2, RoundingMode.HALF_UP);
    }
    private static String group(String value) { return value == null || value.isBlank() ? "Chưa khai báo" : value.strip(); }
    private static List<Group> groups(List<Row> rows, boolean university) {
        Map<String, List<Row>> grouped = new TreeMap<>();
        for (var r : rows) grouped.computeIfAbsent(university ? r.university : r.major, k -> new ArrayList<>()).add(r);
        return grouped.entrySet().stream().map(e -> new Group(e.getKey(), e.getValue().stream().map(Row::intern).distinct().count(), e.getValue().size())).toList();
    }
}
