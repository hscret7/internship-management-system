package com.internship.service;

import com.internship.dao.DepartmentDAO;
import com.internship.dao.InternshipProgramDAO;
import com.internship.dao.MentorDAO;
import com.internship.dao.UserDAO;
import com.internship.model.Department;
import com.internship.model.InternshipProgram;
import com.internship.model.Mentor;
import com.internship.model.User;
import com.internship.model.UserStatus;
import com.internship.util.ConnectionProvider;
import com.internship.util.DBContext;

import java.sql.Connection;
import java.sql.Date;
import java.sql.SQLException;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/** HR-owned setup needed before an approved application can be enrolled. */
public class CatalogService {
    private final DepartmentDAO departments;
    private final InternshipProgramDAO programs;
    private final MentorDAO mentors;
    private final UserDAO users;
    private final ConnectionProvider connections;

    public CatalogService() {
        this(new DepartmentDAO(), new InternshipProgramDAO(), new MentorDAO(), new UserDAO());
    }

    public CatalogService(DepartmentDAO departments, InternshipProgramDAO programs,
                          MentorDAO mentors, UserDAO users) {
        this(departments, programs, mentors, users, DBContext::getConnection);
    }

    public CatalogService(DepartmentDAO departments, InternshipProgramDAO programs,
                          MentorDAO mentors, UserDAO users, ConnectionProvider connections) {
        this.departments = departments;
        this.programs = programs;
        this.mentors = mentors;
        this.users = users;
        this.connections = connections;
    }

    public int createDepartment(String name, String description) throws SQLException {
        String cleanName = required(name, "Tên phòng ban", 150);
        Department department = new Department();
        department.setDepartmentName(cleanName);
        department.setDescription(description == null ? null : description.trim());
        return departments.createDepartment(department);
    }

