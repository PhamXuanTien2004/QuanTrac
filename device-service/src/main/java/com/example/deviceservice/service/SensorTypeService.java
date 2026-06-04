package com.example.deviceservice.service;

import com.example.deviceservice.dto.request.SensorType.SensorTypeCreateRequest;
import com.example.deviceservice.dto.request.SensorType.SensorTypeUpdateRequest;
import com.example.deviceservice.dto.response.SensorType.SensorTypeResponse;
import com.example.deviceservice.entity.SensorType;

public interface SensorTypeService {
    SensorType create (SensorTypeCreateRequest request);
    SensorType update (SensorTypeUpdateRequest request);
}
