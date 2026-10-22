package com.internship.service;

import com.internship.dao.ApplicationDAO;
import com.internship.dao.ContractDAO;
import com.internship.dao.DocumentDAO;
import com.internship.dao.InternProfileDAO;
import com.internship.model.Application;
import com.internship.model.Contract;
import com.internship.model.Document;
import com.internship.model.InternProfile;
import com.internship.model.Role;
import com.internship.model.User;
import jakarta.servlet.http.Part;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests cho DocumentService (US05, US09, US10 - BE2).
 */
class DocumentServiceTest {

    // --- Stub Implementations ---

    static class StubDocumentDAO extends DocumentDAO {
        final List<Document> documents = new ArrayList<>();
        int nextId = 1;

        @Override
        public int addDocument(Document doc) throws SQLException {
            doc.setDocumentId(nextId++);
            documents.add(doc);
            return doc.getDocumentId();
        }

        @Override
        public Document getDocumentById(int documentId) throws SQLException {
            return documents.stream()
                    .filter(d -> d.getDocumentId() == documentId)
                    .findFirst()
                    .orElse(null);
        }

        @Override
        public List<Document> getDocumentsByProfileId(int profileId) throws SQLException {
            List<Document> result = new ArrayList<>();
            for (Document d : documents) {
                if (d.getProfileId() == profileId) {
                    result.add(d);
                }
            }
            return result;
        }

        @Override
        public boolean approveDocument(int documentId, long reviewerId) throws SQLException {
            Document doc = getDocumentById(documentId);
            if (doc != null) {
                doc.setStatus("APPROVED");
                doc.setReviewedBy(reviewerId);
                doc.setReviewedAt(new Timestamp(System.currentTimeMillis()));
                return true;
            }
            return false;
        }

        @Override
        public boolean rejectDocument(int documentId, long reviewerId, String rejectReason) throws SQLException {
            Document doc = getDocumentById(documentId);
            if (doc != null) {
                doc.setStatus("REJECTED");
                doc.setReviewedBy(reviewerId);
                doc.setRejectReason(rejectReason);
                doc.setReviewedAt(new Timestamp(System.currentTimeMillis()));
                return true;
            }
            return false;
        }
    }

    static class StubContractDAO extends ContractDAO {
        final List<Contract> contracts = new ArrayList<>();
        final Map<Integer, Integer> profileToAppMap = new HashMap<>();
        int nextId = 1;

        @Override
        public int saveOrUpdateContract(Contract contract) throws SQLException {
            Contract existing = getContractByApplicationId(contract.getApplicationId());
            if (existing != null) {
                if ("CONFIRMED".equalsIgnoreCase(existing.getStatus())) {
                    throw new IllegalStateException("Hợp đồng này đã được xác nhận (CONFIRMED) và không thể ghi đè.");
                }
                contract.setContractId(existing.getContractId());
                contracts.remove(existing);
                contracts.add(contract);
                return contract.getContractId();
            } else {
                contract.setContractId(nextId++);
                contracts.add(contract);
                return contract.getContractId();
            }
        }

        @Override
        public Contract getContractById(int contractId) throws SQLException {
            return contracts.stream()
                    .filter(c -> c.getContractId() == contractId)
                    .findFirst()
                    .orElse(null);
        }

        @Override
        public Contract getContractByApplicationId(int applicationId) throws SQLException {
            return contracts.stream()
                    .filter(c -> c.getApplicationId() == applicationId)
                    .findFirst()
                    .orElse(null);
        }

        @Override
        public Contract getContractByProfileId(int profileId) throws SQLException {
            Integer appId = profileToAppMap.get(profileId);
            if (appId != null) {
                return getContractByApplicationId(appId);
            }
            return null;
        }

        @Override
        public boolean confirmContract(int contractId) throws SQLException {
            Contract c = getContractById(contractId);
            if (c != null) {
                c.setStatus("CONFIRMED");
                c.setConfirmedAt(new Timestamp(System.currentTimeMillis()));
                return true;
            }
            return false;
        }
    }

    static class StubInternProfileDAO extends InternProfileDAO {
        final List<InternProfile> profiles = new ArrayList<>();

        @Override
        public InternProfile getProfileById(int profileId) throws SQLException {
            return profiles.stream()
                    .filter(p -> p.getProfileId() == profileId)
                    .findFirst()
                    .orElse(null);
        }

        @Override
        public InternProfile getProfileByUserId(long userId) throws SQLException {
            return profiles.stream()
                    .filter(p -> p.getUserId() == userId)
                    .findFirst()
                    .orElse(null);
        }
    }

