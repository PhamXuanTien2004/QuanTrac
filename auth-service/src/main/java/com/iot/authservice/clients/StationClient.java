package com.iot.authservice.clients;

import com.iot.authservice.clients.dto.response.StationResponse;

public interface StationClient {
    StationResponse getByStationId(String stationId);
}
