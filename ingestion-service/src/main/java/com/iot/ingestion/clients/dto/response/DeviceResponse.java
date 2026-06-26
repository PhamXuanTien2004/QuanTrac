package com.iot.ingestion.clients.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class DeviceResponse {
    private String id;
    private String gatewayId;
    private String sensorTypeId;
    private String sensorCode;
    private String name;
    private Double minValue;
    private Double maxValue;
    private String status;
    private String gatewayName;
    private String stationName;
    private String unit;
}