package com.example.deviceservice.dto.request.SensorType;

import com.example.deviceservice.dto.request.BaseSearchRequest;
import jakarta.persistence.Column;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SensorTypeSearchRequest extends BaseSearchRequest {
    private String id;
    private String code;
    private String name;
    private String unit;
    private Double minRange;
    private Double maxRange;
}