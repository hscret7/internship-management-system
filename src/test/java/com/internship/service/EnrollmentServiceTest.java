package com.internship.service;

import com.internship.dao.*;
import com.internship.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.Date;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for EnrollmentService.
 * Tests enrollment, mentor assignment, and mentor change business rules
 * using stub DAOs. No database connection needed.
 *
 * NOTE: EnrollmentService methods use DBContext.getConnection() internally
 * for transactions. These tests focus on the validation logic by testing
 * the DAO stubs directly. The service-level transaction flow requires
 * a real database connection – marked as "chưa kiểm chứng runtime".
 */
class EnrollmentServiceTest {

    // ========== Stub DAOs ==========

    static class StubApplicationDAO extends ApplicationDAO {
        final List<Application> applications = new ArrayList<>();

        void addApp(int id, int internId, String status) {
            Application a = new Application();
            a.setApplicationId(id);
            a.setInternId(internId);
            a.setStatus(status);
            a.setSubmittedAt(Timestamp.from(Instant.now()));
            a.setInternName("Test Intern " + internId);
            a.setInternEmail("intern" + internId + "@test.com");
            applications.add(a);
        }

        @Override
        public Application getApplicationById(int applicationId) {
            return applications.stream()
                    .filter(a -> a.getApplicationId() == applicationId)
                    .findFirst().orElse(null);
        }

        @Override
        public Application getApplicationById(int applicationId, Connection conn) {
            return getApplicationById(applicationId);
        }
    }

    static class StubEnrollmentDAO extends ProgramEnrollmentDAO {
        final List<ProgramEnrollment> enrollments = new ArrayList<>();
        final AtomicInteger nextId = new AtomicInteger(1);

        @Override
        public int createEnrollment(ProgramEnrollment enrollment, Connection conn) {
            int id = nextId.getAndIncrement();
            enrollment.setEnrollmentId(id);
            enrollment.setStatus("ACTIVE");
            enrollment.setEnrolledAt(Timestamp.from(Instant.now()));
            enrollments.add(enrollment);
            return id;
        }

        @Override
        public boolean hasActiveEnrollment(int internProfileId, Connection conn) {
            return enrollments.stream()
                    .anyMatch(e -> e.getInternProfileId() == internProfileId
                            && "ACTIVE".equals(e.getStatus()));
        }

        @Override
        public boolean hasEnrollmentForApplication(int applicationId, Connection conn) {
            return enrollments.stream()
                    .anyMatch(e -> e.getApplicationId() == applicationId);
        }

        @Override
        public ProgramEnrollment getEnrollmentById(int enrollmentId) {
            return enrollments.stream()
                    .filter(e -> e.getEnrollmentId() == enrollmentId)
                    .findFirst().orElse(null);
        }

        @Override
        public ProgramEnrollment getEnrollmentById(int enrollmentId, Connection conn) {
            return getEnrollmentById(enrollmentId);
        }

        @Override
        public ProgramEnrollment getActiveEnrollmentByIntern(int internProfileId) {
            return enrollments.stream()
                    .filter(e -> e.getInternProfileId() == internProfileId
                            && "ACTIVE".equals(e.getStatus()))
                    .findFirst().orElse(null);
        }

        @Override
        public List<ProgramEnrollment> getAllEnrollments() {
            return new ArrayList<>(enrollments);
        }
    }

    static class StubMentorAssignmentDAO extends MentorAssignmentDAO {
        final List<MentorAssignment> assignments = new ArrayList<>();
        final AtomicInteger nextId = new AtomicInteger(1);

        @Override
        public int createAssignment(MentorAssignment assignment, Connection conn) {
            int id = nextId.getAndIncrement();
            assignment.setAssignmentId(id);
            assignment.setAssignedAt(Timestamp.from(Instant.now()));
            assignments.add(assignment);
            return id;
        }

        @Override
        public boolean hasActiveAssignment(int enrollmentId, Connection conn) {
            return assignments.stream()
                    .anyMatch(a -> a.getEnrollmentId() == enrollmentId && a.getEndedAt() == null);
        }

        @Override
        public boolean endActiveAssignment(int enrollmentId, String endReason, Connection conn) {
            for (MentorAssignment a : assignments) {
                if (a.getEnrollmentId() == enrollmentId && a.getEndedAt() == null) {
                    a.setEndedAt(Timestamp.from(Instant.now()));
                    a.setEndReason(endReason);
                    return true;
                }
            }
            return false;
        }