    static class StubApplicationDAO extends ApplicationDAO {
        final List<Application> applications = new ArrayList<>();

        @Override
        public Application getApplicationById(int applicationId) throws SQLException {
            return applications.stream()
                    .filter(a -> a.getApplicationId() == applicationId)
                    .findFirst()
                    .orElse(null);
        }
    }

    static class DummyPart implements Part {
        private final String fileName;
        private final byte[] content;
        private final String contentType;

        DummyPart(String fileName, byte[] content, String contentType) {
            this.fileName = fileName;
            this.content = content;
            this.contentType = contentType;
        }

        @Override
        public InputStream getInputStream() throws IOException {
            return new ByteArrayInputStream(content);
        }

        @Override
        public String getContentType() {
            return contentType;
        }

        @Override
        public String getName() {
            return "file";
        }

        @Override
        public String getSubmittedFileName() {
            return fileName;
        }

        @Override
        public long getSize() {
            return content != null ? content.length : 0;
        }

        @Override
        public void write(String fileName) throws IOException {
        }

        @Override
        public void delete() throws IOException {
        }

        @Override
        public String getHeader(String name) {
            return null;
        }

        @Override
        public Collection<String> getHeaders(String name) {
            return null;
        }

        @Override
        public Collection<String> getHeaderNames() {
            return null;
        }
    }

    // --- Fixtures & Setup ---

    private StubDocumentDAO documentDAO;
    private StubContractDAO contractDAO;
    private StubInternProfileDAO profileDAO;
    private StubApplicationDAO applicationDAO;
    private DocumentService documentService;

    private User hrUser;
    private User adminUser;
    private User internUser;
    private User otherInternUser;
    private InternProfile internProfile;
    private Application approvedApp;

    @BeforeEach
    void setUp() {
        documentDAO = new StubDocumentDAO();
        contractDAO = new StubContractDAO();
        profileDAO = new StubInternProfileDAO();
        applicationDAO = new StubApplicationDAO();
        documentService = new DocumentService(documentDAO, contractDAO, profileDAO, applicationDAO);

        Role hrRole = new Role();
        hrRole.setRoleId(2L);
        hrRole.setRoleName("HR");
        hrUser = new User();
        hrUser.setUserId(2L);
        hrUser.setRole(hrRole);

        Role adminRole = new Role();
        adminRole.setRoleId(1L);
        adminRole.setRoleName("ADMIN");
        adminUser = new User();
        adminUser.setUserId(1L);
        adminUser.setRole(adminRole);

        Role internRole = new Role();
        internRole.setRoleId(4L);
        internRole.setRoleName("INTERN");
        internUser = new User();
        internUser.setUserId(10L);
        internUser.setRole(internRole);

        Role otherInternRole = new Role();
        otherInternRole.setRoleId(4L);
        otherInternRole.setRoleName("INTERN");
        otherInternUser = new User();
        otherInternUser.setUserId(20L);
        otherInternUser.setRole(otherInternRole);

        internProfile = new InternProfile();
        internProfile.setProfileId(100);
        internProfile.setUserId(10L);
        internProfile.setFullName("Nguyễn Văn A");
        profileDAO.profiles.add(internProfile);

        approvedApp = new Application();
        approvedApp.setApplicationId(50);
        approvedApp.setInternId(100);
        approvedApp.setStatus("APPROVED");
        applicationDAO.applications.add(approvedApp);

        contractDAO.profileToAppMap.put(100, 50);
    }

    // --- US04 & File Validation Tests ---

    @Nested
    @DisplayName("File Validation & Upload Tests")
    class FileValidationTests {

        @Test
        @DisplayName("validateFile throws exception when part is null or empty")
        void testValidateFileNullOrEmpty() {
            assertThrows(IllegalArgumentException.class, () -> documentService.validateFile(null));

            DummyPart emptyPart = new DummyPart("test.pdf", new byte[0], "application/pdf");
            assertThrows(IllegalArgumentException.class, () -> documentService.validateFile(emptyPart));
        }

        @Test
        @DisplayName("validateFile throws exception when file exceeds 5MB")
        void testValidateFileTooLarge() {
            // 5MB + 1 byte
            byte[] oversized = new byte[(int) (DocumentService.MAX_FILE_SIZE + 1)];
            DummyPart largePart = new DummyPart("big.pdf", oversized, "application/pdf");

            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, 
                    () -> documentService.validateFile(largePart));
            assertTrue(ex.getMessage().contains("5MB"));
        }

