package com.iot.ingestion.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.iot.ingestion.DTO.TelemetryEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class KafkaProducerService {

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final String TOPIC = "telemetry-normalized";

    public void sendTelemetryEvent(TelemetryEvent event) {
        try {
            // Chuyển đổi DTO Java sang dạng chuỗi JSON thô tránh lỗi phân giải kiểu dữ liệu
            String payload = objectMapper.writeValueAsString(event);
            kafkaTemplate.send(TOPIC, event.getSensorId(), payload);
            log.info("Đã bắn Kafka thành công: Topic={} | Sensor={}", TOPIC, event.getSensorId());
        } catch (Exception e) {
            log.error("Bắn tin lên Kafka thất bại cho Sensor: " + event.getSensorId(), e);
        }
    }
}