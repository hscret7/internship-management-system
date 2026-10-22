package com.internship.service;

import com.internship.dao.ApplicationDAO;
import com.internship.dao.ContractDAO;
import com.internship.dao.DocumentDAO;
import com.internship.dao.InternProfileDAO;
import com.internship.model.Application;
import com.internship.model.Contract;
import com.internship.model.Document;
import com.internship.model.InternProfile;
import com.internship.model.User;
import jakarta.servlet.http.Part;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/**
 * DocumentService: Xử lý upload/download, validation loại file/dung lượng và kiểm tra quyền truy cập (BE2).
 */
public class DocumentService {

    private final DocumentDAO documentDAO;
    private final ContractDAO contractDAO;
    private final InternProfileDAO profileDAO;
    private final ApplicationDAO applicationDAO;

    // Giới hạn dung lượng: 5MB
    public static final long MAX_FILE_SIZE = 5 * 1024 * 1024; 
    
    // Các phần mở rộng hợp lệ: PDF, Word (DOC, DOCX) theo yêu cầu US04
    public static final List<String> ALLOWED_EXTENSIONS = Arrays.asList(".pdf", ".doc", ".docx");

    public DocumentService() {
        this.documentDAO = new DocumentDAO();
        this.contractDAO = new ContractDAO();
        this.profileDAO = new InternProfileDAO();
        this.applicationDAO = new ApplicationDAO();
    }

    public DocumentService(DocumentDAO documentDAO, ContractDAO contractDAO, InternProfileDAO profileDAO) {
        this(documentDAO, contractDAO, profileDAO, new ApplicationDAO());
    }

    public DocumentService(DocumentDAO documentDAO, ContractDAO contractDAO, InternProfileDAO profileDAO, ApplicationDAO applicationDAO) {
        this.documentDAO = documentDAO;
        this.contractDAO = contractDAO;
        this.profileDAO = profileDAO;
        this.applicationDAO = applicationDAO;
    }

    /**
     * Kiểm tra loại file và dung lượng file upload (US04 - BE2).
     */
    public void validateFile(Part filePart) throws IllegalArgumentException {
        if (filePart == null || filePart.getSize() == 0) {
            throw new IllegalArgumentException("Vui lòng chọn file để tải lên.");
        }

        if (filePart.getSize() > MAX_FILE_SIZE) {
            throw new IllegalArgumentException("Dung lượng file vượt quá giới hạn cho phép (tối đa 5MB). Dung lượng thực tế: " 
                    + (filePart.getSize() / 1024 / 1024) + "MB.");
        }

        if (filePart.getSubmittedFileName() == null || filePart.getSubmittedFileName().isBlank()) throw new IllegalArgumentException("Tên file không hợp lệ.");
        String fileName = Paths.get(filePart.getSubmittedFileName()).getFileName().toString();
        if (fileName.length() > 255) throw new IllegalArgumentException("Tên file tối đa 255 ký tự.");
        String lowerName = fileName.toLowerCase();

        boolean validExt = ALLOWED_EXTENSIONS.stream().anyMatch(lowerName::endsWith);
        if (!validExt) {
            throw new IllegalArgumentException("Định dạng file không được hỗ trợ! Chỉ chấp nhận file: " 
                    + String.join(", ", ALLOWED_EXTENSIONS));
        }
    }

    /**
     * Lưu file vật lý vào thư mục an toàn và ghi nhận bản ghi Document vào CSDL.
     */
    public Document saveDocument(int profileId, String docType, Part filePart, String baseUploadDir) 
            throws IllegalArgumentException, IOException, SQLException {
        validateFile(filePart);

        if (docType == null || !List.of("CV", "APPLICATION_LETTER", "OTHER").contains(docType)) throw new IllegalArgumentException("Loại tài liệu không hợp lệ.");

        String originalFileName = Paths.get(filePart.getSubmittedFileName()).getFileName().toString();
        String safeFileName = UUID.randomUUID() + originalFileName.substring(originalFileName.lastIndexOf('.')).toLowerCase(java.util.Locale.ROOT);

        Path uploadPath = Paths.get(baseUploadDir, "documents");
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }

        Path targetPath = uploadPath.resolve(safeFileName);
        try (InputStream input = filePart.getInputStream()) {
            Files.copy(input, targetPath, StandardCopyOption.REPLACE_EXISTING);
        }

        Document doc = new Document();
        doc.setProfileId(profileId);
        doc.setDocumentType(docType);
        doc.setFileName(originalFileName);
        doc.setFilePath("documents/" + safeFileName);
        doc.setFileSize(filePart.getSize());
        doc.setMimeType(filePart.getContentType() != null ? filePart.getContentType() : "application/octet-stream");
        doc.setStatus("PENDING");

