package com.iot.ingestion.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.iot.ingestion.gateway.MqttGateway;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class AqiAlertDownlinkConsumer {

    private final MqttGateway mqttGateway;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "aqi.alert", groupId = "ingestion-service-group")
    public void consumeAqiAlert(String message) {
        log.info("[Downlink] Nhận được cảnh báo AQI từ Kafka: {}", message);
        try {
            Map<String, Object> payload = objectMapper.readValue(message, Map.class);
            String stationId = (String) payload.get("stationId");
            if (stationId != null) {
                // Topic downlink
                String topic = "iot/control/station01"; 
                
                String mqttMessage = objectMapper.writeValueAsString(payload);
                mqttGateway.sendToMqtt(mqttMessage, topic);
                log.info("Đã gửi lệnh điều khiển MQTT xuống thiết bị qua topic {}: {}", topic, mqttMessage);
            }
        } catch (Exception e) {
            log.error("Lỗi khi gửi MQTT Downlink", e);
        }
    }
}
