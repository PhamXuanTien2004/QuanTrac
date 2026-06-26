package com.iot.ingestion.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.iot.ingestion.clients.DeviceClient;
import com.iot.ingestion.clients.dto.request.BatchSensorVerifyRequest;
import com.iot.ingestion.clients.dto.response.DeviceResponse;
import com.iot.ingestion.clients.dto.response.GatewayResponse;
import com.iot.ingestion.dto.Event;
import com.iot.ingestion.dto.MqttPayload;
import com.iot.ingestion.dto.Status;
import com.iot.ingestion.service.InfluxDbService;
import com.iot.ingestion.service.KafkaProducerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.messaging.MessageHandler;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
public class TelemetryIngestionHandler {

    private final ObjectMapper objectMapper;
    private final DeviceClient deviceClient;
    private final InfluxDbService influxDbService;
    private final KafkaProducerService kafkaProducerService;
    private final RedisTemplate<String, Object> redisTemplate;

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

                // 2. Xác thực Gateway thông qua WebClient
                GatewayResponse gatewayMeta = deviceClient.findGatewayById(payload.getGatewayId());
                if (gatewayMeta == null || Boolean.TRUE.equals(gatewayMeta.getIsDeleted())) {
                    log.error("[VALIDATION-FAILED] Gateway ID '{}' không tồn tại hoặc đã bị xóa mềm! Hủy bỏ gói tin.", payload.getGatewayId());
                    return;
                }

                // 🌟 ĐÃ SỬA: Chấp nhận cả trạng thái "ACTIVE" và "ONLINE" (Không phân biệt chữ hoa chữ thường)
                String gatewayStatus = gatewayMeta.getStatus();
                if (!"ONLINE".equalsIgnoreCase(gatewayStatus) && !"ACTIVE".equalsIgnoreCase(gatewayStatus)) {
                    log.warn("[VALIDATION-FAILED] Gateway '{}' đang bị khóa ('{}'). Từ chối gói tin!",
                            gatewayMeta.getCode(), gatewayMeta.getStatus());
                    return;
                }

                log.info("[VALIDATION-SUCCESS] Gateway '{}' hợp lệ. Bắt đầu thu thập danh sách cảm biến...", gatewayMeta.getCode());

                // 3. Thu thập danh sách sensorId
                List<String> sensorIds = payload.getSensors().stream()
                        .map(MqttPayload.SensorReading::getSensorId)
                        .collect(Collectors.toList());

                // 4. Gọi API xác thực chéo lô cảm biến
                BatchSensorVerifyRequest batchRequest = BatchSensorVerifyRequest.builder()
                        .gatewayId(payload.getGatewayId())
                        .sensorIds(sensorIds)
                        .build();

                List<DeviceResponse> verifiedSensors = deviceClient.verifySensorsBatch(batchRequest);

                if (verifiedSensors.isEmpty()) {
                    log.warn("[VALIDATION-FAILED] Không có cảm biến nào trong gói tin được xác thực thành công. Hủy bỏ!");
                    return;
                }

                // Ánh xạ danh sách cảm biến hợp lệ thành Map
                Map<String, DeviceResponse> verifiedMap = verifiedSensors.stream()
                        .collect(Collectors.toMap(DeviceResponse::getId, d -> d));

                // Chuẩn hóa thời gian đo đạc
                Instant instant = payload.getTimestamp() != null
                        ? Instant.ofEpochSecond(payload.getTimestamp())
                        : Instant.now();
                LocalDateTime ldt = LocalDateTime.ofInstant(instant, ZoneId.systemDefault());

                // 5. Duyệt qua mảng cảm biến và ghi nhận dữ liệu
                for (MqttPayload.SensorReading reading : payload.getSensors()) {
                    DeviceResponse metadata = verifiedMap.get(reading.getSensorId());

                    // Nếu cảm biến không có trong danh sách được device-service xác thực
                    if (metadata == null) {
                        log.warn("[VALIDATION-FAILED] Bỏ qua cảm biến sai cấu hình hoặc chưa kích hoạt: {}", reading.getSensorId());
                        continue;
                    }

                    // TỰ ĐỘNG ẤM CACHE: Lưu cấu hình hợp lệ vào Redis
                    redisTemplate.opsForValue().set("sensor:metadata:" + reading.getSensorId(), metadata, 24, TimeUnit.HOURS);

                    // 🌟 ĐÃ SỬA: Kiểm tra khoảng giá trị an toàn (Tránh NullPointerException tuyệt đối)
                    Double currentValue = reading.getValue();
                    Double minValue = metadata.getMinValue();
                    Double maxValue = metadata.getMaxValue();

                    // Nếu minValue hoặc maxValue bằng null, hệ thống sẽ bỏ qua giới hạn đó (mặc định là hợp lệ)
                    boolean isWithinRange = (currentValue != null)
                            && (minValue == null || currentValue >= minValue)
                            && (maxValue == null || currentValue <= maxValue);

                    if (isWithinRange) {

                        // Đóng gói sự kiện chuẩn hóa và bắn lên Kafka Broker
                        Event event = Event.builder()
                                .sensorId(reading.getSensorId())
                                .stationId(payload.getGatewayId())
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
                                .stationId(payload.getGatewayId())
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
                            payload.getGatewayId(),
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