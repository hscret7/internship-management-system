package com.internship.service;

import java.io.ByteArrayInputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.*;
import java.util.*;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class AttendanceExportTest {
    AttendanceReportService.Snapshot sample(int repetitions) {
        var day = LocalDate.of(2026, 10, 10);
        var rows = new ArrayList<AttendanceReportService.Row>();
        for (int i = 0; i < repetitions; i++) {
            rows.add(new AttendanceReportService.Row(3, "Nguyễn Thị Hoa", "Thực tập Công nghệ thông tin", day,
                    LocalTime.of(8, 0), LocalTime.of(17, 0), day.atTime(8, 5), day.atTime(17, 0), false, "PRESENT", 8.916666666666666));
            rows.add(new AttendanceReportService.Row(4, "=HYPERLINK(unsafe)", "Thực tập Java", day,
                    LocalTime.of(8, 0), LocalTime.of(17, 0), day.atTime(8, 0), null, false, "MISSING_CHECKOUT", 0));
            rows.add(new AttendanceReportService.Row(5, "Trần Văn An", "Thực tập Java", day,
                    LocalTime.of(8, 0), LocalTime.of(17, 0), null, null, true, "LEAVE", 0));
            rows.add(new AttendanceReportService.Row(6, "Lê Văn Bình", "Thực tập Java", day,
                    LocalTime.of(8, 0), LocalTime.of(17, 0), null, null, false, "ABSENT", 0));
        }
        return new AttendanceReportService.Snapshot(new AttendanceReportService.Filter(1, null, day, day), rows, Instant.parse("2026-10-10T10:00:00Z"));
    }
    @Test void realWorkbookHasTypedDataLiteralNamesAndNoFabricatedCheckout() throws Exception {
        byte[] file = new AttendanceExport().xlsx(sample(1));
        try (var book = new XSSFWorkbook(new ByteArrayInputStream(file))) {
            var sheet = book.getSheetAt(0);
            assertEquals(8, sheet.getLastRowNum());
            var row = sheet.getRow(5);
            assertEquals(CellType.NUMERIC, row.getCell(0).getCellType());
            assertTrue(DateUtil.isCellDateFormatted(row.getCell(3)));
            assertEquals(LocalDate.of(2026, 10, 10), row.getCell(3).getLocalDateTimeCellValue().toLocalDate());
            assertTrue(DateUtil.isCellDateFormatted(row.getCell(6)));
            assertEquals(8.916666666666666, row.getCell(10).getNumericCellValue());
            assertEquals(CellType.STRING, sheet.getRow(6).getCell(1).getCellType());
            assertEquals("=HYPERLINK(unsafe)", sheet.getRow(6).getCell(1).getStringCellValue());
            assertEquals(CellType.BLANK, sheet.getRow(6).getCell(7).getCellType());
            assertEquals("MISSING_CHECKOUT", sheet.getRow(6).getCell(9).getStringCellValue());
            assertTrue(sheet.getRow(7).getCell(8).getBooleanCellValue());
            assertEquals(5, sheet.getPaneInformation().getHorizontalSplitPosition());
        }
        if (Boolean.getBoolean("be4.export.samples")) { Files.createDirectories(Path.of("target/export-qa")); Files.write(Path.of("target/export-qa/attendance.xlsx"), file); }
    }
    @Test void emptyWorkbookIsExplainedNotClaimedAsAttendanceZeroPercent() throws Exception {
        try (var book = new XSSFWorkbook(new ByteArrayInputStream(new AttendanceExport().xlsx(sample(0))))) {
            assertTrue(book.getSheetAt(0).getRow(5).getCell(0).getStringCellValue().contains("Không có ca ACTIVE"));
            assertTrue(book.getSheetAt(0).getRow(2).getCell(0).getStringCellValue().contains("không cộng"));
        }
    }
    @Test void pdfPreservesVietnameseStatusesAndEveryRowAcrossPages() throws Exception {
        org.junit.jupiter.api.Assumptions.assumeTrue(Files.isRegularFile(Path.of(System.getProperty("be4.pdf.font", "C:/Windows/Fonts/arial.ttf"))));
        byte[] file = new AttendanceExport().pdf(sample(3));
        try (var doc = Loader.loadPDF(file)) {
            String text = new PDFTextStripper().getText(doc);
            assertTrue(doc.getNumberOfPages() > 1); assertTrue(text.contains("Nguyễn Thị Hoa"));
            assertTrue(text.contains("12. Lê Văn Bình")); assertTrue(text.contains("MISSING_CHECKOUT"));
            assertTrue(text.contains("8.92")); assertTrue(text.contains("Trang 2"));
            if (Boolean.getBoolean("be4.export.samples")) {
                Files.createDirectories(Path.of("target/export-qa")); Files.write(Path.of("target/export-qa/attendance.pdf"), file);
                var renderer = new org.apache.pdfbox.rendering.PDFRenderer(doc);
                for (int i = 0; i < doc.getNumberOfPages(); i++) javax.imageio.ImageIO.write(renderer.renderImageWithDPI(i, 110), "PNG", Path.of("target/export-qa/attendance-page-" + i + ".png").toFile());
            }
        }
    }
}
