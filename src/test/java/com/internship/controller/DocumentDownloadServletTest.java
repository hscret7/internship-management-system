package com.internship.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.*;

class DocumentDownloadServletTest {

    @Test
    @DisplayName("generateFallbackPdf creates valid PDF header, catalog, and trailer")
    void testGenerateFallbackPdf() {
        byte[] pdf = DocumentDownloadServlet.generateFallbackPdf("Hop Dong Mau", "sample.pdf");
        assertNotNull(pdf);
        assertTrue(pdf.length > 100);

        String pdfStr = new String(pdf, java.nio.charset.StandardCharsets.ISO_8859_1);
        assertTrue(pdfStr.startsWith("%PDF-1.4"));
        assertTrue(pdfStr.contains("/Type /Catalog"));
        assertTrue(pdfStr.contains("startxref"));
        assertTrue(pdfStr.endsWith("%%EOF\n") || pdfStr.endsWith("%%EOF"));
    }

    @Test
    @DisplayName("Create sample files for seed QA test data in webapp uploads")
    void createSampleFilesForQa() throws IOException {
        Path contractsDir = Paths.get("src/main/webapp/uploads/contracts");
        Path documentsDir = Paths.get("src/main/webapp/uploads/documents");
        Files.createDirectories(contractsDir);
        Files.createDirectories(documentsDir);

        byte[] contractBytes = DocumentDownloadServlet.generateFallbackPdf(
                "Hop Dong Thuc Tap 2026 - Nguyen Van Nam", "HopDongThucTap_NguyenVanNam_2026.pdf");
        Files.write(contractsDir.resolve("HopDongThucTap_NguyenVanNam_2026.pdf"), contractBytes);

        byte[] cvNam = DocumentDownloadServlet.generateFallbackPdf(
                "CV Nguyen Van Nam - Java Backend", "CV_NguyenVanNam_JavaBackend.pdf");
        Files.write(documentsDir.resolve("CV_NguyenVanNam_JavaBackend.pdf"), cvNam);

        byte[] donNam = DocumentDownloadServlet.generateFallbackPdf(
                "Don Xin Thuc Tap - Nguyen Van Nam", "DonXinThucTap_NguyenVanNam.pdf");
        Files.write(documentsDir.resolve("DonXinThucTap_NguyenVanNam.pdf"), donNam);

        byte[] cvHoa = DocumentDownloadServlet.generateFallbackPdf(
                "CV Tran Thi Hoa - Business Analyst", "CV_TranThiHoa_BA.pdf");
        Files.write(documentsDir.resolve("CV_TranThiHoa_BA.pdf"), cvHoa);

        assertTrue(Files.exists(contractsDir.resolve("HopDongThucTap_NguyenVanNam_2026.pdf")));
        assertTrue(Files.exists(documentsDir.resolve("CV_NguyenVanNam_JavaBackend.pdf")));
        assertTrue(Files.exists(documentsDir.resolve("DonXinThucTap_NguyenVanNam.pdf")));
        assertTrue(Files.exists(documentsDir.resolve("CV_TranThiHoa_BA.pdf")));
    }
}
