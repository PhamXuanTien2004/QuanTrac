package com.iot.ingestion.service.impl;

import com.iot.ingestion.service.CacheService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class CacheServiceImpl implements CacheService {

    private final RedisTemplate<String, Object> redisTemplate;

    @Override
    @SuppressWarnings("unchecked")
    public Map<String, Object> getSensorMetadata(String sensorId) {
        try {
            // Định dạng khóa tìm kiếm đồng bộ với thiết lập của device-service
            String key = "sensor:" + sensorId;
            Object metadata = redisTemplate.opsForValue().get(key);
            if (metadata instanceof Map) {
                return (Map<String, Object>) metadata;
            }
        } catch (Exception e) {
            log.error("Lỗi khi truy vấn Redis cho Sensor: " + sensorId, e);
        }
        return null;
    }
}