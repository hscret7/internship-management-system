package com.internship.service;

import com.internship.dao.DepartmentDAO;
import com.internship.dao.InternshipProgramDAO;
import com.internship.dao.MentorDAO;
import com.internship.dao.UserDAO;
import com.internship.model.Department;
import com.internship.model.InternshipProgram;
import com.internship.model.Mentor;
import com.internship.model.Role;
import com.internship.model.User;
import com.internship.model.UserStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.Date;
import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.*;

class CatalogServiceTest {
    private DepartmentDAO departments;
    private InternshipProgramDAO programs;
    private MentorDAO mentors;
    private UserDAO users;
    private CatalogService service;
    private InternshipProgram savedProgram;
    private Mentor savedMentor;

    @BeforeEach
    void setUp() {
        departments = new DepartmentDAO() {
            @Override public Department getDepartmentById(int id) {
                if (id != 1) return null;
                Department d = new Department();
                d.setDepartmentId(1);
                return d;
            }
            @Override public int createDepartment(Department d) { return 1; }
        };
        programs = new InternshipProgramDAO() {
            @Override public int createProgram(InternshipProgram p) {
                savedProgram = p;
                return 2;
            }
            @Override public boolean activatePlannedProgram(int id) { return id == 2; }
        };
        mentors = new MentorDAO() {
            @Override public int createMentor(Mentor m) {
                savedMentor = m;
                return 3;
            }
        };
        users = new UserDAO() {
            @Override public User findById(long id) throws SQLException {
                if (id != 4) return null;
                Role role = new Role();
                role.setRoleName("MENTOR");
                User user = new User();
                user.setUserId(id);
                user.setRole(role);
                user.setStatus(UserStatus.ACTIVE);
                return user;
            }
        };
        service = new CatalogService(departments, programs, mentors, users);
    }

    @Test
    void programStartsPlannedAndRequiresValidDates() throws SQLException {
        assertThrows(IllegalArgumentException.class, () -> service.createProgram(
                "Program", 1, null, Date.valueOf("2026-10-10"),
                Date.valueOf("2026-10-01"), 5));
        assertThrows(IllegalArgumentException.class, () -> service.createProgram(
                "Program", 99, null, Date.valueOf("2026-10-01"),
                Date.valueOf("2026-10-10"), 5));
        assertEquals(2, service.createProgram("Program", 1, null,
                Date.valueOf("2026-10-01"), Date.valueOf("2026-10-10"), 5));
        assertEquals("PLANNED", savedProgram.getStatus());
        service.activateProgram(2);
        assertThrows(IllegalStateException.class, () -> service.activateProgram(99));
    }

    @Test
    void mentorMustHaveActiveMentorAccountAndDepartment() throws SQLException {
        assertThrows(IllegalArgumentException.class,
                () -> service.createMentor(99, 1, null, 5));
        assertThrows(IllegalArgumentException.class,
                () -> service.createMentor(4, 99, null, 5));
        assertThrows(IllegalArgumentException.class,
                () -> service.createMentor(4, 1, null, 0));
        assertEquals(3, service.createMentor(4, 1, "Java", 5));
        assertEquals(1, savedMentor.getDepartmentId());
    }
}