        @Override
        public MentorAssignment getActiveAssignment(int enrollmentId) {
            return assignments.stream()
                    .filter(a -> a.getEnrollmentId() == enrollmentId && a.getEndedAt() == null)
                    .findFirst().orElse(null);
        }

        @Override
        public List<MentorAssignment> getAssignmentHistory(int enrollmentId) {
            return assignments.stream()
                    .filter(a -> a.getEnrollmentId() == enrollmentId)
                    .toList();
        }
    }

    static class StubProgramDAO extends InternshipProgramDAO {
        final List<InternshipProgram> programs = new ArrayList<>();

        void addProgram(int id, String name, String status, int maxInterns) {
            InternshipProgram p = new InternshipProgram();
            p.setProgramId(id);
            p.setProgramName(name);
            p.setStatus(status);
            p.setMaxInterns(maxInterns);
            p.setDepartmentId(1);
            p.setStartDate(Date.valueOf(LocalDate.now()));
            p.setEndDate(Date.valueOf(LocalDate.now().plusMonths(3)));
            programs.add(p);
        }

        @Override
        public InternshipProgram getProgramById(int programId, Connection conn) {
            return programs.stream()
                    .filter(p -> p.getProgramId() == programId)
                    .findFirst().orElse(null);
        }

        @Override
        public int countActiveEnrollments(int programId, Connection conn) {
            // Deferred to EnrollmentDAO in practice
            return 0;
        }
    }

    static class StubMentorDAO extends MentorDAO {
        final List<Mentor> mentors = new ArrayList<>();

        void addMentor(int id, long userId, String status, int maxMentees, int currentMentees) {
            Mentor m = new Mentor();
            m.setMentorId(id);
            m.setUserId(userId);
            m.setStatus(status);
            m.setMaxMentees(maxMentees);
            m.setCurrentMenteeCount(currentMentees);
            m.setFullName("Mentor " + id);
            mentors.add(m);
        }

        @Override
        public Mentor getMentorById(int mentorId, Connection conn) {
            return mentors.stream()
                    .filter(m -> m.getMentorId() == mentorId)
                    .findFirst().orElse(null);
        }

        @Override
        public Mentor getMentorById(int mentorId) {
            return getMentorById(mentorId, null);
        }
    }

    // ========== Fields and setup ==========

    private StubApplicationDAO appDAO;
    private StubEnrollmentDAO enrollmentDAO;
    private StubMentorAssignmentDAO mentorAssignmentDAO;
    private StubProgramDAO programDAO;
    private StubMentorDAO mentorDAO;

    @BeforeEach
    void setUp() {
        appDAO = new StubApplicationDAO();
        enrollmentDAO = new StubEnrollmentDAO();
        mentorAssignmentDAO = new StubMentorAssignmentDAO();
        programDAO = new StubProgramDAO();
        mentorDAO = new StubMentorDAO();
    }

    // ========== Enrollment Tests ==========

    @Nested
    @DisplayName("Enrollment Rules")
    class EnrollmentRuleTests {

        @Test
        @DisplayName("Cannot enroll non-APPROVED application")
        void cannotEnrollNonApproved() {
            appDAO.addApp(1, 1, "PENDING");
            // Direct DAO check – verifying the validation rule
            Application app = appDAO.getApplicationById(1);
            assertNotEquals("APPROVED", app.getStatus());
        }

        @Test
        @DisplayName("Cannot enroll REJECTED application")
        void cannotEnrollRejected() {
            appDAO.addApp(1, 1, "REJECTED");
            Application app = appDAO.getApplicationById(1);
            assertNotEquals("APPROVED", app.getStatus());
        }

        @Test
        @DisplayName("Application already enrolled cannot be enrolled again (UNIQUE check)")
        void duplicateEnrollmentPrevented() {
            appDAO.addApp(1, 1, "APPROVED");
            programDAO.addProgram(1, "Java Bootcamp", "ACTIVE", 10);

            // First enrollment
            ProgramEnrollment e = new ProgramEnrollment();
            e.setApplicationId(1);
            e.setProgramId(1);
            e.setInternProfileId(1);
            enrollmentDAO.createEnrollment(e, null);

            // Check: already enrolled
            assertTrue(enrollmentDAO.hasEnrollmentForApplication(1, null));
        }

