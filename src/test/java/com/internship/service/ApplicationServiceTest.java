package com.internship.service;

import com.internship.dao.ApplicationDAO;
import com.internship.dao.InternProfileDAO;
import com.internship.model.Application;
import com.internship.model.InternProfile;
import com.internship.service.email.EmailService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;
import java.sql.Connection;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for ApplicationService.
 * Uses stub implementations instead of real DAOs/DB to verify business rules.
 */
class ApplicationServiceTest {

    // --- Stub implementations ---

    static class StubApplicationDAO extends ApplicationDAO {
        final List<Application> applications = new CopyOnWriteArrayList<>();
        final AtomicInteger nextId = new AtomicInteger(1);
        boolean approveCalled = false;
        boolean rejectCalled = false;
        // If set, approve/reject return false (simulating already-processed)
        boolean simulateAlreadyProcessed = false;
        StubInternProfileDAO profiles;
        TestConnections connections;
        final ReentrantLock profileLock = new ReentrantLock();

        @Override
        public Integer lockInternProfileId(long userId, Connection conn) {
            profileLock.lock();
            connections.onFinish(profileLock::unlock);
            InternProfile profile = profiles.profile;
            return profile != null && profile.getUserId() == userId ? profile.getProfileId() : null;
        }

        @Override
        public boolean hasPendingApplication(int internId, Connection conn) {
            return hasPendingApplication(internId);
        }

        @Override
        public int createApplication(int internId, Connection conn) {
            return createApplication(internId);
        }

        @Override
        public boolean hasPendingApplication(int internId) {
            return applications.stream()
                    .anyMatch(a -> a.getInternId() == internId && "PENDING".equals(a.getStatus()));
        }

        @Override
        public int createApplication(int internId) {
            Application app = new Application();
            int id = nextId.getAndIncrement();
            app.setApplicationId(id);
            app.setInternId(internId);
            app.setStatus("PENDING");
            app.setSubmittedAt(Timestamp.from(Instant.now()));
            applications.add(app);
            return id;
        }

        @Override
        public Application getApplicationById(int applicationId) {
            return applications.stream()
                    .filter(a -> a.getApplicationId() == applicationId)
                    .findFirst().orElse(null);
        }

        @Override
        public boolean approveApplication(int applicationId, long reviewerId, Connection conn) {
            Application app = getApplicationById(applicationId);
            if (app == null || !"PENDING".equals(app.getStatus())) return false;
            app.setStatus("APPROVED");
            app.setReviewedBy(reviewerId);
            return true;
        }

        @Override
        public boolean rejectApplication(int applicationId, long reviewerId,
                                         String reason, Connection conn) {
            Application app = getApplicationById(applicationId);
            if (app == null || !"PENDING".equals(app.getStatus())) return false;
            app.setStatus("REJECTED");
            app.setReviewedBy(reviewerId);
            app.setRejectReason(reason);
            return true;
        }

        @Override
        public List<Application> getApplicationsByInternId(int internId) {
            return applications.stream()
                    .filter(a -> a.getInternId() == internId)
                    .toList();
        }

        @Override
        public List<Application> getPendingApplications() {
            return applications.stream()
                    .filter(a -> "PENDING".equals(a.getStatus()))
                    .toList();
        }

        @Override
        public List<Application> getAllApplications() {
            return new ArrayList<>(applications);
        }

        void addApplication(Application app) {
            applications.add(app);
        }
    }

    static class StubInternProfileDAO extends InternProfileDAO {
        InternProfile profile;

        @Override
        public InternProfile getProfileByUserId(long userId) {
            if (profile != null && profile.getUserId() == userId) {
                return profile;
            }
            return null;
        }
    }

    static class StubEmailService implements EmailService {
        final List<String> sentEmails = new ArrayList<>();
        boolean shouldThrow = false;
        String lastRecipient;
        String lastSubject;
        String lastContent;

        @Override
        public void sendEmail(String recipient, String subject, String content) {
            if (shouldThrow) {
                throw new RuntimeException("Simulated email failure");
            }
            lastRecipient = recipient;
            lastSubject = subject;
            lastContent = content;
            sentEmails.add(recipient + "|" + subject);
        }
    }

    private StubApplicationDAO appDAO;
    private StubInternProfileDAO profileDAO;
    private StubEmailService emailService;
    private ApplicationService service;
    private TestConnections connections;

    @BeforeEach
    void setUp() {
        appDAO = new StubApplicationDAO();
        profileDAO = new StubInternProfileDAO();
        appDAO.profiles = profileDAO;
        emailService = new StubEmailService();
        connections = new TestConnections();
        appDAO.connections = connections;
        service = new ApplicationService(appDAO, profileDAO, emailService, connections);
    }

    // ========== Submit Application Tests ==========

    @Nested
    @DisplayName("Submit Application")
    class SubmitApplicationTests {

