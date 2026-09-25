package com.internship.model;

import java.io.Serializable;
import java.sql.Timestamp;

/**
 * Document: Đại diện cho tài liệu (CV, Đơn xin thực tập, ...) của TTS.
 */
public class Document implements Serializable {
    private static final long serialVersionUID = 1L;

    private int documentId;
    private int profileId;
    private String documentType; // 'CV', 'APPLICATION_LETTER', 'OTHER'
    private String fileName;
    private String filePath;
    private long fileSize;
    private String mimeType;
    private String status; // 'PENDING', 'APPROVED', 'REJECTED'
    private Timestamp uploadedAt;

    public Document() {
    }

    public Document(int documentId, int profileId, String documentType, String fileName, 
                    String filePath, long fileSize, String mimeType, String status, Timestamp uploadedAt) {
        this.documentId = documentId;
        this.profileId = profileId;
        this.documentType = documentType;
        this.fileName = fileName;
        this.filePath = filePath;
        this.fileSize = fileSize;
        this.mimeType = mimeType;
        this.status = status;
        this.uploadedAt = uploadedAt;
    }

    public int getDocumentId() {
        return documentId;
    }

    public void setDocumentId(int documentId) {
        this.documentId = documentId;
    }

    public int getProfileId() {
        return profileId;
    }

    public void setProfileId(int profileId) {
        this.profileId = profileId;
    }

    public String getDocumentType() {
        return documentType;
    }

    public void setDocumentType(String documentType) {
        this.documentType = documentType;
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

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Timestamp getUploadedAt() {
        return uploadedAt;
    }

    public void setUploadedAt(Timestamp uploadedAt) {
        this.uploadedAt = uploadedAt;
    }

    @Override
    public String toString() {
        return "Document{" +
                "documentId=" + documentId +
                ", profileId=" + profileId +
                ", documentType='" + documentType + '\'' +
                ", fileName='" + fileName + '\'' +
                ", fileSize=" + fileSize +
                ", status='" + status + '\'' +
                '}';
    }
}
