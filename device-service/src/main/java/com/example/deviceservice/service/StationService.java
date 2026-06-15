package com.example.deviceservice.service;

import com.example.deviceservice.dto.request.Station.CreateStationRequest;
import com.example.deviceservice.dto.request.Station.FilterStationRequest;
import com.example.deviceservice.dto.request.Station.UpdateStationRequest;
import com.example.deviceservice.dto.response.StationResponse;
import com.example.deviceservice.entity.Station;
import org.springframework.data.domain.Page;

public interface StationService {
    Station create (CreateStationRequest request);
    Page<StationResponse> filter(FilterStationRequest filter);
    StationResponse update (UpdateStationRequest updateStationRequest);
    Station deleteById(String id);
}
