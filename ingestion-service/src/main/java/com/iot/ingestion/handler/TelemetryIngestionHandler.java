package com.iot.ingestion.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.iot.ingestion.DTO.MqttPayload;
import com.iot.ingestion.DTO.TelemetryEvent;
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
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class TelemetryIngestionHandler {

    private final ObjectMapper objectMapper;
    private final CacheService cacheService;
    private final InfluxDbService influxDbService;
    private final KafkaProducerService kafkaProducerService;

    @Bean
    @ServiceActivator(inputChannel = "mqttInputChannel") // Lắng nghe và tiêu thụ dữ liệu từ kênh MQTT nội bộ
    public MessageHandler handler() {
        return message -> {
            try {
                // 1. Trích xuất Payload thô dạng String từ MQTT
                String rawPayload = (String) message.getPayload();
                log.info("Nhận tin từ MQTT: {}", rawPayload);

                // 2. Chuyển đổi payload thành Object Java
                MqttPayload payload = objectMapper.readValue(rawPayload, MqttPayload.class);

                // 3. Truy xuất metadata của thiết bị trong RAM Redis Cache
                Map<String, Object> metadata = cacheService.getSensorMetadata(payload.getSensorId());
                if (metadata == null) {
                    log.warn("Sensor ID '{}' chưa đăng ký hoặc không hoạt động. Bỏ qua bản tin!", payload.getSensorId());
                    return;
                }

                // 4. Trích xuất thông tin định danh
                String stationId = (String) metadata.get("stationId");
                String sensorTypeCode = (String) metadata.get("sensorTypeCode");
                String unit = (String) metadata.get("unit");

                // 5. Chuẩn hóa mốc thời gian
                Instant instant = payload.getTimestamp() != null
                        ? Instant.ofEpochSecond(payload.getTimestamp())
                        : Instant.now();
                LocalDateTime ldt = LocalDateTime.ofInstant(instant, ZoneId.systemDefault());

                // 6. Ghi bản tin thô vào cơ sở dữ liệu chuỗi thời gian InfluxDB
                influxDbService.writeTelemetry(
                        payload.getSensorId(),
                        stationId,
                        sensorTypeCode,
                        payload.getValue(),
                        instant
                );

                // 7. Đồng thời đóng gói sự kiện chuẩn hóa và đẩy lên Kafka Broker
                TelemetryEvent event = TelemetryEvent.builder()
                        .sensorId(payload.getSensorId())
                        .stationId(stationId)
                        .sensorTypeCode(sensorTypeCode)
                        .value(payload.getValue())
                        .unit(unit)
                        .timestamp(ldt)
                        .build();

                kafkaProducerService.sendTelemetryEvent(event);

            } catch (Exception e) {
                log.error("Lỗi nghiêm trọng trong quá trình xử lý nạp dữ liệu Ingestion!", e);
            }
        };
    }
}