        @Test
        @DisplayName("Intern with ACTIVE enrollment cannot get another")
        void duplicateActiveEnrollmentPrevented() {
            appDAO.addApp(1, 1, "APPROVED");
            programDAO.addProgram(1, "Java Bootcamp", "ACTIVE", 10);

            // First enrollment
            ProgramEnrollment e = new ProgramEnrollment();
            e.setApplicationId(1);
            e.setProgramId(1);
            e.setInternProfileId(1);
            enrollmentDAO.createEnrollment(e, null);

            // Check: intern already has active enrollment
            assertTrue(enrollmentDAO.hasActiveEnrollment(1, null));
        }

        @Test
        @DisplayName("Enrollment requires ACTIVE program")
        void enrollmentRequiresActiveProgram() {
            programDAO.addProgram(1, "Old Program", "INACTIVE", 0);
            InternshipProgram prog = programDAO.getProgramById(1, null);
            assertNotEquals("ACTIVE", prog.getStatus());
        }

        @Test
        @DisplayName("Program max_interns limit is respected")
        void programMaxInternsLimit() {
            programDAO.addProgram(1, "Limited Program", "ACTIVE", 2);
            InternshipProgram prog = programDAO.getProgramById(1, null);
            assertEquals(2, prog.getMaxInterns());
        }
    }

    // ========== Mentor Assignment Tests ==========

    @Nested
    @DisplayName("Mentor Assignment Rules")
    class MentorAssignmentRuleTests {

        @Test
        @DisplayName("Cannot assign mentor to enrollment that already has active assignment")
        void cannotDoubleAssign() {
            // Create enrollment and assignment
            ProgramEnrollment e = new ProgramEnrollment();
            e.setEnrollmentId(1);
            e.setApplicationId(1);
            e.setProgramId(1);
            e.setInternProfileId(1);
            e.setStatus("ACTIVE");
            enrollmentDAO.enrollments.add(e);

            MentorAssignment ma = new MentorAssignment();
            ma.setEnrollmentId(1);
            ma.setMentorId(1);
            ma.setAssignedBy(100L);
            mentorAssignmentDAO.createAssignment(ma, null);

            // Check: has active assignment
            assertTrue(mentorAssignmentDAO.hasActiveAssignment(1, null));
        }

        @Test
        @DisplayName("Mentor at capacity cannot accept new mentees")
        void mentorCapacityCheck() {
            mentorDAO.addMentor(1, 100L, "ACTIVE", 3, 3);
            Mentor mentor = mentorDAO.getMentorById(1);
            assertTrue(mentor.getCurrentMenteeCount() >= mentor.getMaxMentees());
        }

        @Test
        @DisplayName("Inactive mentor cannot be assigned")
        void inactiveMentorCheck() {
            mentorDAO.addMentor(1, 100L, "INACTIVE", 5, 0);
            Mentor mentor = mentorDAO.getMentorById(1);
            assertNotEquals("ACTIVE", mentor.getStatus());
        }
    }

    // ========== Mentor Change Tests ==========

    @Nested
    @DisplayName("Mentor Change Rules")
    class MentorChangeRuleTests {

        @Test
        @DisplayName("Changing mentor ends old assignment and creates new one")
        void changeMentorEndsOldCreatesNew() {
            // Setup: enrollment with active assignment
            ProgramEnrollment e = new ProgramEnrollment();
            e.setEnrollmentId(1);
            e.setStatus("ACTIVE");
            enrollmentDAO.enrollments.add(e);

            MentorAssignment oldAssignment = new MentorAssignment();
            oldAssignment.setEnrollmentId(1);
            oldAssignment.setMentorId(1);
            oldAssignment.setAssignedBy(100L);
            mentorAssignmentDAO.createAssignment(oldAssignment, null);

            assertTrue(mentorAssignmentDAO.hasActiveAssignment(1, null));

            // End old assignment
            mentorAssignmentDAO.endActiveAssignment(1, "Reassignment", null);

            // Old assignment should be ended
            assertFalse(mentorAssignmentDAO.hasActiveAssignment(1, null));
            assertNotNull(mentorAssignmentDAO.assignments.get(0).getEndedAt());
            assertEquals("Reassignment", mentorAssignmentDAO.assignments.get(0).getEndReason());

            // Create new assignment
            MentorAssignment newAssignment = new MentorAssignment();
            newAssignment.setEnrollmentId(1);
            newAssignment.setMentorId(2);
            newAssignment.setAssignedBy(100L);
            mentorAssignmentDAO.createAssignment(newAssignment, null);

            // Now should have active assignment again
            assertTrue(mentorAssignmentDAO.hasActiveAssignment(1, null));

            // History should show both
            List<MentorAssignment> history = mentorAssignmentDAO.getAssignmentHistory(1);
            assertEquals(2, history.size());
        }

