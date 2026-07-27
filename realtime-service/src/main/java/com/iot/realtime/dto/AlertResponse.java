package com.iot.realtime.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AlertResponse {
    private String sensorId;
    private String sensorType;
    private double value;
    private String unit;
    private String message;
    private Instant timestamp;
}