    public int createProgram(String name, int departmentId, String description,
                             Date startDate, Date endDate, int maxInterns) throws SQLException {
        String cleanName = programName(name);
        if (departmentId <= 0 || departments.getDepartmentById(departmentId) == null) {
            throw new IllegalArgumentException("Phòng ban không tồn tại.");
        }
        if (startDate == null || endDate == null || endDate.before(startDate)) {
            throw new IllegalArgumentException("Ngày kết thúc phải sau hoặc bằng ngày bắt đầu.");
        }
        if (maxInterns < 0) throw new IllegalArgumentException("Sức chứa không hợp lệ.");
        InternshipProgram program = new InternshipProgram();
        program.setProgramName(cleanName);
        program.setDepartmentId(departmentId);
        program.setDescription(description == null ? null : description.trim());
        program.setStartDate(startDate);
        program.setEndDate(endDate);
        program.setMaxInterns(maxInterns);
        program.setStatus("PLANNED");
        try (Connection conn = connections.getConnection()) {
            conn.setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);
            conn.setAutoCommit(false);
            try {
                departments.lockDepartment(departmentId, conn);
                requireUniqueProgram(program, conn);
                int id = programs.createProgram(program, conn);
                if (id <= 0) throw new SQLException("Không thể tạo chương trình.");
                conn.commit();
                return id;
            } catch (SQLException | RuntimeException e) {
                conn.rollback();
                throw e;
            }
        }
    }

    public InternshipProgram getProgram(int programId) throws SQLException {
        if (programId <= 0) throw new IllegalArgumentException("Mã chương trình không hợp lệ.");
        InternshipProgram program = programs.getProgramById(programId);
        if (program == null) throw new IllegalArgumentException("Chương trình không tồn tại.");
        return program;
    }

    public boolean isProgramStructureLocked(int programId) throws SQLException {
        try (Connection conn = connections.getConnection()) {
            return programs.hasAnyEnrollment(programId, conn);
        }
    }

    /** Program status changes remain a separate action; edits never accept a client status. */
    public void updateProgram(int programId, String name, int departmentId, String description,
                              Date startDate, Date endDate, int maxInterns) throws SQLException {
        String cleanName = programName(name);
        if (programId <= 0 || departmentId <= 0 || startDate == null || endDate == null
                || endDate.before(startDate) || maxInterns < 0) {
            throw new IllegalArgumentException("Thông tin chương trình hoặc khoảng ngày không hợp lệ.");
        }
        try (Connection conn = connections.getConnection()) {
            conn.setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);
            conn.setAutoCommit(false);
            try {
                // Enrollment uses the same lock before inserting, preventing an edit/enroll race.
                programs.lockProgram(programId, conn);
                InternshipProgram program = programs.getProgramById(programId, conn);
                if (program == null) throw new IllegalArgumentException("Chương trình không tồn tại.");
                if (programs.hasAnyEnrollment(programId, conn)
                        && (program.getDepartmentId() != departmentId
                        || !Objects.equals(program.getStartDate(), startDate)
                        || !Objects.equals(program.getEndDate(), endDate)
                        || program.getMaxInterns() != maxInterns)) {
                    throw new IllegalStateException(
                            "Chương trình đã có ghi danh; chỉ được sửa tên và mô tả.");
                }
                departments.lockDepartment(departmentId, conn);
                program.setProgramName(cleanName);
                program.setDepartmentId(departmentId);
                program.setDescription(description == null ? null : description.trim());
                program.setStartDate(startDate);
                program.setEndDate(endDate);
                program.setMaxInterns(maxInterns);
                requireUniqueProgram(program, conn);
                if (!programs.updateProgram(program, conn)) {
                    throw new SQLException("Không thể cập nhật chương trình.");
                }
                conn.commit();
            } catch (SQLException | RuntimeException e) {
                conn.rollback();
                throw e;
            }
        }
    }

    private void requireUniqueProgram(InternshipProgram program, Connection conn) throws SQLException {
        if (programs.hasEquivalentProgram(program.getProgramName().toLowerCase(Locale.ROOT),
                program.getDepartmentId(), program.getStartDate(), program.getEndDate(),
                program.getProgramId(), conn)) {
            throw new IllegalStateException(
                    "Đã có chương trình cùng tên, phòng ban và khoảng thời gian.");
        }
    }

    public void activateProgram(int programId) throws SQLException {
        if (programId <= 0 || !programs.activatePlannedProgram(programId)) {
            throw new IllegalStateException("Chỉ có thể kích hoạt chương trình PLANNED còn tồn tại.");
        }
    }

    public int createMentor(long userId, int departmentId, String specialization,
                            int maxMentees) throws SQLException {
        User user = users.findById(userId);
        if (user == null || user.getRole() == null
                || !"MENTOR".equals(user.getRole().getRoleName())
                || user.getStatus() != UserStatus.ACTIVE) {
            throw new IllegalArgumentException("Tài khoản phải có role MENTOR và đang hoạt động.");
        }
        if (departmentId <= 0 || departments.getDepartmentById(departmentId) == null) {
            throw new IllegalArgumentException("Phòng ban không tồn tại.");
        }
        if (maxMentees <= 0) throw new IllegalArgumentException("Sức chứa mentor phải lớn hơn 0.");
        if (specialization != null && specialization.trim().length() > 200) {
            throw new IllegalArgumentException("Chuyên môn vượt quá 200 ký tự.");
        }
        if (mentors.hasMentorForUser(userId)) {
            throw new IllegalStateException("Tài khoản này đã có hồ sơ mentor.");
        }
        Mentor mentor = new Mentor();
        mentor.setUserId(userId);
        mentor.setDepartmentId(departmentId);
        mentor.setSpecialization(specialization == null ? null : specialization.trim());
        mentor.setMaxMentees(maxMentees);
        mentor.setStatus("ACTIVE");
        int id = mentors.createMentor(mentor);
        if (id <= 0) throw new IllegalStateException("Không thể tạo mentor cho tài khoản này.");
        return id;
    }

    public List<Department> getDepartments() throws SQLException {
        return departments.getAllDepartments();
    }

    public List<InternshipProgram> getPrograms() throws SQLException {
        return programs.getAllPrograms();
    }

    public List<InternshipProgram> getPrograms(Integer departmentId) throws SQLException {
        validateDepartmentFilter(departmentId);
        return getPrograms().stream()
                .filter(p -> departmentId == null || p.getDepartmentId() == departmentId)
                .toList();
    }

    public List<Mentor> getMentors() throws SQLException {
        return mentors.getAllMentors();
    }

    public List<Mentor> getMentors(Integer departmentId) throws SQLException {
        validateDepartmentFilter(departmentId);
        return getMentors().stream()
                .filter(m -> departmentId == null || Objects.equals(m.getDepartmentId(), departmentId))
                .toList();
    }

    private void validateDepartmentFilter(Integer departmentId) throws SQLException {
        if (departmentId != null
                && (departmentId <= 0 || departments.getDepartmentById(departmentId) == null)) {
            throw new IllegalArgumentException("Phòng ban không tồn tại.");
        }
    }

    public List<User> getEligibleMentorUsers() throws SQLException {
        Set<Long> existing = mentors.getAllMentors().stream()
                .map(Mentor::getUserId).collect(Collectors.toSet());
        return users.findAll().stream()
                .filter(user -> user != null && user.getRole() != null
                        && "MENTOR".equals(user.getRole().getRoleName())
                        && user.getStatus() == UserStatus.ACTIVE
                        && !existing.contains(user.getUserId()))
                .toList();
    }

    private static String required(String value, String label, int maxLength) {
        if (value == null || value.isBlank() || value.trim().length() > maxLength) {
            throw new IllegalArgumentException(label + " không hợp lệ.");
        }
        return value.trim();
    }

    private static String programName(String name) {
        String clean = name == null ? null : name.strip().replaceAll("(?U)\\s+", " ");
        return required(clean, "Tên chương trình", 200);
    }
}
