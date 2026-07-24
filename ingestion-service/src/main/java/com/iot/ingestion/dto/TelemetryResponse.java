package com.iot.ingestion.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TelemetryResponse {
    private String sensorId;
    private String sensorType;
    private Double value;
    private Instant timestamp;
}
