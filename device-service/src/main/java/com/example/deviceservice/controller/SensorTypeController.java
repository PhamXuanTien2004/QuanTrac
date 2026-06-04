package com.example.deviceservice.controller;

import com.example.deviceservice.common.BaseResponse;
import com.example.deviceservice.dto.request.SensorType.SensorTypeCreateRequest;
import com.example.deviceservice.dto.response.SensorType.SensorTypeResponse;
import com.example.deviceservice.entity.SensorType;
import com.example.deviceservice.service.SensorTypeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/sensor-types")
@RequiredArgsConstructor
public class SensorTypeController {
    private final SensorTypeService sensorTypeService;

    @PostMapping
    public ResponseEntity<BaseResponse<SensorType>> create(@Valid @RequestBody SensorTypeCreateRequest sensorTypeCreateRequest) {
        SensorType sensorTypeResponse =  sensorTypeService.create(sensorTypeCreateRequest);
        BaseResponse<SensorType> response = BaseResponse.success(sensorTypeResponse);
        response.setMessage("Tạo thành công SensorTypes");
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
