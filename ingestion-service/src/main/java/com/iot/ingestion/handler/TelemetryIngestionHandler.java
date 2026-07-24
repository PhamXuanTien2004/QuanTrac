package com.iot.ingestion.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.iot.ingestion.clients.dto.response.DeviceResponse;
import com.iot.ingestion.clients.dto.response.GatewayResponse;
import com.iot.ingestion.dto.Event;
import com.iot.ingestion.dto.MqttPayload;
import com.iot.ingestion.dto.Status;
import com.iot.ingestion.service.CacheService;
import com.iot.ingestion.service.InfluxDbService;
import com.iot.ingestion.service.KafkaProducerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.messaging.MessageHandler;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

@Component
@RequiredArgsConstructor
@Slf4j
public class TelemetryIngestionHandler {

    private final ObjectMapper objectMapper;
    private final CacheService cacheService;
    private final InfluxDbService influxDbService;
    private final KafkaProducerService kafkaProducerService;

    @Bean
    @ServiceActivator(inputChannel = "mqttInputChannel")
    public MessageHandler handler() {
        return message -> {
            try {
                String rawPayload = (String) message.getPayload();

                // 1. Giải mã JSON từ MQTT
                MqttPayload payload = objectMapper.readValue(rawPayload, MqttPayload.class);
                log.info("=========== Đã Nhận Được Thông Tin ============================");
                log.info("[MQTT-INBOUND] === NHẬN DỮ LIỆU TỪ TRẠM ===");
                log.info(" - Gateway ID: {}", payload.getGatewayId());
                log.info(" - Mốc thời gian: {}", payload.getTimestamp());
                log.info(" - Số cảm biến gửi tin: {}", payload.getSensors() != null ? payload.getSensors().size() : 0);
                if (payload.getSensors() != null) {
                    payload.getSensors().forEach(s -> log.info("   + SensorID: {} | Value: {}", s.getSensorId(), s.getValue()));
                }
                log.info("=======================================");

                if (payload.getGatewayId() == null || payload.getSensors() == null || payload.getSensors().isEmpty()) {
                    log.warn("[VALIDATION-ERROR] Bản tin thiếu GatewayID hoặc mảng dữ liệu. Bỏ qua!");
                    return;
                }

                // 2. Xác thực Gateway siêu tốc qua Redis Cache
                GatewayResponse gatewayMeta = cacheService.getGatewayMetadata(payload.getGatewayId());
                if (gatewayMeta == null || Boolean.TRUE.equals(gatewayMeta.getIsDeleted())) {
                    log.error("[VALIDATION-FAILED] Gateway ID '{}' không tồn tại hoặc đã bị xóa mềm! Hủy bỏ gói tin.", payload.getGatewayId());
                    return;
                }

                String gatewayStatus = gatewayMeta.getStatus();
                if (!"ONLINE".equalsIgnoreCase(gatewayStatus) && !"ACTIVE".equalsIgnoreCase(gatewayStatus)) {
                    log.warn("[VALIDATION-FAILED] Gateway '{}' đang bị khóa ('{}'). Từ chối gói tin!",
                            gatewayMeta.getCode(), gatewayMeta.getStatus());
                    return;
                }

                log.info("[VALIDATION-SUCCESS] Gateway '{}' hợp lệ. Bắt đầu thu thập danh sách cảm biến...", gatewayMeta.getCode());

                // Chuẩn hóa thời gian đo đạc
                Instant instant = payload.getTimestamp() != null
                        ? Instant.ofEpochSecond(payload.getTimestamp())
                        : Instant.now();
                LocalDateTime ldt = LocalDateTime.ofInstant(instant, ZoneId.systemDefault());

                // 3. Duyệt qua mảng cảm biến và ghi nhận dữ liệu
                for (MqttPayload.SensorReading reading : payload.getSensors()) {
                    // Lấy Metadata siêu tốc qua Redis Cache
                    DeviceResponse metadata = cacheService.getSensorMetadata(payload.getGatewayId(), reading.getSensorId());

                    // Nếu cảm biến không có trong danh sách được xác thực
                    if (metadata == null) {
                        log.warn("[VALIDATION-FAILED] Bỏ qua cảm biến sai cấu hình hoặc chưa kích hoạt: {}", reading.getSensorId());
                        continue;
                    }

                    // Kiểm tra khoảng giá trị an toàn
                    Double currentValue = reading.getValue();
                    Double minValue = metadata.getMinValue();
                    Double maxValue = metadata.getMaxValue();

                    // Nếu minValue hoặc maxValue bằng null, hệ thống sẽ bỏ qua giới hạn đó
                    boolean isWithinRange = (currentValue != null)
                            && (minValue == null || currentValue >= minValue)
                            && (maxValue == null || currentValue <= maxValue);

                    if (isWithinRange) {

                        // Đóng gói sự kiện chuẩn hóa và bắn lên Kafka Broker
                        Event event = Event.builder()
                                .sensorId(reading.getSensorId())
                                .stationId(gatewayMeta.getStationId())
                                .sensorTypeCode(metadata.getSensorCode())
                                .value(currentValue)
                                .unit(metadata.getUnit())
                                .timestamp(ldt)
                                .status(Status.ACTIVE)
                                .build();

                        kafkaProducerService.sendTelemetryEvent(event);
                        log.info("[INGESTION-SUCCESS] Đã lưu và phát sự kiện thành công cho Sensor: {} | Value: {}",
                                reading.getSensorId(), currentValue);
                    } else {
                        log.warn("[OUT-OF-RANGE] Giá trị đo đạc {} của Sensor '{}' nằm ngoài dải đo an toàn (Min: {}, Max: {}). Từ chối lưu dữ liệu!",
                                currentValue, reading.getSensorId(), minValue, maxValue);
                        Event event = Event.builder()
                                .sensorId(reading.getSensorId())
                                .stationId(gatewayMeta.getStationId())
                                .sensorTypeCode(metadata.getSensorCode())
                                .value(currentValue)
                                .unit(metadata.getUnit())
                                .timestamp(ldt)
                                .status(Status.INACTIVE)
                                .build();
                        kafkaProducerService.sendAlertEvent(event);
                        log.info("[INGESTION-SUCCESS] Đã lưu và phát sự kiện thành công cho Sensor: {} | Value: {}",
                                reading.getSensorId(), currentValue);
                    }
                    // Ghi dữ liệu thô vào InfluxDB
                    influxDbService.writeTelemetry(
                            reading.getSensorId(),
                            gatewayMeta.getStationId(),
                            metadata.getName(), // Tên cảm biến
                            currentValue,
                            instant
                    );
                }

            } catch (Exception e) {
                log.error("[INGESTION-ERROR] Lỗi hệ thống nghiêm trọng xảy ra trong quá trình nạp tin!", e);
            }
        };
    }
}