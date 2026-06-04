package com.example.deviceservice.dto.response.SensorType;

import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.time.LocalDateTime;

@Getter
@Setter
public class SensorTypeResponse {
    private String id;
    private String code;
    private String name;
    private String unit;
    private String description;
    private Double minRange;
    private Double maxRange;

    private Boolean isDeleted;
    private String createdBy;
    private Instant createdDate;
    private String lastModifiedBy;
    private Instant lastModifiedDate;
}
