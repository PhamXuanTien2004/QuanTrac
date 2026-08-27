package com.iot.notification.job;

import com.iot.notification.client.DeviceClient;
import com.iot.notification.dto.BaseResponse;
import com.iot.notification.dto.AlertEvent;
import com.iot.notification.dto.StationDto;
import com.iot.notification.dto.TelemetryResponse;
import com.iot.notification.entity.AqiHistory;
import com.iot.notification.repository.AqiHistoryRepository;
import com.iot.notification.service.AqiCalculatorService;
import com.iot.notification.service.InfluxDbService;
import com.iot.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
public class AqiCalculationJob {

    private final DeviceClient deviceClient;
    private final InfluxDbService influxDbService;
    private final AqiCalculatorService aqiCalculatorService;
    private final AqiHistoryRepository aqiHistoryRepository;
    private final NotificationService notificationService;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    // @Scheduled(cron = "0 0/15 * * * ?")
    // Run exactly on the hour (e.g. 1:00, 2:00, 3:00)
    @Scheduled(cron = "0 0 * * * ?")
    public void calculateAqi() {
        log.info("Bắt đầu job tính toán AQI định kỳ...");
        try {
            // 1. Fetch active stations
            BaseResponse<List<StationDto>> stationResponse = deviceClient.getAllStations();
            if (stationResponse == null || stationResponse.getData() == null) {
                log.warn("Không lấy được danh sách trạm từ device-service");
                return;
            }

            List<StationDto> stations = stationResponse.getData();
            log.info("Lấy được {} trạm từ device-service", stations.size());

            Instant endTime = Instant.now();
            Instant startTime = endTime.minus(1, ChronoUnit.HOURS); // Default: 1 hour moving average

            for (StationDto station : stations) {
                log.info("Đang kiểm tra trạm {} với trạng thái {}", station.getId(), station.getStatus());
                if (!"ACTIVE".equalsIgnoreCase(station.getStatus())
                        && !"ONLINE".equalsIgnoreCase(station.getStatus())) {
                    log.debug("Bỏ qua trạm {} vì trạng thái không phải ACTIVE/ONLINE", station.getId());
                    continue;
                }

                calculateAqiForStation(station, startTime, endTime);
            }
        } catch (Exception e) {
            log.error("Lỗi khi thực thi AQI Calculation Job", e);
        }
    }

    private void calculateAqiForStation(StationDto station, Instant startTime, Instant endTime) {
        try {
            // 2. Lấy dữ liệu telemetry 1h qua của trạm
            List<TelemetryResponse> telemetries = influxDbService.getHistoricalTelemetry(station.getId(), startTime,
                    endTime);

            if (telemetries == null || telemetries.isEmpty()) {
                log.info("Không có dữ liệu cho trạm {} trong 1 giờ qua.", station.getId());
                return;
            }

            // 3. Gom nhóm theo sensorType và tính trung bình (concentration)
            Map<String, Double> avgByPollutant = telemetries.stream()
                    .filter(t -> t.getSensorType() != null)
                    .collect(Collectors.groupingBy(
                            TelemetryResponse::getSensorType,
                            Collectors.averagingDouble(TelemetryResponse::getValue)));

            log.info("Trạm {}: Dữ liệu telemetries={}, Các pollutant trung bình: {}", station.getId(),
                    telemetries.size(), avgByPollutant);

            int maxAqi = 0;
            String mainPollutant = "N/A";

            // 4. Tính Sub-AQI cho PM2.5, PM10, SO2, NO2, CO, O3
            for (Map.Entry<String, Double> entry : avgByPollutant.entrySet()) {
                String type = entry.getKey();
                Double avgValue = entry.getValue();

                Integer subAqi = aqiCalculatorService.calculateSubAqi(type, avgValue);
                if (subAqi != null) {
                    log.info("Trạm {}: Sensor {} (Avg: {}) -> Sub-AQI: {}", station.getId(), type, avgValue, subAqi);
                    if (subAqi > maxAqi) {
                        maxAqi = subAqi;
                        mainPollutant = type;
                    }
                } else {
                    log.info("Trạm {}: Sensor {} (Avg: {}) -> Sub-AQI: NULL", station.getId(), type, avgValue);
                }
            }

            if (maxAqi == 0) {
                log.info("Trạm {} không có thông số hợp lệ để tính AQI.", station.getId());
                return;
            }

            // 5. Lưu vào Database
            String level = aqiCalculatorService.getAqiLevel(maxAqi);
            AqiHistory history = AqiHistory.builder()
                    .stationId(station.getId())
                    .aqiValue(maxAqi)
                    .mainPollutant(mainPollutant)
                    .level(level)
                    .build();
            aqiHistoryRepository.save(history);
            log.info("Lưu AQI thành công cho trạm {}: AQI={}, Pollutant={}, Level={}", station.getId(), maxAqi,
                    mainPollutant, level);

            // Bắn sự kiện cập nhật AQI chung (Để Frontend tự động cập nhật số liệu)
            try {
                Map<String, Object> updatePayload = Map.of(
                        "stationId", station.getId(),
                        "aqiValue", maxAqi,
                        "level", level,
                        "pollutant", mainPollutant,
                        "type", "AQI_UPDATE",
                        "timestamp", Instant.now().toString());
                kafkaTemplate.send("aqi.update", station.getId(), updatePayload);
            } catch (Exception e) {
                log.error("Lỗi khi bắn sự kiện aqi.update", e);
            }

            // 6. Kiểm tra ngưỡng và gửi cảnh báo (AQI > 100)
            if (maxAqi > 100) {
                log.warn("AQI trạm {} ở mức {} ({}). Gửi cảnh báo...", station.getId(), maxAqi, level);
                notificationService.processAqiAlert(station.getId(), maxAqi, level, mainPollutant);
            }

        } catch (Exception e) {
            log.error("Lỗi khi tính AQI cho trạm {}", station.getId(), e);
        }
    }
}
