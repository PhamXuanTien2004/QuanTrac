package com.example.deviceservice.service.impl;

import com.example.deviceservice.common.GenericSpecification;
import com.example.deviceservice.dto.request.BatchSensorVerifyRequest;
import com.example.deviceservice.dto.request.Sensor.SensorCreateDTO;
import com.example.deviceservice.dto.request.Sensor.SensorSearchRequest;
import com.example.deviceservice.dto.request.Sensor.SensorUpdateDTO;
import com.example.deviceservice.dto.response.SensorResponseDTO;
import com.example.deviceservice.entity.Gateway;
import com.example.deviceservice.entity.Sensor;
import com.example.deviceservice.entity.SensorType;
import com.example.deviceservice.exception.ApplicationException;
import com.example.deviceservice.mapper.SensorMapper;
import com.example.deviceservice.repository.GatewayRepository;
import com.example.deviceservice.repository.SensorRepository;
import com.example.deviceservice.repository.SensorTypeRepository;
import com.example.deviceservice.service.SensorService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SensorServiceImpl implements SensorService {

    private final SensorRepository sensorRepository;
    private final SensorMapper sensorMapper;
    private final GatewayRepository gatewayRepository;
    private final SensorTypeRepository sensorTypeRepository;

    @Override
    @Transactional
    public Sensor create(SensorCreateDTO sensorCreateDTO) {
        Gateway gateway = gatewayRepository.findByCodeIgnoreCase(sensorCreateDTO.getGatewayCode())
                .orElseThrow(() -> new ApplicationException("Không tìm thấy Gateway với Mã: " + sensorCreateDTO.getGatewayCode()));

        SensorType sensorType = sensorTypeRepository.findByNameIgnoreCase(sensorCreateDTO.getSensorTypeName())
                .orElseThrow(() -> new ApplicationException("Không tìm thấy Loại cảm biến với Tên: " + sensorCreateDTO.getSensorTypeName()));

        // 2. Kiểm tra max - min value đầu vào của request có hợp lệ không
        if (!isValidValue(sensorCreateDTO.getMinValue(), sensorCreateDTO.getMaxValue())) {
            throw new ApplicationException("Giá trị cấu hình không hợp lệ: min_value phải nhỏ hơn max_value");
        }

        // 3. Kiểm tra xem min-maxValue cấu hình có nằm trong khoảng giới hạn vật lý (min-maxRange) của SensorType không
        validateSensorRange(sensorCreateDTO.getMinValue(), sensorCreateDTO.getMaxValue(), sensorType);

        // 4. Kiểm tra trùng mã Sensor Code
        if (sensorRepository.existsBySensorCodeAndIsDeletedFalse(sensorCreateDTO.getSensorCode())) {
            throw new ApplicationException("Sensor code '" + sensorCreateDTO.getSensorCode() + "' đã tồn tại hệ thống!");
        }

        Sensor sensor = sensorMapper.toEntity(sensorCreateDTO);
        sensor.setGatewayId(gateway.getId());
        sensor.setSensorTypeId(sensorType.getId());
        return sensorRepository.save(sensor);
    }

    @Override
    @Transactional
    public Sensor update(SensorUpdateDTO sensorUpdateDTO) {
        Sensor sensor = sensorRepository.findById(sensorUpdateDTO.getId())
                .orElseThrow(() -> new ApplicationException("Sensor not found id " + sensorUpdateDTO.getId()));

        if (sensorUpdateDTO.getGatewayCode() != null) {
            Gateway gateway = gatewayRepository.findByCodeIgnoreCase(sensorUpdateDTO.getGatewayCode())
                    .orElseThrow(() -> new ApplicationException("Không tìm thấy Gateway với Mã: " + sensorUpdateDTO.getGatewayCode()));
            sensor.setGatewayId(gateway.getId());
        }

        if (sensorUpdateDTO.getSensorTypeName() != null) {
            SensorType sensorType = sensorTypeRepository.findByNameIgnoreCase(sensorUpdateDTO.getSensorTypeName())
                    .orElseThrow(() -> new ApplicationException("Không tìm thấy Loại cảm biến với Tên: " + sensorUpdateDTO.getSensorTypeName()));
            sensor.setSensorTypeId(sensorType.getId());
        }
        
        if (sensorUpdateDTO.getSensorCode() != null && !sensorUpdateDTO.getSensorCode().equals(sensor.getSensorCode())) {
            if (sensorRepository.existsBySensorCodeAndIsDeletedFalse(sensorUpdateDTO.getSensorCode())) {
                throw new ApplicationException("Sensor code '" + sensorUpdateDTO.getSensorCode() + "' đã tồn tại hệ thống!");
            }
            sensor.setSensorCode(sensorUpdateDTO.getSensorCode());
        }

        // Thực hiện map các thay đổi từ DTO vào Entity
        sensorMapper.updateFromRequest(sensorUpdateDTO, sensor);

        // 5. Nếu có cập nhật liên quan đến khoảng đo min/max thì kiểm tra lại tính hợp lệ
        if (!isValidValue(sensor.getMinValue(), sensor.getMaxValue())) {
            throw new ApplicationException("Giá trị cập nhật không hợp lệ: min_value phải nhỏ hơn max_value");
        }

        // Lấy thông tin SensorType hiện tại của bản ghi để đối chiếu dải đo
        if (sensor.getSensorTypeId() != null) {
            SensorType sensorType = sensorTypeRepository.findById(sensor.getSensorTypeId())
                    .orElseThrow(() -> new ApplicationException("Không tìm thấy Loại cảm biến gắn với thiết bị này"));

            // Kiểm tra xem giới hạn mới có vượt ngưỡng cho phép của loại cảm biến không
            validateSensorRange(sensor.getMinValue(), sensor.getMaxValue(), sensorType);
        }

        return sensorRepository.save(sensor);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<SensorResponseDTO> filter(SensorSearchRequest sensorSearchRequest) {
        Specification<Sensor> sensorSpecification = GenericSpecification.searchByDto(sensorSearchRequest);

        Pageable pageable = PageRequest.of(
                sensorSearchRequest.getPage(),
                sensorSearchRequest.getSize(),
                Sort.by(Sort.Direction.fromString(sensorSearchRequest.getSortDir()), sensorSearchRequest.getSortBy()));

        return sensorRepository.findAll(sensorSpecification, pageable).map(sensorMapper::toResponse);
    }

    @Override
    @Transactional
    public Sensor delete(String id) {
        Sensor sensor = sensorRepository.findById(id)
                .orElseThrow(() -> new ApplicationException("Sensor not found id " + id));
        if (Boolean.TRUE.equals(sensor.getIsDeleted())){
            throw new ApplicationException("Sensor id: " + id + " đã được xóa");
        }
        sensor.setIsDeleted(true);
        return sensorRepository.save(sensor);
    }

    @Override
    @Transactional
    public Sensor findById(String id) {
        Sensor sensor = sensorRepository.findById(id)
                .orElseThrow(() -> new ApplicationException("Sensor not found id " + id));

        return sensor;
    }

    @Override
    public List<SensorResponseDTO> verifySensorsBatch(BatchSensorVerifyRequest request) {
        // 1. Truy vấn nhanh danh sách cảm biến hợp lệ từ MySQL
        List<Sensor> sensors = sensorRepository.validateSensorsBatch(request.getGatewayId(), request.getSensorIds());
        if (sensors.isEmpty()) {
            return List.of();
        }

        // 2. Thu thập danh sách sensorTypeId không trùng lặp để truy vấn gộp (Tránh lỗi N+1 Query)
        List<String> typeIds = sensors.stream()
                .map(Sensor::getSensorTypeId)
                .distinct()
                .collect(Collectors.toList());

        // 3. Truy vấn duy nhất 1 câu SQL lấy về thông tin các loại cảm biến
        List<SensorType> types = sensorTypeRepository.findAllById(typeIds);
        Map<String, SensorType> typeMap = types.stream()
                .collect(Collectors.toMap(SensorType::getId, t -> t));

        // 4. Ánh xạ dữ liệu sang DTO và gán thông tin đơn vị đo lường 'unit'
        return sensors.stream()
                .map(sensor -> {
                    SensorResponseDTO dto = sensorMapper.toResponse(sensor);
                    SensorType type = typeMap.get(sensor.getSensorTypeId());
                    if (type != null) {
                        dto.setUnit(type.getUnit()); // 🌟 Gán trường unit lấy từ bảng sensor_types
                    }
                    return dto;
                })
                .collect(Collectors.toList());
    }

    /**
     * Hàm helper kiểm tra tính logic cơ bản của min-max đầu vào
     */
    private boolean isValidValue (Double min, Double max){
        if (min == null || max == null) {
            return true;
        }
        return min < max;
    }

    /**
     * Hàm helper so sánh dải đo thiết lập (Sensor) với dải đo giới hạn của nhà sản xuất (SensorType)
     * Giả định SensorType có 2 thuộc tính là minRange và maxRange định nghĩa dải vật lý tối đa.
     */
    private void validateSensorRange(Double minValue, Double maxValue, SensorType sensorType) {
        // Chỉ kiểm tra khi cả cấu hình thiết bị và giới hạn của loại thiết bị đều có dữ liệu
        if (minValue != null && sensorType.getMinRange() != null) {
            if (minValue < sensorType.getMinRange()) {
                throw new ApplicationException("Giá trị min_value (" + minValue + ") không được nhỏ hơn giới hạn cho phép của Loại cảm biến (" + sensorType.getMinRange() + ")");
            }
        }

        if (maxValue != null && sensorType.getMaxRange() != null) {
            if (maxValue > sensorType.getMaxRange()) {
                throw new ApplicationException("Giá trị max_value (" + maxValue + ") không được vượt quá giới hạn cho phép của Loại cảm biến (" + sensorType.getMaxRange() + ")");
            }
        }
    }
}