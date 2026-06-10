package com.example.deviceservice.mapper;

import com.example.deviceservice.dto.request.Sensor.SensorCreateDTO;
import com.example.deviceservice.dto.request.Sensor.SensorUpdateDTO;
import com.example.deviceservice.dto.response.SensorResponseDTO;
import com.example.deviceservice.entity.Sensor;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(componentModel = "spring")
public interface SensorMapper {
    Sensor toEntity(SensorCreateDTO sensorCreateDTO);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateFromRequest(SensorUpdateDTO sensorUpdateDTO, @MappingTarget Sensor sensor);

    SensorResponseDTO toResponse(Sensor sensor);
}
