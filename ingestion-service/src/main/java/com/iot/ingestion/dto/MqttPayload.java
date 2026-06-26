package com.iot.ingestion.dto;

import lombok.Data;
import java.util.List;

@Data
public class MqttPayload {
    private String gatewayId;
    private Long timestamp;
    private List<SensorReading> sensors; // Danh sách các cảm biến gửi kèm

    @Data
    public static class SensorReading {
        private String sensorId;
        private Double value;
    }
}