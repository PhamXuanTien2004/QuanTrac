package com.iot.notification.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.iot.notification.dto.AlertEvent;
import com.iot.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class AlertKafkaConsumer {

    private final ObjectMapper objectMapper;
    private final NotificationService notificationService;

    @KafkaListener(topics = "alert-normalized", groupId = "notification-group")
    public void consume(String message) {
        try {
            // Fix double-encoded JSON strings from Kafka if present
            if (message != null && message.startsWith("\"") && message.endsWith("\"")) {
                message = objectMapper.readValue(message, String.class);
            }
            AlertEvent event = objectMapper.readValue(message, AlertEvent.class);
            
            notificationService.processAlert(event);
        } catch (Exception e) {
            log.error("Error processing alert message in notification-service: {}", message, e);
        }
    }
}
