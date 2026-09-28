package com.internship.model;

import java.io.Serializable;
import java.sql.Timestamp;

/**
 * Contract: Thực thể Hợp đồng thực tập (Phạm vi bổ sung của BE2 theo US9 & US10).
 * Lưu ý theo tài liệu: "Xác nhận" ở đây là ghi nhận confirmed_at trên hệ thống, không mặc định là chữ ký số.
 */
public class Contract implements Serializable {
    private static final long serialVersionUID = 1L;

    private int contractId;
    private int applicationId;
    private String fileName;
    private String filePath;
    private long fileSize;
    private String mimeType;
    private long uploadedBy; // HR user_id
    private Timestamp uploadedAt;
    private String status; // 'PENDING_CONFIRMATION', 'CONFIRMED'
    private Timestamp confirmedAt;

    public Contract() {
    }

    public Contract(int contractId, int applicationId, String fileName, String filePath, 
                    long fileSize, String mimeType, long uploadedBy, Timestamp uploadedAt, 
                    String status, Timestamp confirmedAt) {
        this.contractId = contractId;
        this.applicationId = applicationId;
        this.fileName = fileName;
        this.filePath = filePath;
        this.fileSize = fileSize;
        this.mimeType = mimeType;
        this.uploadedBy = uploadedBy;
        this.uploadedAt = uploadedAt;
        this.status = status;
        this.confirmedAt = confirmedAt;
    }

    public int getContractId() {
        return contractId;
    }

    public void setContractId(int contractId) {
        this.contractId = contractId;
    }

    public int getApplicationId() {
        return applicationId;
    }

    public void setApplicationId(int applicationId) {
        this.applicationId = applicationId;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public long getFileSize() {
        return fileSize;
    }

    public void setFileSize(long fileSize) {
        this.fileSize = fileSize;
    }

    public String getMimeType() {
        return mimeType;
    }

    public void setMimeType(String mimeType) {
        this.mimeType = mimeType;
    }

    public long getUploadedBy() {
        return uploadedBy;
    }

    public void setUploadedBy(long uploadedBy) {
        this.uploadedBy = uploadedBy;
    }

    public Timestamp getUploadedAt() {
        return uploadedAt;
    }

    public void setUploadedAt(Timestamp uploadedAt) {
        this.uploadedAt = uploadedAt;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Timestamp getConfirmedAt() {
        return confirmedAt;
    }

    public void setConfirmedAt(Timestamp confirmedAt) {
        this.confirmedAt = confirmedAt;
    }

    @Override
    public String toString() {
        return "Contract{" +
                "contractId=" + contractId +
                ", applicationId=" + applicationId +
                ", fileName='" + fileName + '\'' +
                ", status='" + status + '\'' +
                ", confirmedAt=" + confirmedAt +
                '}';
    }
}
