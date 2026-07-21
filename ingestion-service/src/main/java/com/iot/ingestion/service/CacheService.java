package com.iot.ingestion.service;

import com.iot.ingestion.clients.dto.response.DeviceResponse;
import com.iot.ingestion.clients.dto.response.GatewayResponse;

public interface CacheService {
    GatewayResponse getGatewayMetadata(String gatewayId);
    DeviceResponse getSensorMetadata(String gatewayId, String sensorId);
}