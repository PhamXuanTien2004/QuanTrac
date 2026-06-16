package com.iot.ingestion.DTO;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Builder
public class TelemetryEvent {
    private String sensorId;
    private String stationId;
    private String sensorTypeCode;
    private Double value;
    private String unit;
    private LocalDateTime timestamp;
}