package com.example.deviceservice.service;

import com.example.deviceservice.dto.request.SensorType.SensorTypeCreateRequest;
import com.example.deviceservice.dto.request.SensorType.SensorTypeSearchRequest;
import com.example.deviceservice.dto.request.SensorType.SensorTypeUpdateRequest;
import com.example.deviceservice.dto.response.SensorType.SensorTypeResponse;
import com.example.deviceservice.entity.SensorType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface SensorTypeService {
    SensorType create (SensorTypeCreateRequest request);
    SensorType update (SensorTypeUpdateRequest request);
    void delete (String id);
    Page<SensorTypeResponse> filter(SensorTypeSearchRequest request);
}