        @Test
        @DisplayName("validateFile rejects unsupported extensions like .exe, .png, .txt")
        void testValidateFileInvalidExtension() {
            DummyPart exePart = new DummyPart("virus.exe", new byte[]{1, 2, 3}, "application/x-msdownload");
            assertThrows(IllegalArgumentException.class, () -> documentService.validateFile(exePart));

            DummyPart pngPart = new DummyPart("image.png", new byte[]{1, 2, 3}, "image/png");
            assertThrows(IllegalArgumentException.class, () -> documentService.validateFile(pngPart));
        }

        @Test
        @DisplayName("validateFile accepts .pdf, .doc, .docx case-insensitively")
        void testValidateFileValidExtensions() {
            DummyPart pdfPart = new DummyPart("cv.PDF", new byte[]{1, 2, 3}, "application/pdf");
            assertDoesNotThrow(() -> documentService.validateFile(pdfPart));

            DummyPart docPart = new DummyPart("resume.Doc", new byte[]{1, 2, 3}, "application/msword");
            assertDoesNotThrow(() -> documentService.validateFile(docPart));

            DummyPart docxPart = new DummyPart("document.DOCX", new byte[]{1, 2, 3}, "application/vnd.openxmlformats-officedocument.wordprocessingml.document");
            assertDoesNotThrow(() -> documentService.validateFile(docxPart));
        }