        try {
            int docId = documentDAO.addDocument(doc);
            if (docId <= 0) throw new SQLException("Không thể lưu tài liệu.");
            doc.setDocumentId(docId);
            return doc;
        } catch (SQLException | RuntimeException e) {
            // A connection failure may occur after commit. Retain the private UUID file
            // rather than deleting a file that an already-committed row might reference.
            throw e;
        }
    }

    /**
     * Lưu hợp đồng (HR upload) theo US9.
     * Quy ước Sprint 2:
     * - Chỉ upload cho Application có trạng thái APPROVED.
     * - Chỉ thay file trước xác nhận; bản CONFIRMED không bị ghi đè.
     */
    public Contract saveContract(int applicationId, long hrUserId, Part filePart, String baseUploadDir) 
            throws IllegalArgumentException, IllegalStateException, IOException, SQLException {
        validateFile(filePart);

        // Kiểm tra Application phải tồn tại và có trạng thái APPROVED
        Application app = applicationDAO.getApplicationById(applicationId);
        if (app == null) {
            throw new IllegalArgumentException("Không tìm thấy đơn ứng tuyển (applicationId: " + applicationId + ").");
        }
        if (!"APPROVED".equalsIgnoreCase(app.getStatus())) {
            throw new IllegalArgumentException("Chỉ được tải lên hợp đồng cho đơn ứng tuyển đã được duyệt (APPROVED). Trạng thái đơn hiện tại: " + app.getStatus());
        }

        // Kiểm tra hợp đồng hiện có, không cho phép ghi đè bản CONFIRMED
        Contract existing = contractDAO.getContractByApplicationId(applicationId);
        if (existing != null && "CONFIRMED".equalsIgnoreCase(existing.getStatus())) {
            throw new IllegalStateException("Hợp đồng này đã được thực tập sinh xác nhận (CONFIRMED), không thể thay thế hoặc ghi đè trong Sprint 2.");
        }

        String originalFileName = Paths.get(filePart.getSubmittedFileName()).getFileName().toString();
        String safeFileName = "contract_" + UUID.randomUUID() + originalFileName.substring(originalFileName.lastIndexOf('.')).toLowerCase(java.util.Locale.ROOT);

        Path uploadPath = Paths.get(baseUploadDir, "contracts");
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }

        Path targetPath = uploadPath.resolve(safeFileName);
        try (InputStream input = filePart.getInputStream()) {
            Files.copy(input, targetPath, StandardCopyOption.REPLACE_EXISTING);
        }

        Contract contract = new Contract();
        contract.setApplicationId(applicationId);
        contract.setFileName(originalFileName);
        contract.setFilePath("contracts/" + safeFileName);
        contract.setFileSize(filePart.getSize());
        contract.setMimeType(filePart.getContentType() != null ? filePart.getContentType() : "application/pdf");
        contract.setUploadedBy(hrUserId);
        contract.setStatus("PENDING_CONFIRMATION");

        try {
            int contractId = contractDAO.saveOrUpdateContract(contract);
            if (contractId <= 0) throw new SQLException("Không thể lưu hợp đồng.");
            contract.setContractId(contractId);
            return contract;
        } catch (SQLException | RuntimeException e) {
            // Retain on uncertain commit; orphan cleanup is an explicit administrative job.
            throw e;
        }
    }

    /**
     * TTS xác nhận hợp đồng (US10): Lưu confirmed_at và chuyển trạng thái sang CONFIRMED.
     * Chỉ TTS sở hữu hợp đồng mới được thực hiện.
     */
    public boolean confirmContract(int contractId, User currentUser) throws SQLException, IllegalAccessException {
        return confirmContract(contractId, currentUser, null);
    }

    public boolean confirmContract(int contractId, User currentUser, String fileVersion) throws SQLException, IllegalAccessException {
        if (currentUser == null) {
            throw new IllegalAccessException("Vui lòng đăng nhập để thực hiện thao tác.");
        }

        if (!isIntern(currentUser)) {
            throw new IllegalAccessException("Chỉ thực tập sinh sở hữu hợp đồng mới có quyền xác nhận hợp đồng.");
        }

        Contract contract = contractDAO.getContractById(contractId);
        if (contract == null) {
            throw new IllegalArgumentException("Không tìm thấy hợp đồng ID: " + contractId);
        }

        if (!canUserAccessContract(currentUser, contract)) {
            throw new IllegalAccessException("Bạn không có quyền xác nhận hợp đồng này.");
        }

        if (fileVersion != null && !fileVersion.equals(contract.getFileVersion())) {
            throw new IllegalArgumentException("HR đã thay file hợp đồng. Vui lòng tải lại và đọc bản mới trước khi xác nhận.");
        }
        if ("CONFIRMED".equalsIgnoreCase(contract.getStatus())) {
            return true; // Đã xác nhận trước đó
        }

        boolean saved = fileVersion == null ? contractDAO.confirmContract(contractId) : contractDAO.confirmContract(contractId, contract.getFilePath());
        if (saved) return true;
        Contract fresh = contractDAO.getContractById(contractId);
        return fresh != null && "CONFIRMED".equals(fresh.getStatus()) && (fileVersion == null || fileVersion.equals(fresh.getFileVersion()));
    }

    /**
     * HR duyệt tài liệu của TTS (US05 - BE2): PENDING -> APPROVED
     */
    public boolean approveDocument(int documentId, User reviewer) throws SQLException, IllegalAccessException {
        if (!isHR(reviewer) && !isAdmin(reviewer)) {
            throw new IllegalAccessException("Chỉ HR hoặc Admin mới có quyền duyệt tài liệu.");
        }

        Document doc = documentDAO.getDocumentById(documentId);
        if (doc == null) {
            throw new IllegalArgumentException("Không tìm thấy tài liệu ID: " + documentId);
        }

        return documentDAO.approveDocument(documentId, reviewer.getUserId());
    }

    /**
     * HR từ chối tài liệu của TTS (US05 - BE2): PENDING -> REJECTED (Bắt buộc lý do từ chối)
     */
    public boolean rejectDocument(int documentId, User reviewer, String rejectReason) throws SQLException, IllegalAccessException {
        if (!isHR(reviewer) && !isAdmin(reviewer)) {
            throw new IllegalAccessException("Chỉ HR hoặc Admin mới có quyền từ chối tài liệu.");
        }

        if (rejectReason == null || rejectReason.trim().isEmpty()) {
            throw new IllegalArgumentException("Lý do từ chối tài liệu là thông tin bắt buộc.");
        }

        Document doc = documentDAO.getDocumentById(documentId);
        if (doc == null) {
            throw new IllegalArgumentException("Không tìm thấy tài liệu ID: " + documentId);
        }

        if (rejectReason.trim().length() > 10000) throw new IllegalArgumentException("Lý do từ chối tối đa 10000 ký tự.");
        return documentDAO.rejectDocument(documentId, reviewer.getUserId(), rejectReason.trim());
    }

    /**
     * Download tài liệu có kiểm tra quyền (Yêu cầu BE2 - Ảnh 1).
     * - HR/Admin có quyền xem/download mọi tài liệu.
     * - Thực tập sinh chỉ có quyền download tài liệu thuộc hồ sơ của chính mình.
     */
    public boolean canUserAccessDocument(User currentUser, Document document) throws SQLException {
        if (currentUser == null || document == null) {
            return false;
        }

        if (isHR(currentUser) || isAdmin(currentUser)) {
            return true;
        }

        if (isIntern(currentUser)) {
            InternProfile userProfile = profileDAO.getProfileByUserId(currentUser.getUserId());
            return userProfile != null && userProfile.getProfileId() == document.getProfileId();
        }

        return false;
    }

    /**
     * Kiểm tra quyền truy cập Hợp đồng (Yêu cầu BE2 - Hợp đồng - Ảnh 2).
     * "Chỉ HR và TTS sở hữu hợp đồng mới xem/tải/xác nhận được".
     */
    public boolean canUserAccessContract(User currentUser, Contract contract) throws SQLException {
        if (currentUser == null || contract == null) {
            return false;
        }

        if (isHR(currentUser) || isAdmin(currentUser)) {
            return true;
        }

        if (isIntern(currentUser)) {
            InternProfile userProfile = profileDAO.getProfileByUserId(currentUser.getUserId());
            if (userProfile == null) return false;
            Contract userContract = contractDAO.getContractByProfileId(userProfile.getProfileId());
            return userContract != null && userContract.getContractId() == contract.getContractId();
        }

        return false;
    }

    public static boolean isHR(User user) {
        if (user == null || user.getRole() == null || user.getRole().getRoleName() == null) {
            return false;
        }
        String role = user.getRole().getRoleName().trim().toUpperCase();
        return "HR".equals(role) || "ADMIN".equals(role);
    }

    public static boolean isAdmin(User user) {
        if (user == null || user.getRole() == null || user.getRole().getRoleName() == null) {
            return false;
        }
        return "ADMIN".equalsIgnoreCase(user.getRole().getRoleName().trim());
    }

    public static boolean isIntern(User user) {
        if (user == null || user.getRole() == null || user.getRole().getRoleName() == null) {
            return false;
        }
        return "INTERN".equalsIgnoreCase(user.getRole().getRoleName().trim());
    }

    public Document getDocumentById(int documentId) throws SQLException {
        return documentDAO.getDocumentById(documentId);
    }

    public List<Document> getDocumentsByProfileId(int profileId) throws SQLException {
        return documentDAO.getDocumentsByProfileId(profileId);
    }

    public Contract getContractById(int contractId) throws SQLException {
        return contractDAO.getContractById(contractId);
    }

    public Contract getContractByProfileId(int profileId) throws SQLException {
        return contractDAO.getContractByProfileId(profileId);
    }

    public Contract getContractByApplicationId(int applicationId) throws SQLException {
        return contractDAO.getContractByApplicationId(applicationId);
    }
}
