package com.iot.ingestion.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.iot.ingestion.dto.Event;
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
    private final String TOPIC_Telemetry = "telemetry-normalized";
    private final String TOPIC_Alert = "alert-normalized";

    // Gửi dữ liệu để cập nhật cho dashboard
    public void sendTelemetryEvent(Event event) {
        try {
            kafkaTemplate.send(TOPIC_Telemetry, event.getSensorId(), event);
            log.info("Đã bắn Kafka thành công: Topic={} | Sensor={}", TOPIC_Telemetry, event.getSensorId());
        } catch (Exception e) {
            log.error("Bắn tin lên Kafka thất bại cho Sensor: " + event.getSensorId(), e);
        }
    }

    // Gửi dữ liệu cảnh báo khi nằm ngoài ngưỡng an toàn
    public void sendAlertEvent(Event event) {
        try {
            kafkaTemplate.send(TOPIC_Alert, event.getSensorId(), event);
            log.info("Đã bắn thông tin lỗi lên Kafa thành công: Topic={} | Sensor={}", TOPIC_Alert, event.getSensorId());
        } catch (Exception e){
            log.error("Bắn tin lên Kafka thất bại cho Sensor : " + event.getSensorId(), e);
        }
    }
}