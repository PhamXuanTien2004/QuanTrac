package com.example.deviceservice.service;

import com.example.deviceservice.dto.request.SensorType.SensorTypeCreateRequest;
import com.example.deviceservice.dto.request.SensorType.SensorTypeSearchRequest;
import com.example.deviceservice.dto.request.SensorType.SensorTypeUpdateRequest;
import com.example.deviceservice.dto.response.SensorTypeResponse;
import com.example.deviceservice.entity.SensorType;
import org.springframework.data.domain.Page;

public interface SensorTypeService {
    SensorType create (SensorTypeCreateRequest request);
    SensorType update (SensorTypeUpdateRequest request);
    void delete (String id);
    Page<SensorTypeResponse> filter(SensorTypeSearchRequest request);
}
