package com.internship.dao;

import com.internship.model.InternshipProgram;
import com.internship.util.DBContext;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * InternshipProgramDAO: Xử lý CSDL cho bảng internship_programs (BE3).
 */
public class InternshipProgramDAO {

    public int createProgram(InternshipProgram program) throws SQLException {
        try (Connection conn = DBContext.getConnection()) {
            return createProgram(program, conn);
        }
    }

    public int createProgram(InternshipProgram program, Connection conn) throws SQLException {
        String sql = """
            INSERT INTO internship_programs
                (program_name, department_id, description, start_date, end_date, max_interns, status)
            VALUES (?, ?, ?, ?, ?, ?, ?)
            """;
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, program.getProgramName());
            ps.setInt(2, program.getDepartmentId());
            ps.setString(3, program.getDescription());
            ps.setDate(4, program.getStartDate());
            ps.setDate(5, program.getEndDate());
            ps.setInt(6, program.getMaxInterns());
            ps.setString(7, program.getStatus() != null ? program.getStatus() : "PLANNED");
            int rows = ps.executeUpdate();
            if (rows > 0) {
                try (ResultSet rs = ps.getGeneratedKeys()) {
                    if (rs.next()) {
                        int id = rs.getInt(1);
                        program.setProgramId(id);
                        return id;
                    }
                }
            }
        }
        return -1;
    }

    public InternshipProgram getProgramById(int programId) throws SQLException {
        String sql = """
            SELECT p.*, d.department_name
            FROM internship_programs p
            JOIN departments d ON p.department_id = d.department_id
            WHERE p.program_id = ?
            """;
        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, programId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapResultSet(rs);
                }
            }
        }
        return null;
    }

    /**
     * Overload với connection cho transaction.
     */
    public InternshipProgram getProgramById(int programId, Connection conn) throws SQLException {
        String sql = """
            SELECT p.*, d.department_name
            FROM internship_programs p
            JOIN departments d ON p.department_id = d.department_id
            WHERE p.program_id = ?
            """;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, programId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapResultSet(rs);
                }
            }
        }
        return null;
    }

    public void lockProgram(int programId, Connection conn) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT program_id FROM internship_programs WHERE program_id = ? FOR UPDATE")) {
            ps.setInt(1, programId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) throw new IllegalStateException("Chương trình thực tập không tồn tại.");
            }
        }
    }

    public List<InternshipProgram> getActivePrograms() throws SQLException {
        String sql = """
            SELECT p.*, d.department_name
            FROM internship_programs p
            JOIN departments d ON p.department_id = d.department_id
            WHERE p.status = 'ACTIVE'
            ORDER BY p.start_date DESC
            """;
        return executeListQuery(sql);
    }

    public List<InternshipProgram> getAllPrograms() throws SQLException {
        String sql = """
            SELECT p.*, d.department_name
            FROM internship_programs p
            JOIN departments d ON p.department_id = d.department_id
            ORDER BY p.created_at DESC
            """;
        return executeListQuery(sql);
    }

    public boolean updateProgram(InternshipProgram program) throws SQLException {
        try (Connection conn = DBContext.getConnection()) {
            return updateProgram(program, conn);
        }
    }

    public boolean updateProgram(InternshipProgram program, Connection conn) throws SQLException {
        String sql = """
            UPDATE internship_programs
            SET program_name = ?, department_id = ?, description = ?,
                start_date = ?, end_date = ?, max_interns = ?, status = ?
            WHERE program_id = ?
            """;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, program.getProgramName());
            ps.setInt(2, program.getDepartmentId());
            ps.setString(3, program.getDescription());
            ps.setDate(4, program.getStartDate());
            ps.setDate(5, program.getEndDate());
            ps.setInt(6, program.getMaxInterns());
            ps.setString(7, program.getStatus());
            ps.setInt(8, program.getProgramId());
            return ps.executeUpdate() > 0;
        }
    }

    public boolean activatePlannedProgram(int programId) throws SQLException {
        String sql = "UPDATE internship_programs SET status = 'ACTIVE' "
                + "WHERE program_id = ? AND status = 'PLANNED'";
        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, programId);
            return ps.executeUpdate() == 1;
        }
    }

    /** Caller holds the department lock, so concurrent creates/edits cannot race. */
    public boolean hasEquivalentProgram(String normalizedName, int departmentId,
                                        Date startDate, Date endDate, int excludedId,
                                        Connection conn) throws SQLException {
        String sql = """
            SELECT 1 FROM internship_programs
            WHERE LOWER(TRIM(REGEXP_REPLACE(program_name, '[[:space:]]+', ' ')))
                    COLLATE utf8mb4_bin = ?
              AND department_id = ? AND start_date = ? AND end_date = ?
              AND program_id <> ? LIMIT 1
            """;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, normalizedName);
            ps.setInt(2, departmentId);
            ps.setDate(3, startDate);
            ps.setDate(4, endDate);
            ps.setInt(5, excludedId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    /** Even a completed/cancelled enrollment freezes the original date/department. */
    public boolean hasAnyEnrollment(int programId, Connection conn) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT 1 FROM program_enrollments WHERE program_id = ? LIMIT 1")) {
            ps.setInt(1, programId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    /**
     * Đếm số enrollment ACTIVE trong chương trình.
     */
    public int countActiveEnrollments(int programId, Connection conn) throws SQLException {
        String sql = "SELECT COUNT(*) FROM program_enrollments WHERE program_id = ? AND status = 'ACTIVE'";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, programId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        }
        return 0;
    }

    // --- Private helpers ---

    private List<InternshipProgram> executeListQuery(String sql) throws SQLException {
        List<InternshipProgram> list = new ArrayList<>();
        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(mapResultSet(rs));
            }
        }
        return list;
    }

    private InternshipProgram mapResultSet(ResultSet rs) throws SQLException {
        InternshipProgram p = new InternshipProgram();
        p.setProgramId(rs.getInt("program_id"));
        p.setProgramName(rs.getString("program_name"));
        p.setDepartmentId(rs.getInt("department_id"));
        p.setDescription(rs.getString("description"));
        p.setStartDate(rs.getDate("start_date"));
        p.setEndDate(rs.getDate("end_date"));
        p.setMaxInterns(rs.getInt("max_interns"));
        p.setStatus(rs.getString("status"));
        p.setCreatedAt(rs.getTimestamp("created_at"));
        p.setUpdatedAt(rs.getTimestamp("updated_at"));
        try {
            p.setDepartmentName(rs.getString("department_name"));
        } catch (SQLException ignored) { }
        return p;
    }
}
