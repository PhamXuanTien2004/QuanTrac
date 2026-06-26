package com.iot.ingestion.dto;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Builder
public class Event {
    private String sensorId;
    private String stationId;
    private String sensorTypeCode;
    private Double value;
    private String unit;
    private Status status;
    private LocalDateTime timestamp;
}