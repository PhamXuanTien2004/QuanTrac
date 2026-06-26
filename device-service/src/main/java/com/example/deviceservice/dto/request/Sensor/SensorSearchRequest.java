package com.example.deviceservice.dto.request.Sensor;

import com.example.deviceservice.dto.request.BaseSearchRequest;
import com.example.deviceservice.entity.Status;
import jakarta.persistence.Column;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
public class SensorSearchRequest extends BaseSearchRequest {
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

    private Status status;
}