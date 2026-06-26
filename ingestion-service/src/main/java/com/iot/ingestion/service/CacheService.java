package com.iot.ingestion.service;

import com.iot.ingestion.clients.dto.response.DeviceResponse;

import java.util.Map;

public interface CacheService {
    DeviceResponse getSensorMetadata(String gatewayId, String sensorId);
}