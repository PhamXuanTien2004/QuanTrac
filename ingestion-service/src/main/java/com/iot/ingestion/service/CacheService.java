package com.iot.ingestion.service;

import java.util.Map;

public interface CacheService {
    Map<String, Object> getSensorMetadata(String sensorId);
}