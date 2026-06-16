package com.iot.ingestion.DTO;

import lombok.Data;

@Data
public class MqttPayload {
    private String sensorId;
    private Double value;
    private Long timestamp; // Epoch Unix timestamp dạng giây hoặc nano giây
}