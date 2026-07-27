package com.iot.realtime.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.iot.realtime.dto.TelemetryEvent;
import com.iot.realtime.dto.TelemetryResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
@RequiredArgsConstructor
@Slf4j
public class TelemetryKafkaConsumer {

    private final SimpMessagingTemplate messagingTemplate;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = {"telemetry-normalized", "alert-normalized"}, groupId = "realtime-group")
    public void consume(String message, @org.springframework.messaging.handler.annotation.Header(org.springframework.kafka.support.KafkaHeaders.RECEIVED_TOPIC) String topic) {
        try {
            // Fix double-encoded JSON strings from Kafka
            if (message != null && message.startsWith("\"") && message.endsWith("\"")) {
                message = objectMapper.readValue(message, String.class);
            }
            TelemetryEvent event = objectMapper.readValue(message, TelemetryEvent.class);
            
            Instant timestamp = event.getTimestamp() != null ? 
                             event.getTimestamp().atZone(java.time.ZoneId.systemDefault()).toInstant() : 
                             Instant.now();

            if ("alert-normalized".equals(topic)) {
                com.iot.realtime.dto.AlertResponse alert = com.iot.realtime.dto.AlertResponse.builder()
                        .sensorId(event.getSensorId())
                        .sensorType(event.getSensorTypeCode())
                        .value(event.getValue())
                        .unit(event.getUnit())
                        .message("Cảnh báo: Cảm biến " + event.getSensorTypeCode() + " vượt ngưỡng (" + event.getValue() + " " + (event.getUnit() != null ? event.getUnit() : "") + ")")
                        .timestamp(timestamp)
                        .build();

                String destination = "/topic/alerts/station/" + event.getStationId();
                messagingTemplate.convertAndSend(destination, alert);
                log.info("Pushed ALERt to {}: Sensor={} Value={}", destination, event.getSensorId(), event.getValue());
            } else {
                TelemetryResponse response = TelemetryResponse.builder()
                        .sensorId(event.getSensorId())
                        .sensorType(event.getSensorTypeCode())
                        .value(event.getValue())
                        .timestamp(timestamp)
                        .build();

                // Push to WebSocket topic for specific station
                String destination = "/topic/station/" + event.getStationId();
                messagingTemplate.convertAndSend(destination, response);
                log.info("Pushed realtime data to {}: Sensor={} Value={}", destination, event.getSensorId(), event.getValue());
            }
        } catch (Exception e) {
            log.error("Error processing kafka message in realtime-service: {}", message, e);
        }
    }
}
