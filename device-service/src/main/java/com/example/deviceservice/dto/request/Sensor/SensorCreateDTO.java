package com.example.deviceservice.dto.request.Sensor;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.time.Instant;
import java.time.LocalDate;

@Data
public class SensorCreateDTO {

    @NotBlank(message = "Gateway ID không được để trống")
    @Size(min = 36, max = 36, message = "Gateway ID phải đúng 36 ký tự")
    private String gatewayId;

    @NotBlank(message = "Sensor Type ID không được để trống")
    @Size(min = 36, max = 36, message = "Sensor Type ID phải đúng 36 ký tự")
    private String sensorTypeId;

    @NotBlank(message = "Sensor Code không được để trống")
    @Size(max = 100, message = "Sensor Code tối đa 100 ký tự")
    private String sensorCode;

    @Size(max = 255, message = "Tên sensor tối đa 255 ký tự")
    private String name;

    @Size(max = 100, message = "Model tối đa 100 ký tự")
    private String model;

    @Size(max = 255, message = "Nhà sản xuất tối đa 255 ký tự")
    private String manufacturer;

    private Instant installationDate;
    private Instant calibrationDate;
    private Double minValue;
    private Double maxValue;

    @Size(max = 50)
    private String status;
}
