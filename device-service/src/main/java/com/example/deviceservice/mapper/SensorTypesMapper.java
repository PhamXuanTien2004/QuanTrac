package com.example.deviceservice.mapper;

import com.example.deviceservice.dto.request.SensorType.SensorTypeCreateRequest;
import com.example.deviceservice.dto.response.SensorType.SensorTypeResponse;
import com.example.deviceservice.entity.SensorType;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface SensorTypesMapper {
    SensorType toEntity(SensorTypeCreateRequest request);
}
