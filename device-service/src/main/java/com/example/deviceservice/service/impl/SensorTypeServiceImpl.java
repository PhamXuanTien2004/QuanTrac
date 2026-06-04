package com.example.deviceservice.service.impl;

import com.example.deviceservice.dto.request.SensorType.SensorTypeCreateRequest;
import com.example.deviceservice.dto.response.SensorType.SensorTypeResponse;
import com.example.deviceservice.entity.SensorType;
import com.example.deviceservice.exception.ApplicationException;
import com.example.deviceservice.mapper.SensorTypesMapper;
import com.example.deviceservice.repository.SensorTypeRepository;
import com.example.deviceservice.service.SensorTypeService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class SensorTypeServiceImpl implements SensorTypeService {
    private final SensorTypeRepository sensorTypeRepository;
    private final SensorTypesMapper sensorTypesMapper;

    @Override
    @Transactional
    public SensorType create(SensorTypeCreateRequest request) {
        // 1. Kiểm tra trùng mã: Nếu TÌM THẤY thì báo lỗi ngay, không cho tạo trùng
        sensorTypeRepository.findByCode(request.getCode())
                .ifPresent(existing -> {
                    throw new ApplicationException("Mã loại cảm biến '" + request.getCode() + "' đã tồn tại!");
                });

        // 2. Kiểm tra logic Min - Max (Cần check null trước để tránh lỗi NullPointerException nếu user không nhập)
        if (request.getMinRange() != null && request.getMaxRange() != null
                && request.getMinRange() >= request.getMaxRange()) {
            throw new ApplicationException("Ngưỡng đo Min phải nhỏ hơn Max");
        }

        // 3. Map dữ liệu từ DTO Request sang Entity để chuẩn bị lưu mới
        SensorType sensorType = sensorTypesMapper.toEntity(request);

        // 4. Lưu vào cơ sở dữ liệu
        return sensorTypeRepository.save(sensorType);
    }
}
