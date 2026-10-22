package com.internship.service;

import com.internship.dao.*;
import com.internship.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.Date;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CatalogSprint2ServiceTest {
    private CatalogService service;
    private TestConnections connections;
    private InternshipProgram savedProgram;
    private boolean hasEnrollment;
    private boolean duplicateProgram;
    private boolean duplicateMentor;
    private boolean failSave;
    private String checkedName;
    private int excludedId;
    private int departmentLocks;
    private final List<Mentor> mentorList = new ArrayList<>();

    @BeforeEach
    void setUp() {
        connections = new TestConnections();
        mentorList.clear();
        var departments = new DepartmentDAO() {
            @Override public Department getDepartmentById(int id) {
                if (id != 1 && id != 2) return null;
                Department d = new Department(); d.setDepartmentId(id); return d;
            }
            @Override public void lockDepartment(int id, Connection c) {
                if (getDepartmentById(id) == null) throw new IllegalArgumentException("Missing department");
                departmentLocks++;
            }
        };
        var programs = new InternshipProgramDAO() {
            @Override public int createProgram(InternshipProgram p, Connection c) {
                if (failSave) return -1;
                p.setProgramId(2); savedProgram = p; return 2;
            }
            @Override public boolean updateProgram(InternshipProgram p, Connection c) {
                if (failSave) return false;
                savedProgram = p; return true;
            }
            @Override public void lockProgram(int id, Connection c) {
                if (id != 2) throw new IllegalStateException("Missing program");
            }
            @Override public InternshipProgram getProgramById(int id, Connection c) {
                if (id != 2) return null;
                InternshipProgram p = new InternshipProgram();
                p.setProgramId(2); p.setProgramName("Original"); p.setDepartmentId(1);
                p.setStartDate(Date.valueOf("2026-10-01")); p.setEndDate(Date.valueOf("2026-10-10"));
                p.setMaxInterns(5); p.setStatus("ACTIVE"); return p;
            }
            @Override public InternshipProgram getProgramById(int id) { return getProgramById(id, null); }
            @Override public boolean hasAnyEnrollment(int id, Connection c) { return hasEnrollment; }
            @Override public boolean hasEquivalentProgram(String name, int dep, Date start, Date end,
                                                          int exclude, Connection c) {
                checkedName = name; excludedId = exclude; return duplicateProgram;
            }
            @Override public List<InternshipProgram> getAllPrograms() {
                InternshipProgram a = getProgramById(2); InternshipProgram b = getProgramById(2);
                b.setProgramId(3); b.setDepartmentId(2); return List.of(a, b);
            }
        };
        var mentors = new MentorDAO() {
            @Override public int createMentor(Mentor m) { return 3; }
            @Override public boolean hasMentorForUser(long id) { return duplicateMentor; }
            @Override public List<Mentor> getAllMentors() { return mentorList; }
        };
        var users = new UserDAO() {
            @Override public User findById(long id) {
                if (id < 4 || id > 7) return null;
                Role role = new Role(); role.setRoleName(id == 6 ? "HR" : "MENTOR");
                User user = new User(); user.setUserId(id); user.setRole(role);
                user.setStatus(id == 7 ? UserStatus.INACTIVE : UserStatus.ACTIVE); return user;
            }
            @Override public List<User> findAll() { return List.of(findById(4), findById(5), findById(6), findById(7)); }
        };
        service = new CatalogService(departments, programs, mentors, users, connections);
    }

    private Date start() { return Date.valueOf("2026-10-01"); }
    private Date end() { return Date.valueOf("2026-10-10"); }
    private void edit(int dep, Date start, Date end, int capacity) throws SQLException {
        service.updateProgram(2, "Renamed", dep, "Updated", start, end, capacity);
    }

    @Test void oneDayProgramIsAllowed() throws SQLException {
        service.createProgram("One day", 1, null, start(), start(), 0);
        assertEquals(savedProgram.getStartDate(), savedProgram.getEndDate());
        assertEquals(1, departmentLocks);
    }

    @Test void requiresBothDatesAndNonnegativeCapacity() {
        assertThrows(IllegalArgumentException.class, () -> service.createProgram("P", 1, null, null, end(), 5));
        assertThrows(IllegalArgumentException.class, () -> service.createProgram("P", 1, null, start(), null, 5));
        assertThrows(IllegalArgumentException.class, () -> service.createProgram("P", 1, null, start(), end(), -1));
        assertThrows(IllegalArgumentException.class, () -> service.createProgram(" ", 1, null, start(), end(), 5));
    }

    @Test void normalizesProgramIdentityAndRejectsDuplicates() {
        duplicateProgram = true;
        assertThrows(IllegalStateException.class, () -> service.createProgram("  Java\t  WEB  ", 1, null, start(), end(), 5));
        assertEquals("java web", checkedName); assertEquals(0, excludedId);
        assertNull(savedProgram); assertEquals(1, connections.rollbacks.get());
    }

    @Test void saveFailureRollsBack() {
        failSave = true;
        assertThrows(SQLException.class, () -> service.createProgram("P", 1, null, start(), end(), 5));
        assertEquals(1, connections.rollbacks.get()); assertEquals(0, connections.commits.get());
        assertThrows(SQLException.class, () -> edit(1, start(), end(), 5));
    }

    @Test void editsDatesAndDepartmentBeforeEnrollmentAndPreservesStatus() throws SQLException {
        edit(2, start(), Date.valueOf("2026-10-15"), 10);
        assertEquals(2, savedProgram.getDepartmentId()); assertEquals("ACTIVE", savedProgram.getStatus());
        assertEquals(2, excludedId); assertEquals(1, connections.commits.get());
    }

    @Test void enrolledProgramAllowsOnlyNameAndDescription() throws SQLException {
        hasEnrollment = true; edit(1, start(), end(), 5);
        assertEquals("Renamed", savedProgram.getProgramName()); assertEquals("Updated", savedProgram.getDescription());
    }

    @Test void anyEnrollmentBlocksDateDepartmentAndCapacityChanges() {
        hasEnrollment = true;
        assertThrows(IllegalStateException.class, () -> edit(2, start(), end(), 5));
        assertThrows(IllegalStateException.class, () -> edit(1, Date.valueOf("2026-10-02"), end(), 5));
        assertThrows(IllegalStateException.class, () -> edit(1, start(), Date.valueOf("2026-10-15"), 5));
        assertThrows(IllegalStateException.class, () -> edit(1, start(), end(), 6));
        assertEquals(4, connections.rollbacks.get()); assertNull(savedProgram);
    }

    @Test void editRejectsDuplicatesAndInvalidDates() {
        duplicateProgram = true;
        assertThrows(IllegalStateException.class, () -> edit(1, start(), end(), 5));
        assertEquals(2, excludedId);
        assertThrows(IllegalArgumentException.class, () -> edit(1, end(), start(), 5));
        assertThrows(IllegalArgumentException.class, () -> edit(1, null, end(), 5));
        assertThrows(IllegalStateException.class, () -> service.updateProgram(99, "P", 1, null, start(), end(), 5));
    }

    @Test void inactiveOrWrongRoleUserCannotBecomeMentor() {
        assertThrows(IllegalArgumentException.class, () -> service.createMentor(6, 1, null, 5));
        assertThrows(IllegalArgumentException.class, () -> service.createMentor(7, 1, null, 5));
        assertThrows(IllegalArgumentException.class, () -> service.createMentor(4, 1, "a".repeat(201), 5));
    }

    @Test void existingMentorUserIsRejectedAndNotOfferedAgain() throws SQLException {
        duplicateMentor = true;
        assertThrows(IllegalStateException.class, () -> service.createMentor(4, 1, null, 5));
        Mentor existing = new Mentor(); existing.setUserId(4); mentorList.add(existing);
        assertEquals(List.of(5L), service.getEligibleMentorUsers().stream().map(User::getUserId).toList());
    }

    @Test void departmentFilterAndWorkloadUseCurrentCount() throws SQLException {
        Mentor a = new Mentor(); a.setDepartmentId(1); a.setMaxMentees(3); a.setCurrentMenteeCount(2);
        Mentor b = new Mentor(); b.setDepartmentId(2); b.setMaxMentees(2); b.setCurrentMenteeCount(2);
        mentorList.add(a); mentorList.add(b);
        assertEquals(List.of(a), service.getMentors(1)); assertEquals(2, service.getMentors(null).size());
        assertEquals(1, a.getRemainingCapacity()); assertFalse(a.isAtCapacity());
        assertEquals(0, b.getRemainingCapacity()); assertTrue(b.isAtCapacity());
        assertEquals(1, service.getPrograms(2).size());
        assertThrows(IllegalArgumentException.class, () -> service.getMentors(99));
    }
}
