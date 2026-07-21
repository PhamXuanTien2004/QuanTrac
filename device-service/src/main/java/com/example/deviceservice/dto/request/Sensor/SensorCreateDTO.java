package com.example.deviceservice.dto.request.Sensor;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.time.Instant;
import java.time.LocalDate;

@Data
public class SensorCreateDTO {

    @NotBlank(message = "Gateway Code không được để trống")
    @Size(max = 100, message = "Gateway Code tối đa 100 ký tự")
    private String gatewayCode;

    @NotBlank(message = "Tên Loại Sensor không được để trống")
    @Size(max = 255, message = "Tên Loại Sensor tối đa 255 ký tự")
    private String sensorTypeName;

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