        @Test
        void concurrentSubmissionsProduceOnePendingApplication() throws Exception {
            InternProfile profile = new InternProfile();
            profile.setProfileId(1);
            profile.setUserId(10L);
            profileDAO.profile = profile;
            var pool = Executors.newFixedThreadPool(2);
            var start = new CountDownLatch(1);
            try {
                var first = pool.submit(() -> {
                    start.await();
                    try { service.submitApplication(10L); return true; }
                    catch (IllegalStateException e) { return false; }
                });
                var second = pool.submit(() -> {
                    start.await();
                    try { service.submitApplication(10L); return true; }
                    catch (IllegalStateException e) { return false; }
                });
                start.countDown();
                assertNotEquals(first.get(5, TimeUnit.SECONDS), second.get(5, TimeUnit.SECONDS));
                assertEquals(1, appDAO.applications.size());
            } finally {
                pool.shutdownNow();
            }
        }

        @Test
        @DisplayName("Fails when intern has no profile")
        void submitWithoutProfile() {
            // profileDAO returns null
            assertThrows(IllegalStateException.class,
                    () -> service.submitApplication(999L));
        }

        @Test
        @DisplayName("Fails when intern already has PENDING application")
        void submitWithExistingPending() {
            InternProfile profile = new InternProfile();
            profile.setProfileId(1);
            profile.setUserId(10L);
            profileDAO.profile = profile;

            // Create existing PENDING application
            Application existing = new Application();
            existing.setApplicationId(1);
            existing.setInternId(1);
            existing.setStatus("PENDING");
            appDAO.addApplication(existing);

            assertThrows(IllegalStateException.class,
                    () -> service.submitApplication(10L));
        }

        @Test
        @DisplayName("Succeeds when intern has profile and no PENDING application")
        void submitSuccess() throws SQLException {
            InternProfile profile = new InternProfile();
            profile.setProfileId(1);
            profile.setUserId(10L);
            profileDAO.profile = profile;

            int appId = service.submitApplication(10L);
            assertTrue(appId > 0);
            assertEquals(1, appDAO.applications.size());
            assertEquals("PENDING", appDAO.applications.get(0).getStatus());
        }

        @Test
        @DisplayName("Can submit new application after previous was APPROVED")
        void submitAfterApproved() throws SQLException {
            InternProfile profile = new InternProfile();
            profile.setProfileId(1);
            profile.setUserId(10L);
            profileDAO.profile = profile;

            // Previous application is APPROVED (not PENDING)
            Application previous = new Application();
            previous.setApplicationId(1);
            previous.setInternId(1);
            previous.setStatus("APPROVED");
            appDAO.addApplication(previous);

            int appId = service.submitApplication(10L);
            assertTrue(appId > 0);
        }

        @Test
        @DisplayName("Can submit new application after previous was REJECTED")
        void submitAfterRejected() throws SQLException {
            InternProfile profile = new InternProfile();
            profile.setProfileId(1);
            profile.setUserId(10L);
            profileDAO.profile = profile;

            Application previous = new Application();
            previous.setApplicationId(1);
            previous.setInternId(1);
            previous.setStatus("REJECTED");
            appDAO.addApplication(previous);

            int appId = service.submitApplication(10L);
            assertTrue(appId > 0);
        }
    }

    // ========== Reject Application Validation Tests ==========

    @Nested
    @DisplayName("Reject Application Validation")
    class RejectValidationTests {

        @Test
        @DisplayName("Fails when reject reason is null")
        void rejectWithNullReason() {
            assertThrows(IllegalArgumentException.class,
                    () -> service.rejectApplication(1, 100L, null));
        }

        @Test
        @DisplayName("Fails when reject reason is blank")
        void rejectWithBlankReason() {
            assertThrows(IllegalArgumentException.class,
                    () -> service.rejectApplication(1, 100L, "   "));
        }
    }

    // ========== Email Behavior Tests ==========

    @Nested
    @DisplayName("Email Notification Behavior")
    class EmailTests {

        private Application pending() {
            Application app = new Application();
            app.setApplicationId(123); app.setInternId(1); app.setStatus("PENDING");
            app.setInternEmail("be3-qa@example.invalid"); app.setInternName("TTS kiểm thử");
            appDAO.addApplication(app); return app;
        }

        @Test void approvalEmailContainsCorrectRecipientNameAndApplicationId() throws SQLException {
            Application app = pending();
            service.approveApplication(app.getApplicationId(), 100L);
            assertEquals("be3-qa@example.invalid", emailService.lastRecipient);
            assertTrue(emailService.lastSubject.contains("được duyệt"));
            assertTrue(emailService.lastContent.contains("TTS kiểm thử"));
            assertTrue(emailService.lastContent.contains("123"));
            assertTrue(emailService.lastContent.contains("DUYỆT"));
            assertEquals(1, connections.commits.get());
        }

