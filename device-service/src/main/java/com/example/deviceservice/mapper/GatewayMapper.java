package com.example.deviceservice.mapper;

import com.example.deviceservice.dto.request.Gateway.CreateGatewayRequest;
import com.example.deviceservice.dto.request.Gateway.UpdateGatewayRequest;
import com.example.deviceservice.dto.response.GatewayResponse;
import com.example.deviceservice.entity.Gateway;
import org.mapstruct.*;

@Mapper(componentModel = "spring")
public interface GatewayMapper {

    Gateway fromCreate(CreateGatewayRequest request);

    @Mapping(source = "station.id", target = "stationId")
    GatewayResponse toResponse(Gateway gateway);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateGatewayFromRequest(UpdateGatewayRequest dto, @MappingTarget Gateway entity);
}