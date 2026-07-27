package com.iot.notification.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.iot.notification.dto.OtpEvent;
import com.iot.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class OtpKafkaConsumer {

    private final ObjectMapper objectMapper;
    private final NotificationService notificationService;

    @KafkaListener(topics = "otp-normalized", groupId = "notification-group")
    public void consume(String message) {
        try {
            if (message != null && message.startsWith("\"") && message.endsWith("\"")) {
                message = objectMapper.readValue(message, String.class);
            }
            OtpEvent event = objectMapper.readValue(message, OtpEvent.class);
            
            notificationService.sendOtpEmail(event);
        } catch (Exception e) {
            log.error("Error processing otp message: {}", message, e);
        }
    }
}
