package com.internship.controller;

import com.internship.model.Contract;
import com.internship.model.Document;
import com.internship.model.User;
import com.internship.service.DocumentService;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.*;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.SQLException;

/**
 * DocumentDownloadServlet: Tải tài liệu và hợp đồng có kiểm tra quyền chặt chẽ (BE2).
 * - Quyền tài liệu: TTS chỉ tải được tài liệu của chính mình; HR/Admin tải được tất cả.
 * - Quyền hợp đồng: Chỉ HR/Admin và TTS sở hữu hợp đồng mới được tải.
 */
@WebServlet(name = "DocumentDownloadServlet", urlPatterns = {"/document/download", "/contract/download"})
public class DocumentDownloadServlet extends HttpServlet {

    private DocumentService documentService;

    @Override
    public void init() throws ServletException {
        this.documentService = new DocumentService();
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        HttpSession session = request.getSession(false);
        User currentUser = (session != null) ? (User) session.getAttribute("user") : null;

        if (currentUser == null) {
            response.sendRedirect(request.getContextPath() + "/auth/login");
            return;
        }

        String path = request.getServletPath();

        try {
            if ("/contract/download".equals(path)) {
                handleDownloadContract(request, response, currentUser);
            } else {
                handleDownloadDocument(request, response, currentUser);
            }
        } catch (SQLException e) {
            log("Lỗi CSDL khi tải tài liệu", e);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Lỗi truy vấn cơ sở dữ liệu.");
        }
    }

    /**
     * Tải tài liệu (CV, Đơn xin thực tập).
     */
    private void handleDownloadDocument(HttpServletRequest request, HttpServletResponse response, User currentUser)
            throws SQLException, IOException {
        String idStr = request.getParameter("id");
        if (idStr == null || idStr.trim().isEmpty()) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Thiếu tham số mã tài liệu.");
            return;
        }

        int documentId = Integer.parseInt(idStr);
        Document doc = documentService.getDocumentById(documentId);

        if (doc == null) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND, "Không tìm thấy tài liệu yêu cầu.");
            return;
        }

        // Kiểm tra quyền (BE2 Requirement)
        if (!documentService.canUserAccessDocument(currentUser, doc)) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Bạn không có quyền tải tài liệu này.");
            return;
        }

        streamFile(request, response, doc.getFilePath(), doc.getFileName(), doc.getMimeType());
    }

    /**
     * Tải Hợp đồng thực tập (US9 & US10).
     */
    private void handleDownloadContract(HttpServletRequest request, HttpServletResponse response, User currentUser)
            throws SQLException, IOException {
        String idStr = request.getParameter("id");
        if (idStr == null || idStr.trim().isEmpty()) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Thiếu mã hợp đồng.");
            return;
        }

        int contractId = Integer.parseInt(idStr);
        Contract contract = documentService.getContractById(contractId);

        if (contract == null) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND, "Không tìm thấy hợp đồng yêu cầu.");
            return;
        }

        // Kiểm tra quyền (Chỉ HR/Admin và TTS sở hữu hợp đồng)
        if (!documentService.canUserAccessContract(currentUser, contract)) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Bạn không có quyền truy cập hợp đồng này.");
            return;
        }

        streamFile(request, response, contract.getFilePath(), contract.getFileName(), contract.getMimeType());
    }

    /**
     * Đọc file vật lý và ghi ra HttpServletResponse OutputStream.
     */
    private void streamFile(HttpServletRequest request, HttpServletResponse response, 
                           String relativePath, String originalFileName, String mimeType) 
            throws IOException {
        String baseUploadDir = getServletContext().getRealPath("/");
        if (baseUploadDir == null) {
            baseUploadDir = System.getProperty("user.home") + File.separator + "internship_uploads";
        }

        Path filePath = Paths.get(baseUploadDir, relativePath);
        if (!Files.exists(filePath)) {
            // Thử kiểm tra ở thư mục uploads dự phòng
            Path altPath = Paths.get(System.getProperty("user.home"), "internship_uploads", relativePath.replace("uploads/", ""));
            if (Files.exists(altPath)) {
                filePath = altPath;
            } else {
                response.sendError(HttpServletResponse.SC_NOT_FOUND, "File vật lý không tồn tại trên hệ thống lưu trữ.");
                return;
            }
        }

        response.setContentType(mimeType != null ? mimeType : "application/octet-stream");
        response.setContentLengthLong(Files.size(filePath));

        String encodedFileName = URLEncoder.encode(originalFileName, StandardCharsets.UTF_8.toString()).replaceAll("\\+", "%20");
        response.setHeader("Content-Disposition", "attachment; filename=\"" + originalFileName + "\"; filename*=UTF-8''" + encodedFileName);

        try (InputStream in = Files.newInputStream(filePath);
             OutputStream out = response.getOutputStream()) {
            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = in.read(buffer)) != -1) {
                out.write(buffer, 0, bytesRead);
            }
            out.flush();
        }
    }
}
