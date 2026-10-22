package com.internship.service;

import com.internship.dao.ProgramStatisticsDAO;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.ss.usermodel.*;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import java.io.*;
import java.nio.file.*;
import java.util.*;

/** Literal text cells, typed counts and the same transparent denominator as the BE3 screen. */
public class ProgramStatisticsExport {
    static String metadata(ProgramStatisticsReportService.Snapshot snapshot) {
        var f=snapshot.filter();return "Xuất lúc "+EndTermExport.time(snapshot.generatedAt())+"; Chương trình: "+(f.programId()==null?"Tất cả":f.programId())+"; Trạng thái: "+(f.status()==null?"Tất cả":f.status())+"; Ngày bắt đầu chương trình: "+(f.cohortFrom()==null?"Tất cả":f.cohortFrom()+" → "+f.cohortTo());
    }
    public byte[] xlsx(ProgramStatisticsReportService.Snapshot snapshot)throws IOException {
        try(var book=new XSSFWorkbook();var out=new ByteArrayOutputStream()) {
            var normal=book.createCellStyle();var font=book.createFont();font.setFontName("Arial");font.setFontHeightInPoints((short)11);normal.setFont(font);normal.setWrapText(true);
            var heading=book.createCellStyle();heading.cloneStyleFrom(normal);heading.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());heading.setFillPattern(FillPatternType.SOLID_FOREGROUND);var white=book.createFont();white.setFontName("Arial");white.setBold(true);white.setColor(IndexedColors.WHITE.getIndex());heading.setFont(white);
            var sheet=book.createSheet("Tổng quan");sheet.setColumnWidth(0,256*45);sheet.setColumnWidth(1,256*80);
            String[] labels={"THỐNG KÊ THỰC TẬP",metadata(snapshot),"TTS DISTINCT","Tổng ghi danh (mẫu số)","ACTIVE","COMPLETED (tử số)","CANCELLED","Tỷ lệ COMPLETED / tổng ghi danh"};
            var s=snapshot.summary();Object[] values={null,null,s.interns(),s.enrollments(),s.active(),s.completed(),s.cancelled(),s.enrollments()==0?"Chưa có dữ liệu":s.completionRate().doubleValue()/100};
            for(int i=0;i<labels.length;i++){var row=sheet.createRow(i);row.setHeightInPoints(i==1?65:24);var title=row.createCell(0);title.setCellValue(labels[i]);title.setCellStyle(i==0?heading:normal);if(i==1){sheet.addMergedRegion(new org.apache.poi.ss.util.CellRangeAddress(1,1,0,1));continue;}var cell=row.createCell(1);cell.setCellStyle(normal);if(values[i] instanceof Number n)cell.setCellValue(n.doubleValue());else if(values[i]!=null)cell.setCellValue(values[i].toString());if(i==7&&values[i] instanceof Number){var percent=book.createCellStyle();percent.cloneStyleFrom(normal);percent.setDataFormat(book.createDataFormat().getFormat("0.00%"));cell.setCellStyle(percent);}}
            var explanation=sheet.createRow(9);explanation.createCell(0).setCellValue("TTS đếm DISTINCT theo mã hồ sơ. Một TTS có thể có nhiều ghi danh. Trạng thái/ngày lọc áp dụng cả tử số và mẫu số; không suy luận hoàn thành từ ngày hết hạn.");explanation.getCell(0).setCellStyle(normal);sheet.addMergedRegion(new org.apache.poi.ss.util.CellRangeAddress(9,9,0,1));explanation.setHeightInPoints(65);
            groups(book,"Theo trường",s.universities(),heading,normal);groups(book,"Theo ngành",s.majors(),heading,normal);book.write(out);return out.toByteArray();
        }
    }
    private void groups(XSSFWorkbook book,String name,List<ProgramStatisticsDAO.Group> groups,CellStyle head,CellStyle normal) {
        var sheet=book.createSheet(name);sheet.setColumnWidth(0,256*60);sheet.setColumnWidth(1,256*20);sheet.setColumnWidth(2,256*20);var row=sheet.createRow(0);String[] labels={name,"TTS DISTINCT","Ghi danh"};for(int i=0;i<3;i++){var cell=row.createCell(i);cell.setCellValue(labels[i]);cell.setCellStyle(head);}int index=1;
        for(var g:groups){row=sheet.createRow(index++);row.setHeightInPoints(45);var text=row.createCell(0);text.setCellValue(g.name());text.setCellStyle(normal);row.createCell(1).setCellValue(g.interns());row.createCell(2).setCellValue(g.enrollments());}
        if(groups.isEmpty())sheet.createRow(1).createCell(0).setCellValue("Chưa có dữ liệu.");sheet.createFreezePane(0,1);
    }
    public byte[] pdf(ProgramStatisticsReportService.Snapshot snapshot)throws IOException {
        Path font=Path.of(System.getProperty("be4.pdf.font","C:/Windows/Fonts/arial.ttf"));if(!Files.isRegularFile(font))throw new IOException("Missing Vietnamese PDF font");
        try(var doc=new PDDocument();var out=new ByteArrayOutputStream();var input=Files.newInputStream(font)) {
            var writer=new EndTermExport.PdfLines(doc,PDType0Font.load(doc,input,true));var s=snapshot.summary();writer.line("THỐNG KÊ THỰC TẬP",16);writer.line(metadata(snapshot),9);
            writer.line("TTS DISTINCT: "+s.interns()+"; Ghi danh: "+s.enrollments(),11);writer.line("ACTIVE: "+s.active()+"; COMPLETED: "+s.completed()+"; CANCELLED: "+s.cancelled(),10);
            writer.line("Tỷ lệ hoàn thành: "+(s.enrollments()==0?"Chưa có dữ liệu":s.completed()+" / "+s.enrollments()+" = "+s.completionRate()+"%"),11);
            writer.line("Bộ lọc áp dụng cả tử số và mẫu số. Đếm TTS DISTINCT theo mã hồ sơ, không theo tên.",9);
            for(var entry:Map.of("Theo trường",s.universities(),"Theo ngành",s.majors()).entrySet()){writer.line(entry.getKey(),12);if(entry.getValue().isEmpty())writer.line("Chưa có dữ liệu.",10);for(var g:entry.getValue())writer.line(g.name()+": "+g.interns()+" TTS DISTINCT, "+g.enrollments()+" ghi danh",10);}
            writer.close();doc.save(out);return out.toByteArray();
        }
    }
}
