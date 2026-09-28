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

import java.sql.Date;
import java.sql.SQLException;
import java.util.List;

/** HR-owned setup needed before an approved application can be enrolled. */
public class CatalogService {
    private final DepartmentDAO departments;
    private final InternshipProgramDAO programs;
    private final MentorDAO mentors;
    private final UserDAO users;

    public CatalogService() {
        this(new DepartmentDAO(), new InternshipProgramDAO(), new MentorDAO(), new UserDAO());
    }

    public CatalogService(DepartmentDAO departments, InternshipProgramDAO programs,
                          MentorDAO mentors, UserDAO users) {
        this.departments = departments;
        this.programs = programs;
        this.mentors = mentors;
        this.users = users;
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
        String cleanName = required(name, "Tên chương trình", 200);
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
        return programs.createProgram(program);
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

    public List<Mentor> getMentors() throws SQLException {
        return mentors.getAllMentors();
    }

    public List<User> getEligibleMentorUsers() throws SQLException {
        return users.findAll().stream()
                .filter(user -> user != null && user.getRole() != null
                        && "MENTOR".equals(user.getRole().getRoleName())
                        && user.getStatus() == UserStatus.ACTIVE)
                .toList();
    }

    private static String required(String value, String label, int maxLength) {
        if (value == null || value.isBlank() || value.trim().length() > maxLength) {
            throw new IllegalArgumentException(label + " không hợp lệ.");
        }
        return value.trim();
    }
}
