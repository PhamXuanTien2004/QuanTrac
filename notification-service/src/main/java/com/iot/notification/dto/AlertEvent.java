package com.iot.notification.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AlertEvent {
    private String id;
    private String stationId;
    private String sensorId;
    private String sensorTypeCode;
    private Double value;
    private LocalDateTime timestamp;
    private String status; // e.g. "WARNING", "CRITICAL"
}
