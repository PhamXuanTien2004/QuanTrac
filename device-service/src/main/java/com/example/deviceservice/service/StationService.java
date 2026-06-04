package com.example.deviceservice.service;

import com.example.deviceservice.dto.request.Station.CreateStationRequest;
import com.example.deviceservice.dto.request.Station.FilterStationRequest;
import com.example.deviceservice.dto.request.Station.UpdateStationRequest;
import com.example.deviceservice.dto.response.Station.StationResponse;
import com.example.deviceservice.entity.Station;
import org.springframework.data.domain.Page;

import org.springframework.data.domain.Pageable;

public interface StationService {
    Station create (CreateStationRequest request);
    Page<StationResponse> filter(FilterStationRequest filter, Pageable pageable);
    StationResponse update (UpdateStationRequest updateStationRequest);
    Station deleteById(String id);
}
