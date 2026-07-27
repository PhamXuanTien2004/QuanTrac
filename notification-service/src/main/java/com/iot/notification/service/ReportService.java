package com.iot.notification.service;

import com.iot.notification.dto.ReportRequest;
import com.iot.notification.entity.AlertHistory;
import com.iot.notification.repository.AlertHistoryRepository;
import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.FontFactory;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReportService {

    private final AlertHistoryRepository alertHistoryRepository;
    private final JavaMailSender mailSender;

    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")
            .withZone(ZoneId.of("Asia/Ho_Chi_Minh"));

    private static final DateTimeFormatter fileDateFormatter = DateTimeFormatter.ofPattern("yyyyMMdd")
            .withZone(ZoneId.of("Asia/Ho_Chi_Minh"));

    public void generateAndSendReport(ReportRequest request) {
        log.info("Generating {} report for station {} from {} to {}", request.getFormat(), request.getStationId(), request.getStartDate(), request.getEndDate());
        
        List<AlertHistory> alerts = alertHistoryRepository.findByStationIdAndTimestampBetweenOrderByTimestampAsc(
                request.getStationId(), request.getStartDate(), request.getEndDate());

        try {
            byte[] fileBytes;
            
            String stationStr = request.getStationName() != null ? request.getStationName().replaceAll("[^a-zA-Z0-9_-]", "_") : request.getStationId();
            String startStr = fileDateFormatter.format(request.getStartDate());
            String endStr = fileDateFormatter.format(request.getEndDate());
            String baseFileName = stationStr + "_" + startStr + "_" + endStr;
            
            String fileName;
            String contentType;

            if ("EXCEL".equalsIgnoreCase(request.getFormat())) {
                fileBytes = generateExcel(alerts, request);
                fileName = baseFileName + ".xlsx";
                contentType = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
            } else {
                fileBytes = generatePdf(alerts, request);
                fileName = baseFileName + ".pdf";
                contentType = "application/pdf";
            }

            sendEmailWithAttachment(request.getEmailTo(), fileName, fileBytes, contentType);
            log.info("Report sent to {}", request.getEmailTo());
        } catch (Exception e) {
            log.error("Failed to generate and send report", e);
            throw new RuntimeException("Lỗi xuất báo cáo: " + e.getMessage());
        }
    }

    private byte[] generateExcel(List<AlertHistory> alerts, ReportRequest request) throws Exception {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Alerts Report");

            // Header row
            Row headerRow = sheet.createRow(0);
            String[] columns = {"ID", "Thời gian", "Sensor", "Giá trị", "Đơn vị", "Cảnh báo"};
            for (int i = 0; i < columns.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(columns[i]);
                CellStyle style = workbook.createCellStyle();
                org.apache.poi.ss.usermodel.Font font = workbook.createFont();
                font.setBold(true);
                style.setFont(font);
                cell.setCellStyle(style);
            }

            int rowIdx = 1;
            for (AlertHistory alert : alerts) {
                Row row = sheet.createRow(rowIdx++);
                row.createCell(0).setCellValue(alert.getId());
                row.createCell(1).setCellValue(formatter.format(alert.getTimestamp()));
                row.createCell(2).setCellValue(alert.getSensorType() + " (" + alert.getSensorId().substring(0, 8) + ")");
                row.createCell(3).setCellValue(alert.getValue());
                row.createCell(4).setCellValue(alert.getUnit());
                row.createCell(5).setCellValue(alert.getMessage());
            }

            workbook.write(out);
            return out.toByteArray();
        }
    }

    private byte[] generatePdf(List<AlertHistory> alerts, ReportRequest request) throws Exception {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Document document = new Document();
            PdfWriter.getInstance(document, out);
            document.open();

            com.lowagie.text.Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18);
            Paragraph title = new Paragraph("Bao cao Canh bao (Alerts Report)", titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            document.add(title);
            document.add(new Paragraph("Station ID: " + request.getStationId()));
            document.add(new Paragraph("Tu ngay: " + formatter.format(request.getStartDate())));
            document.add(new Paragraph("Den ngay: " + formatter.format(request.getEndDate())));
            document.add(new Paragraph(" "));

            PdfPTable table = new PdfPTable(6);
            table.setWidthPercentage(100);
            String[] headers = {"ID", "Thoi gian", "Sensor", "Gia tri", "Don vi", "Canh bao"};
            for (String header : headers) {
                PdfPCell cell = new PdfPCell(new Phrase(header, FontFactory.getFont(FontFactory.HELVETICA_BOLD)));
                cell.setHorizontalAlignment(Element.ALIGN_CENTER);
                table.addCell(cell);
            }

            for (AlertHistory alert : alerts) {
                table.addCell(String.valueOf(alert.getId()));
                table.addCell(formatter.format(alert.getTimestamp()));
                table.addCell(alert.getSensorType());
                table.addCell(String.valueOf(alert.getValue()));
                table.addCell(alert.getUnit());
                table.addCell(alert.getMessage());
            }

            document.add(table);
            document.close();
            return out.toByteArray();
        }
    }

    private void sendEmailWithAttachment(String to, String fileName, byte[] fileData, String contentType) throws Exception {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

        helper.setTo(to);
        helper.setSubject("Báo cáo Cảnh báo Trạm Quan Trắc");
        helper.setText("Chào bạn,\n\nĐính kèm là báo cáo lịch sử cảnh báo theo yêu cầu của bạn.\n\nTrân trọng,\nHệ thống Quan Trắc IoT");

        ByteArrayResource resource = new ByteArrayResource(fileData);
        helper.addAttachment(fileName, resource, contentType);

        mailSender.send(message);
    }
}
