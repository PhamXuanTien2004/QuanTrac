package com.example.deviceservice.mapper;

import com.example.deviceservice.dto.request.SensorType.SensorTypeCreateRequest;
import com.example.deviceservice.dto.request.SensorType.SensorTypeUpdateRequest;
import com.example.deviceservice.dto.response.SensorTypeResponse;
import com.example.deviceservice.entity.SensorType;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(componentModel = "spring")
public interface SensorTypesMapper {
    SensorType toEntity(SensorTypeCreateRequest request);

    SensorTypeResponse toResponse (SensorType entity);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateSensorTypeFromRequest(SensorTypeUpdateRequest request, @MappingTarget SensorType sensorType);
}
