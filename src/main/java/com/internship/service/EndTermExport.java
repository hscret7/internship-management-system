package com.internship.service;

import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.*;
import org.apache.pdfbox.pdmodel.*;
import org.apache.pdfbox.pdmodel.font.*;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import java.io.*;
import java.nio.file.*;
import java.time.*;
import java.util.*;

/** Native Java exports. String cells are never treated as formulas. PDF embeds a configured TTF. */
public class EndTermExport {
    public byte[] xlsx(EndTermReportService.Page data)throws IOException {
        try(var book=new XSSFWorkbook();var out=new ByteArrayOutputStream()) {
            var sheet=book.createSheet("Báo cáo cuối kỳ");sheet.setDisplayGridlines(false);
            var font=book.createFont();font.setFontName("Arial");font.setFontHeightInPoints((short)10);
            var normal=book.createCellStyle();normal.setFont(font);normal.setVerticalAlignment(VerticalAlignment.CENTER);
            var wrap=book.createCellStyle();wrap.cloneStyleFrom(normal);wrap.setWrapText(true);
            var head=book.createCellStyle();head.cloneStyleFrom(wrap);head.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());head.setFillPattern(FillPatternType.SOLID_FOREGROUND);head.setAlignment(HorizontalAlignment.CENTER);
            var white=book.createFont();white.setFontName("Arial");white.setFontHeightInPoints((short)10);white.setBold(true);white.setColor(IndexedColors.WHITE.getIndex());head.setFont(white);
            var date=book.createCellStyle();date.cloneStyleFrom(normal);date.setDataFormat(book.createDataFormat().getFormat("dd/mm/yyyy"));
            var timestamp=book.createCellStyle();timestamp.cloneStyleFrom(normal);timestamp.setDataFormat(book.createDataFormat().getFormat("dd/mm/yyyy hh:mm"));
            var title=book.createCellStyle();title.cloneStyleFrom(normal);var large=book.createFont();large.setFontName("Arial");large.setBold(true);large.setFontHeightInPoints((short)14);title.setFont(large);
            sheet.createRow(1).createCell(0).setCellValue("Báo cáo cuối kỳ");sheet.getRow(1).getCell(0).setCellStyle(title);
            sheet.createRow(2).createCell(0).setCellValue(metadata(data));sheet.createRow(3).createCell(0).setCellValue("Nguồn: dữ liệu hệ thống tại thời điểm xuất; mỗi dòng là một ghi danh.");
            String[] labels={"Mã ghi danh","Thực tập sinh","Trường","Ngành","Chương trình","Trạng thái","Bắt đầu","Kết thúc","Mentor","Task","Đã xong","Báo cáo đã nộp","Đã phản hồi","Kỹ năng /5","Thái độ /5","Người đánh giá","Ngày đánh giá","Nhận xét","Điểm cũ /100"};
            var header=sheet.createRow(5);header.setHeightInPoints(32);for(int i=0;i<labels.length;i++){var cell=header.createCell(i);cell.setCellValue(labels[i]);cell.setCellStyle(head);sheet.setColumnWidth(i,256*(i==17?55:i==1||i==2||i==4||i==15?24:17));}
            int index=6;
            for(var r:data.items()){
                var a=r.assessments().isEmpty()?null:r.assessments().getFirst();
                String comment=a==null?null:a.comment();if(comment!=null&&comment.length()>180)comment=comment.substring(0,180)+"... (đầy đủ: Lịch sử đánh giá)";
                Object[] values={r.enrollmentId(),r.internName(),r.university(),r.major(),r.programName(),r.status(),r.startDate(),r.endDate(),r.mentorName(),r.taskCount(),r.tasksDone(),r.submittedReports(),r.reviewedReports(),a==null?null:a.skillsScore(),a==null?null:a.attitudeScore(),a==null?null:a.mentorName(),a==null?null:a.createdAt(),comment,r.legacyScore()};
                var row=sheet.createRow(index++);row.setHeightInPoints(60);for(int i=0;i<values.length;i++){var cell=row.createCell(i);Object v=values[i];cell.setCellStyle(v instanceof Number?normal:wrap);if(v instanceof Number n)cell.setCellValue(n.doubleValue());else if(v instanceof LocalDate d){cell.setCellValue(d);cell.setCellStyle(date);}else if(v instanceof Instant d){cell.setCellValue(d.atZone(ZoneId.of("Asia/Ho_Chi_Minh")).toLocalDateTime());cell.setCellStyle(timestamp);}else if(v!=null)cell.setCellValue(v.toString());}
            }
            if(data.items().isEmpty())sheet.createRow(index).createCell(0).setCellValue("Không có ghi danh phù hợp bộ lọc.");
            sheet.createFreezePane(2,6);sheet.setAutoFilter(new CellRangeAddress(5,Math.max(5,index-1),0,labels.length-1));sheet.setRepeatingRows(new CellRangeAddress(5,5,-1,-1));
            var history=book.createSheet("Lịch sử đánh giá");history.setDisplayGridlines(false);
            String[] historyLabels={"Mã ghi danh","Mã đánh giá","Mentor","Kỹ năng /5","Thái độ /5","Ngày","Nhận xét"};
            var hr=history.createRow(0);for(int i=0;i<historyLabels.length;i++){var cell=hr.createCell(i);cell.setCellValue(historyLabels[i]);cell.setCellStyle(head);history.setColumnWidth(i,256*(i==6?60:i==2?25:18));}
            int h=1;for(var r:data.items())for(var a:r.assessments())for(String part:commentParts(a.comment())){var row=history.createRow(h++);Object[] values={r.enrollmentId(),String.valueOf(a.assessmentId()),a.mentorName(),a.skillsScore(),a.attitudeScore(),a.createdAt(),part};for(int i=0;i<values.length;i++){var cell=row.createCell(i);cell.setCellStyle(i==6?wrap:normal);if(values[i] instanceof Number n)cell.setCellValue(n.doubleValue());else if(values[i] instanceof Instant d){cell.setCellValue(d.atZone(ZoneId.of("Asia/Ho_Chi_Minh")).toLocalDateTime());cell.setCellStyle(timestamp);}else cell.setCellValue(String.valueOf(values[i]));}row.setHeightInPoints(Math.min(350,Math.max(45,18*(part.length()/50+2+part.chars().filter(ch->ch=='\n').count()))));}history.createFreezePane(0,1);
            book.write(out);return out.toByteArray();
        }
    }
    public byte[] pdf(EndTermReportService.Page data)throws IOException {
        String fontPath=System.getProperty("be4.pdf.font", "C:/Windows/Fonts/arial.ttf");
        if(!Files.isRegularFile(Path.of(fontPath)))throw new IOException("Configure -Dbe4.pdf.font to a Vietnamese-capable TTF font.");
        try(var doc=new PDDocument();var out=new ByteArrayOutputStream();var fontInput=Files.newInputStream(Path.of(fontPath))) {
            var font=PDType0Font.load(doc,fontInput,true);var writer=new PdfLines(doc,font);
            writer.line("BÁO CÁO CUỐI KỲ",16);writer.line(metadata(data),9);writer.line("Mỗi mục là một ghi danh. Điểm cũ /100 không quy đổi sang tiêu chí /5.",9);
            if(data.items().isEmpty())writer.line("Không có ghi danh phù hợp bộ lọc.",11);
            for(var r:data.items()) {
                writer.line("#"+r.enrollmentId()+" - "+r.internName()+" - "+r.status(),12);
                writer.line("Trường: "+nullable(r.university())+"; Ngành: "+nullable(r.major()),10);
                writer.line("Chương trình: "+r.programName()+" ("+r.startDate()+" - "+r.endDate()+")",10);
                writer.line("Mentor: "+nullable(r.mentorName())+"; Task hoàn thành: "+r.tasksDone()+"/"+r.taskCount(),10);
                writer.line("Báo cáo đã nộp: "+r.submittedReports()+"; đã phản hồi: "+r.reviewedReports(),10);
                if(r.legacyScore()!=null)writer.line("Đánh giá cũ: "+r.legacyScore()+"/100",10);
                if(r.assessments().isEmpty())writer.line("Chưa có đánh giá hai tiêu chí.",10);
                for(var a:r.assessments()){writer.line("Đánh giá #"+a.assessmentId()+" - "+a.mentorName()+" - "+time(a.createdAt()),10);writer.line("Kỹ năng: "+a.skillsScore()+"/5; Thái độ: "+a.attitudeScore()+"/5",10);writer.line(a.comment(),10);}
                writer.line("",10);
            }
            writer.close();doc.save(out);return out.toByteArray();
        }
    }
    static String time(Instant instant){return java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm 'UTC+7'").withZone(ZoneId.of("Asia/Ho_Chi_Minh")).format(instant);}
    static String metadata(EndTermReportService.Page data){return "Xuất lúc "+time(data.generatedAt())+"; Chương trình: "+nullable(data.filter().programId())+"; Ghi danh: "+nullable(data.filter().enrollmentId())+"; Trạng thái: "+nullable(data.filter().status())+"; Số ghi danh: "+data.items().size();}
    private static String nullable(Object v){return v==null?"Tất cả / chưa khai báo":v.toString();}
    // Split long comments into continuation rows with the same IDs, keeping all characters.
    private static List<String> commentParts(String text){List<String> parts=new ArrayList<>();StringBuilder part=new StringBuilder();int lines=0;for(int cp:text.codePoints().toArray()){part.appendCodePoint(cp);if(cp=='\n')lines++;if(part.length()>=640||lines>=15){parts.add(part.toString());part.setLength(0);lines=0;}}if(!part.isEmpty()||parts.isEmpty())parts.add(part.toString());return parts;}
    static class PdfLines implements AutoCloseable {
        final PDDocument doc;final PDType0Font font;PDPageContentStream stream;float y;
        PdfLines(PDDocument doc,PDType0Font font)throws IOException{this.doc=doc;this.font=font;page();}
        void page()throws IOException{if(stream!=null)stream.close();var page=new PDPage(PDRectangle.A4);doc.addPage(page);stream=new PDPageContentStream(doc,page);y=page.getMediaBox().getHeight()-45;draw("Internship Management System",9,45,y);y-=25;draw("Trang "+doc.getNumberOfPages(),9,45,25);}
        void draw(String line,float size,float x,float top)throws IOException{stream.beginText();stream.setFont(font,size);stream.newLineAtOffset(x,top);stream.showText(line);stream.endText();}
        void line(String raw,float size)throws IOException {
            String clean=raw==null?"":raw.replaceAll("[\\p{Cntrl}&&[^\\n]]", " ");
            for(String paragraph:clean.split("\\n",-1)) {
                StringBuilder chunk=new StringBuilder();
                for(int point:paragraph.codePoints().toArray()) {
                    String next=chunk.toString()+new String(Character.toChars(point));
                    if(font.getStringWidth(next)/1000*size>505 && !chunk.isEmpty()){emit(chunk.toString(),size);chunk.setLength(0);}
                    chunk.appendCodePoint(point);
                }
                emit(chunk.toString(),size);
            }
        }
        void emit(String line,float size)throws IOException{if(y-size*1.5<50)page();draw(line,size,45,y);y-=size*1.5;}
        public void close()throws IOException{if(stream!=null)stream.close();}
    }
}