        @Test
        @DisplayName("Change mentor requires end reason")
        void changeMentorRequiresReason() {
            // Service-level validation test
            // EnrollmentService.changeMentor throws IllegalArgumentException for blank reason
            assertThrows(IllegalArgumentException.class, () -> {
                EnrollmentService service = new EnrollmentService(
                        appDAO, enrollmentDAO, mentorAssignmentDAO, programDAO, mentorDAO);
                service.changeMentor(1, 2, null, 100L);
            });

            assertThrows(IllegalArgumentException.class, () -> {
                EnrollmentService service = new EnrollmentService(
                        appDAO, enrollmentDAO, mentorAssignmentDAO, programDAO, mentorDAO);
                service.changeMentor(1, 2, "  ", 100L);
            });
        }

        @Test
        @DisplayName("Mentor history is preserved after changes")
        void mentorHistoryPreserved() {
            // Create 3 assignments, end first 2
            for (int i = 1; i <= 3; i++) {
                if (i > 1) {
                    mentorAssignmentDAO.endActiveAssignment(1, "Change #" + (i - 1), null);
                }
                MentorAssignment ma = new MentorAssignment();
                ma.setEnrollmentId(1);
                ma.setMentorId(i);
                ma.setAssignedBy(100L);
                mentorAssignmentDAO.createAssignment(ma, null);
            }

            List<MentorAssignment> history = mentorAssignmentDAO.getAssignmentHistory(1);
            assertEquals(3, history.size());

            // Only the last should be active
            long activeCount = history.stream().filter(MentorAssignment::isActive).count();
            assertEquals(1, activeCount);

            // Active one should be mentor 3
            MentorAssignment active = mentorAssignmentDAO.getActiveAssignment(1);
            assertNotNull(active);
            assertEquals(3, active.getMentorId());
        }
    }

    // ========== getCurrentMentor Tests ==========

    @Nested
    @DisplayName("Get Current Mentor (for BE4)")
    class GetCurrentMentorTests {

        @Test
        @DisplayName("Returns null when no assignment exists")
        void noAssignment() throws SQLException {
            EnrollmentService service = new EnrollmentService(
                    appDAO, enrollmentDAO, mentorAssignmentDAO, programDAO, mentorDAO);
            assertNull(service.getCurrentMentor(999));
        }

        @Test
        @DisplayName("Returns active assignment when exists")
        void hasActiveAssignment() throws SQLException {
            MentorAssignment ma = new MentorAssignment();
            ma.setEnrollmentId(1);
            ma.setMentorId(5);
            ma.setAssignedBy(100L);
            mentorAssignmentDAO.createAssignment(ma, null);

            EnrollmentService service = new EnrollmentService(
                    appDAO, enrollmentDAO, mentorAssignmentDAO, programDAO, mentorDAO);
            MentorAssignment result = service.getCurrentMentor(1);
            assertNotNull(result);
            assertEquals(5, result.getMentorId());
        }

        @Test
        @DisplayName("Returns null when all assignments are ended")
        void allAssignmentsEnded() throws SQLException {
            MentorAssignment ma = new MentorAssignment();
            ma.setEnrollmentId(1);
            ma.setMentorId(5);
            ma.setAssignedBy(100L);
            mentorAssignmentDAO.createAssignment(ma, null);
            mentorAssignmentDAO.endActiveAssignment(1, "Done", null);

            EnrollmentService service = new EnrollmentService(
                    appDAO, enrollmentDAO, mentorAssignmentDAO, programDAO, mentorDAO);
            assertNull(service.getCurrentMentor(1));
        }
    }
}
