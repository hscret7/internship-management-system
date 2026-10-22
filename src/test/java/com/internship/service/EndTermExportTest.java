package com.internship.service;
import com.internship.dao.Sprint3WorkflowDAO;
import org.junit.jupiter.api.Test;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.text.PDFTextStripper;
import java.io.*;
import java.nio.file.*;
import java.time.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
class EndTermExportTest {
    EndTermReportService.Page sample(int count){var a=new Sprint3WorkflowDAO.Assessment(1,2,3,"Nguyễn Văn Sơn",4,5,"Hoàn thành tốt. "+"Nội dung nhận xét đầy đủ. ".repeat(40),Instant.parse("2026-10-09T17:00:00Z"));var r=new EndTermReportService.Row(2,1,4,"=HYPERLINK(\"unsafe\")","Đại học ICTU","Công nghệ thông tin","Thực tập Java","COMPLETED",LocalDate.of(2026,9,1),LocalDate.of(2026,10,1),"Nguyễn Văn Sơn",3,2,4,3,List.of(a),null);return new EndTermReportService.Page(Collections.nCopies(count,r),1,count,false,Instant.parse("2026-10-09T17:00:00Z"),new EndTermReportService.Filter(1,null,null));}
    @Test void excelIsRealWorkbookWithTypedCountsAndSafeLiteralUserText()throws Exception{var file=new EndTermExport().xlsx(sample(1));try(var book=new XSSFWorkbook(new ByteArrayInputStream(file))){var cell=book.getSheetAt(0).getRow(6).getCell(1);assertEquals(CellType.STRING,cell.getCellType());assertTrue(cell.getStringCellValue().startsWith("=HYPERLINK"));assertEquals(CellType.NUMERIC,book.getSheetAt(0).getRow(6).getCell(9).getCellType());assertEquals(3,book.getSheetAt(0).getRow(6).getCell(9).getNumericCellValue());assertEquals(2,book.getNumberOfSheets());}if(Boolean.getBoolean("be4.export.samples")){Files.createDirectories(Path.of("target/export-qa"));Files.write(Path.of("target/export-qa/end-term.xlsx"),file);}}
    @Test void emptyExcelStillContainsTitleAndExplanation()throws Exception{try(var book=new XSSFWorkbook(new ByteArrayInputStream(new EndTermExport().xlsx(sample(0))))){assertEquals("Báo cáo cuối kỳ",book.getSheetAt(0).getRow(1).getCell(0).getStringCellValue());assertTrue(book.getSheetAt(0).getRow(6).getCell(0).getStringCellValue().contains("Không có"));}}
    @Test void pdfVietnameseAndPagination()throws Exception{org.junit.jupiter.api.Assumptions.assumeTrue(Files.isRegularFile(Path.of(System.getProperty("be4.pdf.font","C:/Windows/Fonts/arial.ttf"))),"PDF font missing");var file=new EndTermExport().pdf(sample(4));try(var doc=Loader.loadPDF(file)){assertTrue(doc.getNumberOfPages()>1);var text=new PDFTextStripper().getText(doc);assertTrue(text.contains("Nguyễn Văn Sơn"));assertTrue(text.contains("Công nghệ thông tin"));assertTrue(text.contains("Kỹ năng: 4/5"));if(Boolean.getBoolean("be4.export.samples")){Files.createDirectories(Path.of("target/export-qa"));var renderer=new org.apache.pdfbox.rendering.PDFRenderer(doc);for(int i=0;i<doc.getNumberOfPages();i++)javax.imageio.ImageIO.write(renderer.renderImageWithDPI(i,110),"PNG",Path.of("target/export-qa/pdf-page-"+i+".png").toFile());}}if(Boolean.getBoolean("be4.export.samples"))Files.write(Path.of("target/export-qa/end-term.pdf"),file);}
}
