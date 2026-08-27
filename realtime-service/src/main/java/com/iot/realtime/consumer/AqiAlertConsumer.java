package com.iot.realtime.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class AqiAlertConsumer {

    private final SimpMessagingTemplate messagingTemplate;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "aqi.alert", groupId = "realtime-service-group")
    public void consume(String message) {
        log.info("[Realtime] Nhận được cảnh báo AQI từ Kafka: {}", message);
        try {
            Map<String, Object> payload = objectMapper.readValue(message, Map.class);
            String stationId = (String) payload.get("stationId");
            if (stationId != null) {
                // Thêm trường message cho Frontend đọc
                String aqiLevel = (String) payload.get("level");
                payload.put("message", "CẢNH BÁO MÔI TRƯỜNG: Chỉ số AQI hiện tại ở mức " + aqiLevel + "! Vui lòng kiểm tra ngay.");
                
                // Broadcast to Web Socket
                messagingTemplate.convertAndSend("/topic/alerts/station/" + stationId, payload);
                log.info("Đã phát cảnh báo qua WebSocket tới /topic/alerts/station/{}", stationId);
            }
        } catch (Exception e) {
            log.error("Lỗi khi xử lý Kafka event aqi.alert", e);
        }
    }
}
