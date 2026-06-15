package com.example.deviceservice.dto.request.Sensor;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.Instant;
import java.time.LocalDate;

@Data
public class SensorUpdateDTO {

    @NotBlank
    private String id;

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
