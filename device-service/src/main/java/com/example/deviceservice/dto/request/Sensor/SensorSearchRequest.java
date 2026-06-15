package com.example.deviceservice.dto.request.Sensor;

import com.example.deviceservice.dto.request.BaseSearchRequest;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SensorSearchRequest extends BaseSearchRequest {
    private String keyword;
}