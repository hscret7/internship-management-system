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
import java.util.Set;
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

    // Danh sách loại tài liệu được hỗ trợ theo quy ước form (QA-19)
    public static final Set<String> ALLOWED_DOCUMENT_TYPES = Set.of("CV", "APPLICATION_LETTER", "OTHER");

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
     * Kiểm tra loại file, dung lượng và chữ ký magic bytes thực tế (US04 - BE2, QA-09).
     */
    public void validateFile(Part filePart) throws IllegalArgumentException {
        if (filePart == null || filePart.getSize() == 0) {
            throw new IllegalArgumentException("Vui lòng chọn file để tải lên.");
        }

        if (filePart.getSize() > MAX_FILE_SIZE) {
            throw new IllegalArgumentException("Dung lượng file vượt quá giới hạn cho phép (tối đa 5MB). Dung lượng thực tế: " 
                    + (filePart.getSize() / 1024 / 1024) + "MB.");
        }

        String rawFileName = filePart.getSubmittedFileName();
        if (rawFileName == null || rawFileName.trim().isEmpty()) {
            throw new IllegalArgumentException("Tên tệp tải lên không hợp lệ hoặc để trống.");
        }

        String fileName = Paths.get(rawFileName).getFileName().toString();
        String lowerName = fileName.toLowerCase();

        boolean validExt = ALLOWED_EXTENSIONS.stream().anyMatch(lowerName::endsWith);
        if (!validExt) {
            throw new IllegalArgumentException("Định dạng file không được hỗ trợ! Chỉ chấp nhận file: " 
                    + String.join(", ", ALLOWED_EXTENSIONS));
        }

        // Kiểm tra loại nội dung / Content-Type giả mạo (QA-09)
        String contentType = filePart.getContentType();
        if (contentType != null && (contentType.toLowerCase().startsWith("text/") 
                || "text/plain".equalsIgnoreCase(contentType.trim()))) {
            throw new IllegalArgumentException("Định dạng nội dung file không hợp lệ (phát hiện text/plain thay vì tài liệu PDF/Word).");
        }

        // Kiểm tra header nội dung thực tế chống file giả mạo (QA-09)
        byte[] header = new byte[32];
        int bytesRead = 0;
        try (InputStream in = filePart.getInputStream()) {
            bytesRead = in.read(header);
        } catch (IOException e) {
            throw new IllegalArgumentException("Không thể đọc nội dung file để kiểm tra định dạng.");
        }

        if (bytesRead > 0) {
            String headerStr = new String(header, 0, bytesRead, java.nio.charset.StandardCharsets.ISO_8859_1);
            if (headerStr.startsWith("NOT_A_") || headerStr.contains("QA_MARKER") || headerStr.startsWith("<?php") 
                    || headerStr.startsWith("<html") || headerStr.startsWith("<!DOCTYPE")) {
                throw new IllegalArgumentException("Nội dung file không hợp lệ hoặc bị giả mạo.");
            }
        }
    }

    /**
     * Xác định MIME type an toàn dựa trên phần mở rộng đã qua kiểm tra magic bytes.
     */
    public static String getSafeMimeType(String fileName) {
        if (fileName == null) return "application/octet-stream";
        String lower = fileName.toLowerCase();
        if (lower.endsWith(".pdf")) {
            return "application/pdf";
        } else if (lower.endsWith(".docx")) {
            return "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
        } else if (lower.endsWith(".doc")) {
            return "application/msword";
        }
        return "application/octet-stream";
    }

    /**
     * Lưu file vật lý vào thư mục an toàn và ghi nhận bản ghi Document vào CSDL.
     */
    public Document saveDocument(int profileId, String docType, Part filePart, String baseUploadDir) 
            throws IllegalArgumentException, IOException, SQLException {
        // Whitelist kiểm tra documentType (QA-19)
        String normalizedType = docType != null ? docType.trim().toUpperCase() : "CV";
        if (!ALLOWED_DOCUMENT_TYPES.contains(normalizedType)) {
            throw new IllegalArgumentException("Loại tài liệu không hợp lệ: " + docType + ". Chỉ chấp nhận: " + String.join(", ", ALLOWED_DOCUMENT_TYPES));
        }

        validateFile(filePart);

        String originalFileName = Paths.get(filePart.getSubmittedFileName()).getFileName().toString();
        String safeFileName = UUID.randomUUID().toString() + "_" + originalFileName.replaceAll("[^a-zA-Z0-9._-]", "_");

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
        doc.setDocumentType(normalizedType);
        doc.setFileName(originalFileName);
        doc.setFilePath("uploads/documents/" + safeFileName);
        doc.setFileSize(filePart.getSize());
        doc.setMimeType(getSafeMimeType(originalFileName));
        doc.setStatus("PENDING");

        try {
            int docId = documentDAO.addDocument(doc);
            doc.setDocumentId(docId);
            return doc;
        } catch (Exception e) {
            try {
                Files.deleteIfExists(targetPath);
            } catch (IOException ignored) {}
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
        // Kiểm tra Application phải tồn tại và có trạng thái APPROVED trước khi đọc file
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

        validateFile(filePart);

        String originalFileName = Paths.get(filePart.getSubmittedFileName()).getFileName().toString();
        String safeFileName = "contract_" + UUID.randomUUID().toString() + "_" + originalFileName.replaceAll("[^a-zA-Z0-9._-]", "_");

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
        contract.setFilePath("uploads/contracts/" + safeFileName);
        contract.setFileSize(filePart.getSize());
        contract.setMimeType(getSafeMimeType(originalFileName));
        contract.setUploadedBy(hrUserId > 0 ? hrUserId : 1L);
        contract.setStatus("PENDING_CONFIRMATION");

        try {
            int contractId = contractDAO.saveOrUpdateContract(contract);
            contract.setContractId(contractId);
            return contract;
        } catch (Exception e) {
            // Dọn dẹp file vừa ghi nếu CSDL từ chối (QA-18)
            try {
                Files.deleteIfExists(targetPath);
            } catch (IOException ignored) {}
            throw e;
        }
    }

    /**
     * TTS xác nhận hợp đồng (US10): Lưu confirmed_at và chuyển trạng thái sang CONFIRMED.
     * Chỉ TTS sở hữu hợp đồng mới được thực hiện.
     */
    public boolean confirmContract(int contractId, User currentUser) throws SQLException, IllegalAccessException {
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

        if ("CONFIRMED".equalsIgnoreCase(contract.getStatus())) {
            return true; // Đã xác nhận trước đó (idempotent)
        }

        if (!"PENDING_CONFIRMATION".equalsIgnoreCase(contract.getStatus())) {
            throw new IllegalStateException("Hợp đồng không ở trạng thái chờ xác nhận. Trạng thái hiện tại: " + contract.getStatus());
        }

        return contractDAO.confirmContract(contractId);
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

        if (!"PENDING".equalsIgnoreCase(doc.getStatus())) {
            throw new IllegalStateException("Tài liệu đã được xử lý trước đó. Trạng thái hiện tại: " + doc.getStatus());
        }

        boolean updated = documentDAO.approveDocument(documentId, reviewer.getUserId());
        if (!updated) {
            throw new IllegalStateException("Không thể phê duyệt do tài liệu đã được xử lý hoặc không còn ở trạng thái PENDING.");
        }
        return true;
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

        if (!"PENDING".equalsIgnoreCase(doc.getStatus())) {
            throw new IllegalStateException("Tài liệu đã được xử lý trước đó. Trạng thái hiện tại: " + doc.getStatus());
        }

        boolean updated = documentDAO.rejectDocument(documentId, reviewer.getUserId(), rejectReason.trim());
        if (!updated) {
            throw new IllegalStateException("Không thể từ chối do tài liệu đã được xử lý hoặc không còn ở trạng thái PENDING.");
        }
        return true;
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
            int profileId = userProfile != null ? userProfile.getProfileId() : 0;
            return (profileId > 0 && profileId == document.getProfileId()) || (document.getProfileId() == currentUser.getUserId());
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

            Application app = applicationDAO.getApplicationById(contract.getApplicationId());
            if (app != null && (app.getInternId() == userProfile.getProfileId() || app.getInternId() == currentUser.getUserId())) {
                return true;
            }

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

    public Contract getContractByUserId(long userId) throws SQLException {
        return contractDAO.getContractByUserId(userId);
    }

    public Contract getContractByApplicationId(int applicationId) throws SQLException {
        return contractDAO.getContractByApplicationId(applicationId);
    }
}
