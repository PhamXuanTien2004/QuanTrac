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
import org.springframework.data.redis.core.StringRedisTemplate;

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
    private final StringRedisTemplate stringRedisTemplate;

    @Override
    @Transactional
    public Sensor create(SensorCreateDTO sensorCreateDTO) {
        Gateway gateway = gatewayRepository.findByCodeIgnoreCase(sensorCreateDTO.getGatewayCode())
                .orElseThrow(() -> new ApplicationException("Không tìm thấy Gateway với Mã: " + sensorCreateDTO.getGatewayCode()));

        SensorType sensorType = sensorTypeRepository.findByNameIgnoreCaseAndDeletedAtIsNull(sensorCreateDTO.getSensorTypeName())
                .orElseThrow(() -> new ApplicationException("Không tìm thấy Loại cảm biến với Tên: " + sensorCreateDTO.getSensorTypeName()));

        // 2. Kiểm tra xem cấu hình có nằm trong khoảng giới hạn vật lý (min-maxRange) của SensorType không
        // (Removed validateSensorRange since min/max is no longer configurable per sensor)

        java.util.Optional<Sensor> existingSensorOpt = sensorRepository.findBySensorCode(sensorCreateDTO.getSensorCode());
        
        if (existingSensorOpt.isPresent()) {
            Sensor existingSensor = existingSensorOpt.get();
            if (!existingSensor.isDeleted()) {
                throw new ApplicationException("Sensor code '" + sensorCreateDTO.getSensorCode() + "' đã tồn tại hệ thống!");
            }
            
            existingSensor.setGatewayId(gateway.getId());
            existingSensor.setSensorTypeId(sensorType.getId());
            existingSensor.setName(sensorCreateDTO.getName());
            existingSensor.setModel(sensorCreateDTO.getModel());
            existingSensor.setManufacturer(sensorCreateDTO.getManufacturer());
            existingSensor.setInstallationDate(sensorCreateDTO.getInstallationDate());
            existingSensor.setCalibrationDate(sensorCreateDTO.getCalibrationDate());
            existingSensor.setStatus(sensorCreateDTO.getStatus() != null ? com.example.deviceservice.entity.Status.valueOf(sensorCreateDTO.getStatus().toUpperCase()) : null);
            existingSensor.setDeletedAt(null);
            
            return sensorRepository.save(existingSensor);
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
            SensorType sensorType = sensorTypeRepository.findByNameIgnoreCaseAndDeletedAtIsNull(sensorUpdateDTO.getSensorTypeName())
                    .orElseThrow(() -> new ApplicationException("Không tìm thấy Loại cảm biến với Tên: " + sensorUpdateDTO.getSensorTypeName()));
            sensor.setSensorTypeId(sensorType.getId());
        }
        
        if (sensorUpdateDTO.getSensorCode() != null && !sensorUpdateDTO.getSensorCode().equals(sensor.getSensorCode())) {
            if (sensorRepository.existsBySensorCodeAndDeletedAtIsNull(sensorUpdateDTO.getSensorCode())) {
                throw new ApplicationException("Sensor code '" + sensorUpdateDTO.getSensorCode() + "' đã tồn tại hệ thống!");
            }
            sensor.setSensorCode(sensorUpdateDTO.getSensorCode());
        }

        // Thực hiện map các thay đổi từ DTO vào Entity
        sensorMapper.updateFromRequest(sensorUpdateDTO, sensor);


        Sensor savedSensor = sensorRepository.save(sensor);
        
        // Xóa cache của sensor trong Redis để ingestion-service bắt buộc phải query lại DB ngay lập tức
        try {
            stringRedisTemplate.delete("sensor:metadata:" + savedSensor.getId());
        } catch (Exception e) {
            // Log lỗi nếu Redis gặp sự cố, nhưng không làm gián đoạn luồng lưu DB
            System.err.println("Lỗi khi xóa Redis cache cho Sensor: " + savedSensor.getId());
        }

        return savedSensor;
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
        if (sensor.isDeleted()){
            throw new ApplicationException("Sensor id: " + id + " đã được xóa");
        }
        sensor.setDeletedAt(java.time.Instant.now());
        
        // Also delete from redis cache
        stringRedisTemplate.delete("sensor:metadata:" + sensor.getId());
        
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


}