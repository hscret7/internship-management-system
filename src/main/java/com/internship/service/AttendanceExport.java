package com.internship.service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.format.DateTimeFormatter;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

/** Real XLSX/PDF; one row per BE2 ACTIVE work shift, with no invented percentages. */
public class AttendanceExport {
    static String metadata(AttendanceReportService.Snapshot snapshot) {
        var filter = snapshot.filter();
        return "Ngày làm việc: " + filter.from() + " đến " + filter.to()
                + "; Chương trình: " + (filter.programId() == null ? "Tất cả" : filter.programId())
                + "; Ghi danh: " + (filter.enrollmentId() == null ? "Tất cả" : filter.enrollmentId())
                + "; Xuất lúc: " + EndTermExport.time(snapshot.generatedAt());
    }
    private static final String NOTE = "Nguồn: AttendanceService BE2; mỗi dòng là một ca ACTIVE. "
            + "Giờ công/trạng thái giữ nguyên từ BE2. Nhiều ca cùng ngày có thể dùng cùng bản ghi check-in/out; "
            + "không cộng các dòng để suy ra tổng giờ theo ngày. Chưa check-out: 0 giờ, không suy luận đã hoàn thành ca.";

    public byte[] xlsx(AttendanceReportService.Snapshot snapshot) throws IOException {
        try (var book = new XSSFWorkbook(); var out = new ByteArrayOutputStream()) {
            var sheet = book.createSheet("Chuyên cần");
            var normal = book.createCellStyle();
            var font = book.createFont(); font.setFontName("Arial"); font.setFontHeightInPoints((short)11);
            normal.setFont(font); normal.setWrapText(true); normal.setVerticalAlignment(VerticalAlignment.TOP);
            var heading = book.createCellStyle(); heading.cloneStyleFrom(normal);
            heading.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex()); heading.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            var white = book.createFont(); white.setFontName("Arial"); white.setBold(true); white.setColor(IndexedColors.WHITE.getIndex()); heading.setFont(white);
            var date = style(book, normal, "dd/mm/yyyy");
            var timestamp = style(book, normal, "dd/mm/yyyy hh:mm");
            var hours = style(book, normal, "0.00");
            String[] info = {"BÁO CÁO CHUYÊN CẦN", metadata(snapshot), NOTE};
            for (int i = 0; i < info.length; i++) {
                var row = sheet.createRow(i); row.setHeightInPoints(i == 0 ? 30 : 48);
                text(row, 0, info[i], i == 0 ? heading : normal);
                sheet.addMergedRegion(new CellRangeAddress(i, i, 0, 10));
            }
            String[] labels = {"Mã ghi danh", "Thực tập sinh", "Chương trình", "Ngày làm việc", "Bắt đầu ca", "Kết thúc ca",
                    "Check-in", "Check-out", "Nghỉ được duyệt", "Trạng thái (BE2)", "Giờ công (BE2)"};
            int[] widths = {15, 32, 32, 16, 15, 15, 24, 24, 20, 25, 20};
            var header = sheet.createRow(4); header.setHeightInPoints(38);
            for (int i = 0; i < labels.length; i++) { text(header, i, labels[i], heading); sheet.setColumnWidth(i, widths[i] * 256); }
            int index = 5;
            for (var source : snapshot.rows()) {
                var row = sheet.createRow(index++);
                int lines = Math.max(wrappedLines(source.internName(), 30), wrappedLines(source.programName(), 30));
                row.setHeightInPoints(Math.min(400, Math.max(48, 16 * lines)));
                number(row, 0, source.enrollmentId(), normal);
                text(row, 1, source.internName(), normal); text(row, 2, source.programName(), normal);
                var cell = row.createCell(3); cell.setCellStyle(date); if (source.workDate() != null) cell.setCellValue(source.workDate());
                text(row, 4, value(source.startTime()), normal); text(row, 5, value(source.endTime()), normal);
                cell = row.createCell(6); cell.setCellStyle(timestamp); if (source.checkInTime() != null) cell.setCellValue(source.checkInTime());
                cell = row.createCell(7); cell.setCellStyle(timestamp); if (source.checkOutTime() != null) cell.setCellValue(source.checkOutTime());
                cell = row.createCell(8); cell.setCellStyle(normal); cell.setCellValue(source.approvedLeave());
                text(row, 9, source.status(), normal); number(row, 10, source.workedHours(), hours);
            }
            if (snapshot.rows().isEmpty()) { text(sheet.createRow(5), 0, "Không có ca ACTIVE phù hợp bộ lọc.", normal); sheet.addMergedRegion(new CellRangeAddress(5, 5, 0, 10)); }
            sheet.createFreezePane(0, 5);
            sheet.setAutoFilter(new CellRangeAddress(4, Math.max(4, index - 1), 0, 10));
            sheet.setFitToPage(true); sheet.getPrintSetup().setLandscape(true); sheet.getPrintSetup().setFitWidth((short)1); sheet.getPrintSetup().setFitHeight((short)0);
            sheet.setRepeatingRows(new CellRangeAddress(4, 4, -1, -1));
            book.write(out); return out.toByteArray();
        }
    }

    public byte[] pdf(AttendanceReportService.Snapshot snapshot) throws IOException {
        Path font = Path.of(System.getProperty("be4.pdf.font", "C:/Windows/Fonts/arial.ttf"));
        if (!Files.isRegularFile(font)) throw new IOException("Missing Vietnamese PDF font");
        try (var doc = new PDDocument(); var out = new ByteArrayOutputStream(); var input = Files.newInputStream(font)) {
            var writer = new EndTermExport.PdfLines(doc, PDType0Font.load(doc, input, true));
            writer.line("BÁO CÁO CHUYÊN CẦN", 16); writer.line(metadata(snapshot), 9); writer.line(NOTE, 9);
            writer.line("Số ca ACTIVE phù hợp: " + snapshot.rows().size(), 11);
            int index = 1;
            for (var row : snapshot.rows()) {
                writer.line(index++ + ". " + value(row.internName()) + " — Ghi danh #" + row.enrollmentId(), 12);
                writer.line("Chương trình: " + value(row.programName()) + "; ngày: " + value(row.workDate()), 10);
                writer.line("Ca: " + value(row.startTime()) + " - " + value(row.endTime()) + "; trạng thái BE2: " + value(row.status()), 10);
                writer.line("Check-in: " + time(row.checkInTime()) + "; check-out: " + time(row.checkOutTime()), 10);
                writer.line("Nghỉ được duyệt: " + (row.approvedLeave() ? "Có" : "Không") + "; giờ công BE2: " + String.format(java.util.Locale.ROOT, "%.2f", row.workedHours()), 10);
                writer.line("", 6);
            }
            if (snapshot.rows().isEmpty()) writer.line("Không có ca ACTIVE phù hợp bộ lọc.", 11);
            writer.close(); doc.save(out); return out.toByteArray();
        }
    }

    private static CellStyle style(XSSFWorkbook book, CellStyle base, String format) { var style = book.createCellStyle(); style.cloneStyleFrom(base); style.setDataFormat(book.createDataFormat().getFormat(format)); return style; }
    private static void text(org.apache.poi.ss.usermodel.Row row, int column, String value, CellStyle style) { var cell = row.createCell(column); cell.setCellStyle(style); cell.setCellValue(value == null ? "—" : value); }
    private static void number(org.apache.poi.ss.usermodel.Row row, int column, double value, CellStyle style) { var cell = row.createCell(column); cell.setCellStyle(style); cell.setCellValue(value); }
    private static String value(Object value) { return value == null ? "—" : value.toString(); }
    private static String time(java.time.LocalDateTime value) { return value == null ? "—" : value.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")); }
    private static int wrappedLines(String value, int width) { return value == null ? 1 : java.util.Arrays.stream(value.split("\\R", -1)).mapToInt(line -> Math.max(1, (line.codePointCount(0, line.length()) + width - 1) / width)).sum(); }
}
