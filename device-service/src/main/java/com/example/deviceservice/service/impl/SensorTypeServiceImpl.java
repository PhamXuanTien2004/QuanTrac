package com.example.deviceservice.service.impl;

import com.example.deviceservice.dto.request.SensorType.SensorTypeCreateRequest;
import com.example.deviceservice.dto.request.SensorType.SensorTypeUpdateRequest;
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
        if (!isValidRange(request.getMinRange(), request.getMaxRange())) {
            throw new ApplicationException("Ngưỡng đo Min phải nhỏ hơn Max");
        }

        // 3. Map dữ liệu từ DTO Request sang Entity để chuẩn bị lưu mới
        SensorType sensorType = sensorTypesMapper.toEntity(request);

        // 4. Lưu vào cơ sở dữ liệu
        return sensorTypeRepository.save(sensorType);
    }

    @Override
    @Transactional
    public SensorType update(SensorTypeUpdateRequest request) {
        // 1. Kiểm tra id và tìm thực thể cũ dưới DB
        SensorType sensorType = sensorTypeRepository.findById(request.getId())
                .orElseThrow(() -> new ApplicationException("SensorTypes not found id '" + request.getId() + "'"));

        // 2. Chuyển đổi dữ liệu từ Request vào Entity (các trường null trong request sẽ bị bỏ qua)
        sensorTypesMapper.updateSensorTypeFromRequest(request, sensorType);

        // 3. Kiểm tra logic khoảng đo dựa trên dữ liệu CUỐI CÙNG của Entity sau khi map
        if (!isValidRange(sensorType.getMinRange(), sensorType.getMaxRange())) {
            throw new ApplicationException("Ngưỡng đo Min phải nhỏ hơn Max");
        }

        return sensorTypeRepository.save(sensorType);
    }

    // Đổi tên thành camelCase và tối ưu hóa logic
    private boolean isValidRange(Double min, Double max) {
        // Nếu thiết kế DB cho phép 1 trong 2 trường này bằng NULL (không bắt buộc nhập)
        if (min == null || max == null) {
            return true; // Hoặc trả về false nếu nghiệp vụ của bạn BẮT BUỘC phải có khoảng đo
        }

        return min < max;
    }
}
