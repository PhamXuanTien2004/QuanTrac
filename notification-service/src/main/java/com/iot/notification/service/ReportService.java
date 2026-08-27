package com.iot.notification.service;

import com.iot.notification.dto.ReportRequest;
import com.iot.notification.dto.TelemetryResponse;
import com.iot.notification.entity.AqiHistory;
import com.iot.notification.repository.AqiHistoryRepository;
import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.FontFactory;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import jakarta.mail.internet.MimeMessage;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReportService {

    private final AqiHistoryRepository aqiHistoryRepository;
    private final InfluxDbService influxDbService;
    private final JavaMailSender mailSender;

    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")
            .withZone(ZoneId.of("Asia/Ho_Chi_Minh"));

    private static final DateTimeFormatter fileDateFormatter = DateTimeFormatter.ofPattern("yyyyMMdd")
            .withZone(ZoneId.of("Asia/Ho_Chi_Minh"));

    @Data
    public static class HourlyReportRow {
        private LocalDateTime hour;
        private Integer aqi;
        private String level;
        private String mainPollutant;
        private Map<String, Double> sensorValues = new HashMap<>();

        public HourlyReportRow(LocalDateTime hour) {
            this.hour = hour;
        }
    }

    private String normalizeSensorName(String rawName) {
        if (rawName == null)
            return "UNKNOWN";
        String upper = rawName.toUpperCase();
        if (upper.contains("PM2.5") || upper.contains("PM25"))
            return "PM2.5";
        if (upper.contains("PM10"))
            return "PM10";
        if (upper.contains("CO"))
            return "CO";
        if (upper.contains("SO2"))
            return "SO2";
        if (upper.contains("NO2"))
            return "NO2";
        if (upper.contains("O3") || upper.contains("OZ") || upper.contains("OZONE"))
            return "O3";
        if (upper.contains("TEMP") || upper.contains("NHIỆT ĐỘ") || upper.contains("NHIET DO"))
            return "TEMP";
        if (upper.contains("HUMI") || upper.contains("ĐỘ ẨM") || upper.contains("DO AM"))
            return "HUMIDITY";
        return rawName.length() > 15 ? rawName.substring(0, 15) + "..." : rawName;
    }

    public void generateAndSendReport(ReportRequest request) {
        log.info("Generating {} report for station {} from {} to {}", request.getFormat(), request.getStationId(),
                request.getStartDate(), request.getEndDate());

        // 1. Lấy dữ liệu AQI
        List<AqiHistory> aqiList = aqiHistoryRepository.findByStationIdAndCalculatedAtBetweenOrderByCalculatedAtAsc(
                request.getStationId(),
                request.getStartDate(),
                request.getEndDate());

        // 2. Lấy dữ liệu cảm biến từ InfluxDB (trung bình theo giờ)
        List<TelemetryResponse> telemetryList = influxDbService.getHourlyAggregatedTelemetry(
                request.getStationId(),
                request.getStartDate(),
                request.getEndDate());

        // 3. Gộp dữ liệu theo giờ
        Map<LocalDateTime, HourlyReportRow> rowsMap = new TreeMap<>();

        for (TelemetryResponse t : telemetryList) {
            if (t.getTimestamp() == null)
                continue;
            LocalDateTime hour = LocalDateTime.ofInstant(t.getTimestamp(), ZoneId.systemDefault())
                    .truncatedTo(ChronoUnit.HOURS);
            rowsMap.putIfAbsent(hour, new HourlyReportRow(hour));

            String rawType = t.getSensorType() != null ? t.getSensorType() : t.getSensorId();
            String normalizedType = normalizeSensorName(rawType);

            // Tính trung bình nếu có nhiều cảm biến cùng loại trong 1 giờ
            Map<String, Double> sensorValues = rowsMap.get(hour).getSensorValues();
            if (sensorValues.containsKey(normalizedType)) {
                sensorValues.put(normalizedType, (sensorValues.get(normalizedType) + t.getValue()) / 2.0);
            } else {
                sensorValues.put(normalizedType, t.getValue());
            }
        }

        for (AqiHistory aqi : aqiList) {
            if (aqi.getCalculatedAt() == null)
                continue;
            LocalDateTime hour = LocalDateTime.ofInstant(aqi.getCalculatedAt(), ZoneId.systemDefault())
                    .truncatedTo(ChronoUnit.HOURS);
            rowsMap.putIfAbsent(hour, new HourlyReportRow(hour));
            HourlyReportRow row = rowsMap.get(hour);
            // Lấy AQI lớn nhất trong giờ đó nếu có nhiều bản ghi
            if (row.getAqi() == null || aqi.getAqiValue() > row.getAqi()) {
                row.setAqi(aqi.getAqiValue());
                row.setLevel(aqi.getLevel());
                row.setMainPollutant(aqi.getMainPollutant());
            }
        }

        List<HourlyReportRow> reportData = new ArrayList<>(rowsMap.values());

        // Tìm tất cả các loại cảm biến để tạo cột
        Set<String> allSensors = new TreeSet<>();
        for (HourlyReportRow row : reportData) {
            allSensors.addAll(row.getSensorValues().keySet());
        }
        List<String> sensorColumns = new ArrayList<>(allSensors);

        try {
            byte[] fileBytes;
            String stationStr = request.getStationName() != null
                    ? request.getStationName().replaceAll("[^a-zA-Z0-9_-]", "_")
                    : request.getStationId();
            String startStr = fileDateFormatter.format(request.getStartDate());
            String endStr = fileDateFormatter.format(request.getEndDate());
            String baseFileName = stationStr + startStr + "_" + endStr;
            String fileName;
            String contentType;

            if ("EXCEL".equalsIgnoreCase(request.getFormat())) {
                fileBytes = generateExcel(reportData, sensorColumns, request);
                fileName = baseFileName + ".xlsx";
                contentType = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
            } else {
                fileBytes = generatePdf(reportData, sensorColumns, request);
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

    private byte[] generateExcel(List<HourlyReportRow> reportData, List<String> sensorColumns, ReportRequest request)
            throws Exception {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Air Quality Report");

            Row headerRow = sheet.createRow(0);
            List<String> columns = new ArrayList<>(
                    Arrays.asList("Thời gian", "Chỉ số AQI", "Mức độ", "Chất ô nhiễm chính"));
            columns.addAll(sensorColumns);

            for (int i = 0; i < columns.size(); i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(columns.get(i));
                CellStyle style = workbook.createCellStyle();
                org.apache.poi.ss.usermodel.Font font = workbook.createFont();
                font.setBold(true);
                style.setFont(font);
                cell.setCellStyle(style);
            }

            int rowIdx = 1;
            for (HourlyReportRow rowData : reportData) {
                Row row = sheet.createRow(rowIdx++);
                row.createCell(0).setCellValue(formatter.format(rowData.getHour()));
                row.createCell(1).setCellValue(rowData.getAqi() != null ? String.valueOf(rowData.getAqi()) : "-");
                row.createCell(2).setCellValue(rowData.getLevel() != null ? rowData.getLevel() : "-");
                row.createCell(3).setCellValue(rowData.getMainPollutant() != null ? rowData.getMainPollutant() : "-");

                int colIdx = 4;
                for (String sensor : sensorColumns) {
                    Double val = rowData.getSensorValues().get(sensor);
                    if (val != null) {
                        row.createCell(colIdx).setCellValue(Math.round(val * 100.0) / 100.0);
                    } else {
                        row.createCell(colIdx).setCellValue("-");
                    }
                    colIdx++;
                }
            }

            workbook.write(out);
            return out.toByteArray();
        }
    }

    private byte[] generatePdf(List<HourlyReportRow> reportData, List<String> sensorColumns, ReportRequest request)
            throws Exception {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Document document = new Document();
            PdfWriter.getInstance(document, out);
            document.open();

            com.lowagie.text.Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18);
            Paragraph title = new Paragraph("Bao cao Chat luong Khong khi (AQI Report)", titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            document.add(title);
            document.add(new Paragraph("Station: "
                    + (request.getStationName() != null ? request.getStationName() : request.getStationId())));
            document.add(new Paragraph("Tu ngay: " + formatter.format(request.getStartDate())));
            document.add(new Paragraph("Den ngay: " + formatter.format(request.getEndDate())));
            document.add(new Paragraph(" "));

            int numCols = 4 + sensorColumns.size();
            PdfPTable table = new PdfPTable(numCols);
            table.setWidthPercentage(100);

            List<String> headers = new ArrayList<>(Arrays.asList("Thoi gian", "AQI", "Muc do", "Chat ON"));
            for (String s : sensorColumns)
                headers.add(s);

            for (String header : headers) {
                PdfPCell cell = new PdfPCell(new Phrase(header, FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10)));
                cell.setHorizontalAlignment(Element.ALIGN_CENTER);
                table.addCell(cell);
            }

            com.lowagie.text.Font dataFont = FontFactory.getFont(FontFactory.HELVETICA, 10);
            for (HourlyReportRow rowData : reportData) {
                table.addCell(new Phrase(formatter.format(rowData.getHour()), dataFont));
                table.addCell(new Phrase(rowData.getAqi() != null ? String.valueOf(rowData.getAqi()) : "-", dataFont));
                table.addCell(new Phrase(rowData.getLevel() != null ? rowData.getLevel() : "-", dataFont));
                table.addCell(
                        new Phrase(rowData.getMainPollutant() != null ? rowData.getMainPollutant() : "-", dataFont));

                for (String sensor : sensorColumns) {
                    Double val = rowData.getSensorValues().get(sensor);
                    String valStr = val != null ? String.valueOf(Math.round(val * 100.0) / 100.0) : "-";
                    table.addCell(new Phrase(valStr, dataFont));
                }
            }

            document.add(table);
            document.close();
            return out.toByteArray();
        }
    }

    private void sendEmailWithAttachment(String to, String fileName, byte[] fileData, String contentType)
            throws Exception {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

        helper.setTo(to);
        helper.setSubject("Báo cáo Chất lượng Không khí (AQI)");
        helper.setText(
                "Chào bạn,\n\nĐính kèm là báo cáo chất lượng không khí tổng hợp (bao gồm AQI và các chỉ số cảm biến) theo yêu cầu của bạn.\n\nTrân trọng,\nHệ thống Quan Trắc IoT");

        ByteArrayResource resource = new ByteArrayResource(fileData);
        helper.addAttachment(fileName, resource, contentType);

        mailSender.send(message);
    }
}
