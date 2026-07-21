package com.iot.ingestion.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.iot.ingestion.clients.DeviceClient;
import com.iot.ingestion.clients.dto.request.BatchSensorVerifyRequest; // 🌟 Thêm import này
import com.iot.ingestion.clients.dto.response.DeviceResponse;
import com.iot.ingestion.service.CacheService;
import com.iot.ingestion.clients.dto.response.GatewayResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class CacheServiceImpl implements CacheService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final DeviceClient deviceClient;
    private final ObjectMapper objectMapper;

    private static final String CACHE_PREFIX = "sensor:metadata:";
    private static final String GATEWAY_CACHE_PREFIX = "gateway:metadata:";

    @Override
    public GatewayResponse getGatewayMetadata(String gatewayId) {
        String key = GATEWAY_CACHE_PREFIX + gatewayId;
        
        try {
            Object rawCached = redisTemplate.opsForValue().get(key);
            if (rawCached != null) {
                GatewayResponse cachedData = objectMapper.convertValue(rawCached, GatewayResponse.class);
                log.info("[CACHE-HIT] Tìm thấy cấu hình Gateway {} trong Redis.", gatewayId);
                return cachedData;
            }
        } catch (Exception e) {
            log.error("[REDIS-ERROR] Không đọc được hoặc lỗi giải mã Redis Cache cho Gateway: {}", gatewayId, e);
        }
        
        log.warn("[CACHE-MISS] Đang gọi API dự phòng lấy metadata cho Gateway {}...", gatewayId);
        
        GatewayResponse gatewayMeta = deviceClient.findGatewayById(gatewayId);
        if (gatewayMeta != null) {
            redisTemplate.opsForValue().set(key, gatewayMeta, 24, TimeUnit.HOURS);
            return gatewayMeta;
        }
        
        log.warn("[VALIDATION-FAILED] Không tìm thấy Gateway ID: {} dưới Database.", gatewayId);
        return null;
    }

    @Override
    public DeviceResponse getSensorMetadata(String gatewayId, String sensorId) {
        String key = CACHE_PREFIX + sensorId;

        // 1. Đọc thử từ Redis Cache (RAM kết nối siêu nhanh < 1ms)
        try {
            Object rawCached = redisTemplate.opsForValue().get(key);
            if (rawCached != null) {
                DeviceResponse cachedData = objectMapper.convertValue(rawCached, DeviceResponse.class);
                log.info("[CACHE-HIT] Tìm thấy cấu hình Sensor {} trong Redis.", sensorId);
                return cachedData;
            }
        } catch (Exception e) {
            log.error("[REDIS-ERROR] Không đọc được hoặc lỗi giải mã Redis Cache cho Sensor: {}", sensorId, e);
        }

        // 2. Gọi API dự phòng (WebClient đồng bộ) nếu xảy ra Cache Miss
        log.warn("[CACHE-MISS] Đang gọi API dự phòng lấy metadata cho Sensor {}...", sensorId);

        BatchSensorVerifyRequest request = BatchSensorVerifyRequest.builder()
                .gatewayId(gatewayId)
                .sensorIds(List.of(sensorId)) // Tạo danh sách đơn lẻ chứa 1 phần tử
                .build();

        // Gọi trực tiếp phương thức verifySensorsBatch có sẵn trong Interface của bạn
        List<DeviceResponse> deviceList = deviceClient.verifySensorsBatch(request);

        if (deviceList != null && !deviceList.isEmpty()) {
            // Lấy ra phần tử đầu tiên (và duy nhất) trả về từ database
            DeviceResponse remoteData = deviceList.get(0);

            // Ghi vào Redis Cache dưới dạng đối tượng thô
            redisTemplate.opsForValue().set(key, remoteData, 24, TimeUnit.HOURS);
            return remoteData;
        }

        log.warn("[VALIDATION-FAILED] Không tìm thấy cấu hình hợp lệ cho Sensor ID: {} dưới Database.", sensorId);
        return null; // Thiết bị thực sự không tồn tại hoặc thông tin liên kết sai lệch dưới DB
    }
}