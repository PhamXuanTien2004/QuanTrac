package com.example.deviceservice.dto.response;

import lombok.Data;

import java.time.Instant;

@Data
public class SensorResponseDTO {
    private String id;
    private String gatewayId;
    private String sensorTypeId;
    private String sensorCode;
    private String name;
    private String model;
    private String manufacturer;
    private Instant installationDate;
    private Instant calibrationDate;
    private Double minValue;
    private Double maxValue;
    private String status;
    private Boolean isDeleted;
    private Instant createdDate;
    private String createdBy;
    private String lastModifiedBy;
    private Instant lastModifiedDate;
}
