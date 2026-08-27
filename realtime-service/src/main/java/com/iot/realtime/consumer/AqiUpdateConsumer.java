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
public class AqiUpdateConsumer {

    private final SimpMessagingTemplate messagingTemplate;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "aqi.update", groupId = "realtime-service-aqi-group")
    public void consume(String message) {
        log.debug("[Realtime] Nhận được cập nhật AQI từ Kafka: {}", message);
        try {
            Map<String, Object> payload = objectMapper.readValue(message, Map.class);
            String stationId = (String) payload.get("stationId");
            if (stationId != null) {
                // Broadcast to Web Socket
                messagingTemplate.convertAndSend("/topic/aqi/station/" + stationId, payload);
                log.debug("Đã phát cập nhật AQI qua WebSocket tới /topic/aqi/station/{}", stationId);
            }
        } catch (Exception e) {
            log.error("Lỗi khi xử lý Kafka event aqi.update", e);
        }
    }
}
