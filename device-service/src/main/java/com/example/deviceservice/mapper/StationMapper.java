package com.example.deviceservice.mapper;

import com.example.deviceservice.dto.request.Station.CreateStationRequest;
import com.example.deviceservice.dto.request.Station.UpdateStationRequest;

import com.example.deviceservice.dto.response.Station.StationResponse;
import com.example.deviceservice.entity.Station;
import org.mapstruct.*;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface StationMapper {
    // Tự động map từ Request DTO sang Entity
    Station toEntity(CreateStationRequest request);

    // Map dữ liệu cập nhật
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateStationFromRequest(UpdateStationRequest dto, @MappingTarget Station entity);

    // ĐẢM BẢO CÓ HÀM NÀY: Map thẳng từ Entity sang StationDTO
    StationResponse toDTO(Station station);

    StationResponse toResponse(Station station);
}