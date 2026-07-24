package com.iot.realtime.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TelemetryEvent {
    private String sensorId;
    private String stationId;
    private String sensorTypeCode;
    private double value;
    private String unit;
    private LocalDateTime timestamp;
}
