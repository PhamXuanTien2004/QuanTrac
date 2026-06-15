package com.example.deviceservice.controller;

import com.example.deviceservice.common.BaseResponse;
import com.example.deviceservice.dto.request.Sensor.SensorCreateDTO;
import com.example.deviceservice.dto.request.Sensor.SensorSearchRequest;
import com.example.deviceservice.dto.request.Sensor.SensorUpdateDTO;
import com.example.deviceservice.dto.request.SensorType.SensorTypeCreateRequest;
import com.example.deviceservice.dto.request.SensorType.SensorTypeSearchRequest;
import com.example.deviceservice.dto.request.SensorType.SensorTypeUpdateRequest;
import com.example.deviceservice.dto.response.SensorResponseDTO;
import com.example.deviceservice.dto.response.SensorTypeResponse;
import com.example.deviceservice.entity.Sensor;
import com.example.deviceservice.entity.SensorType;
import com.example.deviceservice.mapper.SensorMapper;
import com.example.deviceservice.service.SensorService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/sensors")
@RequiredArgsConstructor
public class SensorController {
    private final SensorService sensorService;
    private final SensorMapper sensorMapper;

    @PostMapping
    public ResponseEntity<BaseResponse<Sensor>> create(@Valid @RequestBody SensorCreateDTO sensorCreateDTO) {
        Sensor sensor =  sensorService.create(sensorCreateDTO);
        BaseResponse<Sensor> response = BaseResponse.success(sensor);
        response.setMessage("Tạo thành công Sensor");
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping
    public ResponseEntity<BaseResponse<Sensor>> update(@Valid @RequestBody SensorUpdateDTO sensorTypeUpdateDTO){
        Sensor sensor = sensorService.update(sensorTypeUpdateDTO);
        BaseResponse<Sensor> response = BaseResponse.success(sensor);
        response.setMessage("Cập nhật Sensor thành công");
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    @PostMapping("/filter")
    public ResponseEntity<BaseResponse<Page<SensorResponseDTO>>> filter(SensorSearchRequest searchRequest){
        // 1. Gọi service để lấy dữ liệu đã phân trang và filter
        Page<SensorResponseDTO> filterResult = sensorService.filter(searchRequest);

        // 2. Đóng gói vào cấu trúc BaseResponse quen thuộc
        BaseResponse<Page<SensorResponseDTO>> response = BaseResponse.success(filterResult);
        response.setMessage("Lấy danh sách cảm biến thành công");

        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<BaseResponse<Void>> delete (@Valid @PathVariable String id){
        sensorService.delete(id);

        BaseResponse<Void> response = BaseResponse.success(null);
        response.setMessage("Soft deleted Sensor id " + id);
        return ResponseEntity.ok(response);
    }
}
