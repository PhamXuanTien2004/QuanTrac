package com.example.deviceservice.mapper;

import com.example.deviceservice.dto.request.Sensor.SensorCreateDTO;
import com.example.deviceservice.dto.request.Sensor.SensorUpdateDTO;
import com.example.deviceservice.dto.response.SensorResponseDTO;
import com.example.deviceservice.entity.Sensor;
import org.mapstruct.*;

@Mapper(componentModel = "spring")
public interface SensorMapper {
    Sensor toEntity(SensorCreateDTO sensorCreateDTO);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateFromRequest(SensorUpdateDTO sensorUpdateDTO, @MappingTarget Sensor sensor);

    @Mapping(target = "gatewayId", source = "gateway.id")
    @Mapping(target = "stationId", source = "gateway.station.id")
    SensorResponseDTO toResponse(Sensor sensor);
}