        @Test
        @DisplayName("saveDocument saves document record with status PENDING")
        void testSaveDocument(@TempDir Path tempDir) throws Exception {
            DummyPart part = new DummyPart("my_cv.pdf", "sample content".getBytes(), "application/pdf");
            Document doc = documentService.saveDocument(100, "CV", part, tempDir.toString());

            assertNotNull(doc);
            assertEquals(1, doc.getDocumentId());
            assertEquals(100, doc.getProfileId());
            assertEquals("CV", doc.getDocumentType());
            assertEquals("my_cv.pdf", doc.getFileName());
            assertEquals("PENDING", doc.getStatus());
            assertEquals(1, documentDAO.documents.size());
        }
    }

    // --- US05: HR Review Document Tests ---

    @Nested
    @DisplayName("US05: Document Review Tests (Approve/Reject)")
    class DocumentReviewTests {

        private Document pendingDoc;

        @BeforeEach
        void initDoc() {
            pendingDoc = new Document();
            pendingDoc.setDocumentId(1);
            pendingDoc.setProfileId(100);
            pendingDoc.setDocumentType("CV");
            pendingDoc.setFileName("cv.pdf");
            pendingDoc.setStatus("PENDING");
            documentDAO.documents.add(pendingDoc);
        }

        @Test
        @DisplayName("approveDocument succeeds when reviewer is HR or ADMIN")
        void testApproveDocumentByHrOrAdmin() throws Exception {
            boolean resultHr = documentService.approveDocument(1, hrUser);
            assertTrue(resultHr);
            assertEquals("APPROVED", pendingDoc.getStatus());
            assertEquals(2L, pendingDoc.getReviewedBy());
            assertNotNull(pendingDoc.getReviewedAt());

            // Reset and test Admin
            pendingDoc.setStatus("PENDING");
            pendingDoc.setReviewedBy(null);
            boolean resultAdmin = documentService.approveDocument(1, adminUser);
            assertTrue(resultAdmin);
            assertEquals("APPROVED", pendingDoc.getStatus());
            assertEquals(1L, pendingDoc.getReviewedBy());
        }

        @Test
        @DisplayName("approveDocument throws IllegalAccessException if user is not HR/Admin")
        void testApproveDocumentUnauthorized() {
            assertThrows(IllegalAccessException.class, () -> documentService.approveDocument(1, internUser));
            assertThrows(IllegalAccessException.class, () -> documentService.approveDocument(1, null));
        }

        @Test
        @DisplayName("approveDocument throws IllegalArgumentException for non-existent document")
        void testApproveDocumentNotFound() {
            assertThrows(IllegalArgumentException.class, () -> documentService.approveDocument(999, hrUser));
        }

        @Test
        @DisplayName("rejectDocument succeeds with mandatory reason and sets status to REJECTED")
        void testRejectDocumentSuccess() throws Exception {
            boolean result = documentService.rejectDocument(1, hrUser, "Bản CV thiếu số điện thoại liên lạc");
            assertTrue(result);
            assertEquals("REJECTED", pendingDoc.getStatus());
            assertEquals(2L, pendingDoc.getReviewedBy());
            assertEquals("Bản CV thiếu số điện thoại liên lạc", pendingDoc.getRejectReason());
            assertNotNull(pendingDoc.getReviewedAt());
        }

        @Test
        @DisplayName("rejectDocument throws IllegalArgumentException when reason is empty or null")
        void testRejectDocumentMissingReason() {
            IllegalArgumentException exNull = assertThrows(IllegalArgumentException.class, 
                    () -> documentService.rejectDocument(1, hrUser, null));
            assertTrue(exNull.getMessage().contains("bắt buộc"));

            IllegalArgumentException exBlank = assertThrows(IllegalArgumentException.class, 
                    () -> documentService.rejectDocument(1, hrUser, "   "));
            assertTrue(exBlank.getMessage().contains("bắt buộc"));
        }

        @Test
        @DisplayName("rejectDocument throws IllegalAccessException if user is not HR/Admin")
        void testRejectDocumentUnauthorized() {
            assertThrows(IllegalAccessException.class, 
                    () -> documentService.rejectDocument(1, internUser, "Lý do hợp lệ"));
        }
    }

    // --- US09: Contract Management Tests ---

    @Nested
    @DisplayName("US09: Contract Upload & Lifecycle Tests")
    class ContractUploadTests {

        @Test
        @DisplayName("saveContract throws exception if Application does not exist")
        void testSaveContractAppNotFound(@TempDir Path tempDir) {
            DummyPart part = new DummyPart("contract.pdf", "content".getBytes(), "application/pdf");
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, 
                    () -> documentService.saveContract(999, 2L, part, tempDir.toString()));
            assertTrue(ex.getMessage().contains("Không tìm thấy đơn ứng tuyển"));
        }

        @Test
        @DisplayName("saveContract throws exception if Application is not APPROVED (e.g., PENDING or REJECTED)")
        void testSaveContractAppNotApproved(@TempDir Path tempDir) {
            Application pendingApp = new Application();
            pendingApp.setApplicationId(51);
            pendingApp.setStatus("PENDING");
            applicationDAO.applications.add(pendingApp);

            DummyPart part = new DummyPart("contract.pdf", "content".getBytes(), "application/pdf");
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, 
                    () -> documentService.saveContract(51, 2L, part, tempDir.toString()));
            assertTrue(ex.getMessage().contains("APPROVED"));
        }

        @Test
        @DisplayName("saveContract succeeds for APPROVED Application and sets status PENDING_CONFIRMATION")
        void testSaveContractSuccess(@TempDir Path tempDir) throws Exception {
            DummyPart part = new DummyPart("contract.pdf", "sample contract content".getBytes(), "application/pdf");
            Contract contract = documentService.saveContract(50, 2L, part, tempDir.toString());

            assertNotNull(contract);
            assertEquals(50, contract.getApplicationId());
            assertEquals("PENDING_CONFIRMATION", contract.getStatus());
            assertEquals(2L, contract.getUploadedBy());
            assertEquals(1, contractDAO.contracts.size());
        }

        @Test
        @DisplayName("saveContract permits overwriting/updating when existing contract is PENDING_CONFIRMATION")
        void testSaveContractOverwritePending(@TempDir Path tempDir) throws Exception {
            DummyPart part1 = new DummyPart("contract_v1.pdf", "v1".getBytes(), "application/pdf");
            Contract c1 = documentService.saveContract(50, 2L, part1, tempDir.toString());
            assertEquals(1, c1.getContractId());

            DummyPart part2 = new DummyPart("contract_v2.pdf", "v2".getBytes(), "application/pdf");
            Contract c2 = documentService.saveContract(50, 2L, part2, tempDir.toString());
            assertEquals(1, c2.getContractId());
            assertEquals("contract_v2.pdf", c2.getFileName());
            assertEquals(1, contractDAO.contracts.size());
        }

        @Test
        @DisplayName("saveContract throws IllegalStateException if existing contract is CONFIRMED")
        void testSaveContractCannotOverwriteConfirmed(@TempDir Path tempDir) throws Exception {
            Contract confirmedContract = new Contract();
            confirmedContract.setContractId(1);
            confirmedContract.setApplicationId(50);
            confirmedContract.setStatus("CONFIRMED");
            contractDAO.contracts.add(confirmedContract);

            DummyPart part = new DummyPart("contract_new.pdf", "new content".getBytes(), "application/pdf");
            IllegalStateException ex = assertThrows(IllegalStateException.class, 
                    () -> documentService.saveContract(50, 2L, part, tempDir.toString()));
            assertTrue(ex.getMessage().contains("CONFIRMED"));
        }
    }

    // --- US10: Intern Contract Confirmation Tests ---

    @Nested
    @DisplayName("US10: Contract Confirmation Tests")
    class ContractConfirmationTests {

        private Contract pendingContract;

        @BeforeEach
        void initContract() {
            pendingContract = new Contract();
            pendingContract.setContractId(10);
            pendingContract.setApplicationId(50);
            pendingContract.setStatus("PENDING_CONFIRMATION");
            contractDAO.contracts.add(pendingContract);
        }

        @Test
        @DisplayName("confirmContract succeeds when intern owns the contract")
        void testConfirmContractSuccess() throws Exception {
            boolean confirmed = documentService.confirmContract(10, internUser);
            assertTrue(confirmed);
            assertEquals("CONFIRMED", pendingContract.getStatus());
            assertNotNull(pendingContract.getConfirmedAt());
        }

        @Test
        @DisplayName("confirmContract is idempotent if already CONFIRMED")
        void testConfirmContractIdempotent() throws Exception {
            pendingContract.setStatus("CONFIRMED");
            boolean confirmed = documentService.confirmContract(10, internUser);
            assertTrue(confirmed);
        }

        @Test
        @DisplayName("confirmContract throws IllegalAccessException if user is not INTERN")
        void testConfirmContractNonIntern() {
            assertThrows(IllegalAccessException.class, () -> documentService.confirmContract(10, hrUser));
            assertThrows(IllegalAccessException.class, () -> documentService.confirmContract(10, adminUser));
        }

        @Test
        @DisplayName("confirmContract throws IllegalAccessException if intern does not own the contract")
        void testConfirmContractNotOwner() {
            // otherInternUser (userId 20) does not own contract 10 (which belongs to userId 10)
            InternProfile otherProfile = new InternProfile();
            otherProfile.setProfileId(200);
            otherProfile.setUserId(20L);
            profileDAO.profiles.add(otherProfile);

            assertThrows(IllegalAccessException.class, () -> documentService.confirmContract(10, otherInternUser));
        }

        @Test
        @DisplayName("confirmContract throws IllegalArgumentException if contract not found")
        void testConfirmContractNotFound() {
            assertThrows(IllegalArgumentException.class, () -> documentService.confirmContract(999, internUser));
        }
    }

    // --- Access Control Tests ---

    @Nested
    @DisplayName("Document and Contract Access Control Tests")
    class AccessControlTests {

        @Test
        @DisplayName("canUserAccessDocument verifies HR, Admin and owner permissions")
        void testCanUserAccessDocument() throws Exception {
            Document doc = new Document();
            doc.setDocumentId(1);
            doc.setProfileId(100);

            // HR and Admin can access
            assertTrue(documentService.canUserAccessDocument(hrUser, doc));
            assertTrue(documentService.canUserAccessDocument(adminUser, doc));

            // Owner Intern can access
            assertTrue(documentService.canUserAccessDocument(internUser, doc));

            // Other Intern cannot access
            InternProfile otherProfile = new InternProfile();
            otherProfile.setProfileId(200);
            otherProfile.setUserId(20L);
            profileDAO.profiles.add(otherProfile);
            assertFalse(documentService.canUserAccessDocument(otherInternUser, doc));

            // Null checks
            assertFalse(documentService.canUserAccessDocument(null, doc));
            assertFalse(documentService.canUserAccessDocument(internUser, null));
        }

        @Test
        @DisplayName("canUserAccessContract verifies HR, Admin and owner permissions")
        void testCanUserAccessContract() throws Exception {
            Contract contract = new Contract();
            contract.setContractId(10);
            contract.setApplicationId(50);
            contractDAO.contracts.add(contract);

            // HR and Admin can access
            assertTrue(documentService.canUserAccessContract(hrUser, contract));
            assertTrue(documentService.canUserAccessContract(adminUser, contract));

            // Owner Intern can access
            assertTrue(documentService.canUserAccessContract(internUser, contract));

            // Other Intern cannot access
            InternProfile otherProfile = new InternProfile();
            otherProfile.setProfileId(200);
            otherProfile.setUserId(20L);
            profileDAO.profiles.add(otherProfile);
            assertFalse(documentService.canUserAccessContract(otherInternUser, contract));

            // Null checks
            assertFalse(documentService.canUserAccessContract(null, contract));
            assertFalse(documentService.canUserAccessContract(internUser, null));
        }
    }
}
