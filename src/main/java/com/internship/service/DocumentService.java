package com.internship.service;

import com.internship.dao.ContractDAO;
import com.internship.dao.DocumentDAO;
import com.internship.dao.InternProfileDAO;
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

    // Giới hạn dung lượng: 5MB
    public static final long MAX_FILE_SIZE = 5 * 1024 * 1024; 
    
    // Các phần mở rộng hợp lệ: PDF, Word (DOC, DOCX) theo yêu cầu US04
    public static final List<String> ALLOWED_EXTENSIONS = Arrays.asList(".pdf", ".doc", ".docx");

    public DocumentService() {
        this.documentDAO = new DocumentDAO();
        this.contractDAO = new ContractDAO();
        this.profileDAO = new InternProfileDAO();
    }

    public DocumentService(DocumentDAO documentDAO, ContractDAO contractDAO, InternProfileDAO profileDAO) {
        this.documentDAO = documentDAO;
        this.contractDAO = contractDAO;
        this.profileDAO = profileDAO;
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

        String fileName = Paths.get(filePart.getSubmittedFileName()).getFileName().toString();
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
        doc.setDocumentType(docType);
        doc.setFileName(originalFileName);
        doc.setFilePath("uploads/documents/" + safeFileName);
        doc.setFileSize(filePart.getSize());
        doc.setMimeType(filePart.getContentType() != null ? filePart.getContentType() : "application/octet-stream");
        doc.setStatus("PENDING");

        int docId = documentDAO.addDocument(doc);
        doc.setDocumentId(docId);
        return doc;
    }

    /**
     * Lưu hợp đồng (HR upload) theo US9.
     */
    public Contract saveContract(int applicationId, int hrUserId, Part filePart, String baseUploadDir) 
            throws IllegalArgumentException, IOException, SQLException {
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
        contract.setMimeType(filePart.getContentType() != null ? filePart.getContentType() : "application/pdf");
        contract.setUploadedBy(hrUserId);
        contract.setStatus("PENDING_CONFIRMATION");

        int contractId = contractDAO.saveOrUpdateContract(contract);
        contract.setContractId(contractId);
        return contract;
    }

    /**
     * TTS xác nhận hợp đồng (US10): Lưu confirmed_at và chuyển trạng thái sang CONFIRMED.
     */
    public boolean confirmContract(int contractId, User currentUser) throws SQLException, IllegalAccessException {
        Contract contract = contractDAO.getContractById(contractId);
        if (contract == null) {
            throw new IllegalArgumentException("Không tìm thấy hợp đồng ID: " + contractId);
        }

        if (!canUserAccessContract(currentUser, contract)) {
            throw new IllegalAccessException("Bạn không có quyền xác nhận hợp đồng này.");
        }

        return contractDAO.confirmContract(contractId);
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

        if (currentUser.isHR() || currentUser.isAdmin()) {
            return true;
        }

        if (currentUser.isIntern()) {
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

        if (currentUser.isHR() || currentUser.isAdmin()) {
            return true;
        }

        if (currentUser.isIntern()) {
            InternProfile userProfile = profileDAO.getProfileByUserId(currentUser.getUserId());
            if (userProfile == null) return false;
            Contract userContract = contractDAO.getContractByProfileId(userProfile.getProfileId());
            return userContract != null && userContract.getContractId() == contract.getContractId();
        }

        return false;
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
