package com.example.deviceservice.controller;

import com.example.deviceservice.common.BaseResponse;
import com.example.deviceservice.dto.request.SensorType.SensorTypeCreateRequest;
import com.example.deviceservice.dto.request.SensorType.SensorTypeSearchRequest;
import com.example.deviceservice.dto.request.SensorType.SensorTypeUpdateRequest;
import com.example.deviceservice.dto.response.SensorTypeResponse;
import com.example.deviceservice.entity.SensorType;
import com.example.deviceservice.mapper.SensorTypesMapper;
import com.example.deviceservice.service.SensorTypeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/sensor-types")
@RequiredArgsConstructor
public class SensorTypeController {

    private final SensorTypeService sensorTypeService;
    private final SensorTypesMapper sensorTypesMapper;

    @PostMapping
    public ResponseEntity<BaseResponse<SensorType>> create(@Valid @RequestBody SensorTypeCreateRequest sensorTypeCreateRequest) {
        SensorType sensorTypeResponse =  sensorTypeService.create(sensorTypeCreateRequest);
        BaseResponse<SensorType> response = BaseResponse.success(sensorTypeResponse);
        response.setMessage("Tạo thành công SensorTypes");
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping
    public ResponseEntity<BaseResponse<SensorTypeResponse>> update(@Valid @RequestBody SensorTypeUpdateRequest sensorTypeUpdateRequest){
        SensorType sensorType = sensorTypeService.update(sensorTypeUpdateRequest);

        SensorTypeResponse sensorTypeResponse = sensorTypesMapper.toResponse(sensorType);

        BaseResponse<SensorTypeResponse> response = BaseResponse.success(sensorTypeResponse);
        response.setMessage("Cập nhật SensorType thành công");
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    @PostMapping("/filter")
    public ResponseEntity<BaseResponse<Page<SensorTypeResponse>>> filter(
            SensorTypeSearchRequest searchRequest){
        // 1. Gọi service để lấy dữ liệu đã phân trang và filter
        Page<SensorTypeResponse> filterResult = sensorTypeService.filter(searchRequest);

        // 2. Đóng gói vào cấu trúc BaseResponse quen thuộc
        BaseResponse<Page<SensorTypeResponse>> response = BaseResponse.success(filterResult);
        response.setMessage("Lấy danh sách Loại cảm biến thành công");

        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<BaseResponse<Void>> delete (@Valid @PathVariable String id){
        sensorTypeService.delete(id);

        BaseResponse<Void> response = BaseResponse.success(null);
        response.setMessage("Soft deleted SensorType id " + id);
        return ResponseEntity.ok(BaseResponse.success(null));
    }

}