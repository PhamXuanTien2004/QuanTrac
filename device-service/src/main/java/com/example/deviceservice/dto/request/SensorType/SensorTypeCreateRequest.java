package com.example.deviceservice.dto.request.SensorType;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SensorTypeCreateRequest {
    @NotBlank(message = "Mã loại cảm biến không được để trống")
    @Size(max = 100, message = "Mã loại cảm biến không vượt quá 100 ký tự")
    private String code;

    @NotBlank(message = "Tên loại cảm biến không được để trống")
    @Size(max = 255, message = "Tên loại cảm biến không vượt quá 255 ký tự")
    private String name;

    @Size(max = 50, message = "Đơn vị đo không vượt quá 50 ký tự")
    private String unit;

    private String description;

    private Double minRange;

    private Double maxRange;
}