        @Test void rejectionEmailContainsReasonAndCorrectRecipient() throws SQLException {
            Application app = pending();
            service.rejectApplication(app.getApplicationId(), 100L, " Thiếu CV kiểm thử ");
            assertEquals("be3-qa@example.invalid", emailService.lastRecipient);
            assertTrue(emailService.lastSubject.contains("không được chấp nhận"));
            assertTrue(emailService.lastContent.contains("123"));
            assertTrue(emailService.lastContent.contains("Thiếu CV kiểm thử"));
            assertEquals("REJECTED", app.getStatus());
        }

        @Test void rejectionEmailFailurePreservesDecisionReasonAndCommit() {
            Application app = pending(); emailService.shouldThrow = true;
            assertDoesNotThrow(() -> service.rejectApplication(123, 100L, "Thiếu CV kiểm thử"));
            assertEquals("REJECTED", app.getStatus());
            assertEquals("Thiếu CV kiểm thử", app.getRejectReason());
            assertEquals(100L, app.getReviewedBy());
            assertEquals(1, connections.commits.get()); assertEquals(0, connections.rollbacks.get());
        }

        @Test void emailIsInvokedOnlyAfterSuccessfulCommit() throws SQLException {
            pending();
            ApplicationService ordered = new ApplicationService(appDAO, profileDAO,
                    (recipient, subject, content) -> assertEquals(1, connections.commits.get()), connections);
            ordered.approveApplication(123, 100L);
        }

        @Test
        @DisplayName("Email service failure is logged but does not throw")
        void emailFailureDoesNotThrow() {
            emailService.shouldThrow = true;
            Application app = new Application();
            app.setApplicationId(1);
            app.setInternId(1);
            app.setStatus("PENDING");
            app.setInternEmail("test@example.com");
            app.setInternName("Test Intern");
            appDAO.addApplication(app);
            assertDoesNotThrow(() -> service.approveApplication(1, 100L));
            assertEquals("APPROVED", app.getStatus());
            assertEquals(1, connections.commits.get());
            assertEquals(0, connections.rollbacks.get());
        }

        @Test
        @DisplayName("Stub email service records sent emails")
        void emailServiceRecordsSentEmails() {
            emailService.sendEmail("intern@test.com", "Approval", "You are approved");
            assertEquals(1, emailService.sentEmails.size());
            assertEquals("intern@test.com", emailService.lastRecipient);
            assertTrue(emailService.lastSubject.contains("Approval"));
        }
    }

    // ========== Application Status Tests ==========

    @Nested
    @DisplayName("Application Status Rules")
    class StatusRuleTests {

        @Test
        void decisionCanOnlyBeSavedOnceAndEmailIsNotRepeated() throws SQLException {
            Application pending = new Application();
            pending.setApplicationId(1);
            pending.setInternId(1);
            pending.setStatus("PENDING");
            pending.setInternEmail("intern@test.com");
            pending.setInternName("Intern");
            appDAO.addApplication(pending);

            service.rejectApplication(1, 100L, "Thiếu tài liệu");
            assertEquals("REJECTED", pending.getStatus());
            assertEquals("Thiếu tài liệu", pending.getRejectReason());
            assertEquals(1, emailService.sentEmails.size());
            assertEquals(1, connections.commits.get());

            assertThrows(IllegalStateException.class,
                    () -> service.approveApplication(1, 100L));
            assertEquals(1, emailService.sentEmails.size());
            assertEquals(1, connections.rollbacks.get());
        }

        @Test
        @DisplayName("Only PENDING applications can be approved/rejected")
        void onlyPendingCanBeProcessed() {
            // This test verifies the DAO-level WHERE clause behavior
            Application approved = new Application();
            approved.setApplicationId(1);
            approved.setInternId(1);
            approved.setStatus("APPROVED");
            appDAO.addApplication(approved);

            Application rejected = new Application();
            rejected.setApplicationId(2);
            rejected.setInternId(2);
            rejected.setStatus("REJECTED");
            appDAO.addApplication(rejected);

            // hasPendingApplication should return false for non-PENDING
            assertFalse(appDAO.hasPendingApplication(1));
            assertFalse(appDAO.hasPendingApplication(2));
        }

        @Test
        @DisplayName("getPendingApplications only returns PENDING status")
        void getPendingReturnsOnlyPending() {
            Application pending = new Application();
            pending.setApplicationId(1);
            pending.setInternId(1);
            pending.setStatus("PENDING");
            appDAO.addApplication(pending);

            Application approved = new Application();
            approved.setApplicationId(2);
            approved.setInternId(2);
            approved.setStatus("APPROVED");
            appDAO.addApplication(approved);

            List<Application> pendingApps = appDAO.getPendingApplications();
            assertEquals(1, pendingApps.size());
            assertEquals("PENDING", pendingApps.get(0).getStatus());
        }
    }
}
