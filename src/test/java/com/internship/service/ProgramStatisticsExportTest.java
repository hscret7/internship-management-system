package com.internship.service;
import com.internship.dao.ProgramStatisticsDAO;
import java.util.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.io.*;
import org.junit.jupiter.api.Test;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.ss.usermodel.CellType;
import static org.junit.jupiter.api.Assertions.*;
class ProgramStatisticsExportTest {
    ProgramStatisticsReportService.Snapshot sample(long total){return new ProgramStatisticsReportService.Snapshot(new ProgramStatisticsDAO.Filter(null,null,null,null),new ProgramStatisticsDAO.Summary(1,total,0,total,0,new BigDecimal("100.00"),List.of(new ProgramStatisticsDAO.Group("=HYPERLINK(unsafe)",1,total)),List.of(new ProgramStatisticsDAO.Group("Công nghệ thông tin",1,total))),Instant.parse("2026-10-09T17:00:00Z"));}
    @Test void workbookUsesSameDenominatorAndLiteralGroupLabels()throws Exception {
        try(var book=new XSSFWorkbook(new ByteArrayInputStream(new ProgramStatisticsExport().xlsx(sample(2))))){assertEquals(3,book.getNumberOfSheets());assertEquals(2,book.getSheetAt(0).getRow(3).getCell(1).getNumericCellValue());assertEquals(1,book.getSheetAt(0).getRow(7).getCell(1).getNumericCellValue());var cell=book.getSheetAt(1).getRow(1).getCell(0);assertEquals(CellType.STRING,cell.getCellType());assertTrue(cell.getStringCellValue().startsWith("=HYPERLINK"));}
    }
    @Test void emptySnapshotDoesNotClaimZeroPercentCompletion()throws Exception {
        try(var book=new XSSFWorkbook(new ByteArrayInputStream(new ProgramStatisticsExport().xlsx(sample(0))))){assertEquals("Chưa có dữ liệu",book.getSheetAt(0).getRow(7).getCell(1).getStringCellValue());}
    }
    @Test void pdfUsesVietnameseLabelsAndActualCounts()throws Exception {
        org.junit.jupiter.api.Assumptions.assumeTrue(java.nio.file.Files.isRegularFile(java.nio.file.Path.of(System.getProperty("be4.pdf.font","C:/Windows/Fonts/arial.ttf"))));
        byte[] file=new ProgramStatisticsExport().pdf(sample(2));try(var doc=org.apache.pdfbox.Loader.loadPDF(file)){String text=new org.apache.pdfbox.text.PDFTextStripper().getText(doc);assertTrue(text.contains("2 / 2 = 100.00%"));assertTrue(text.contains("Công nghệ thông tin"));}
    }
}
