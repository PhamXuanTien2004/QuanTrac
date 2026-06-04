package com.example.deviceservice.dto.request.SensorType;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SensorTypeUpdateRequest {
    @NotBlank
    private String id;

    @Size(max = 255, message = "Tên loại cảm biến không vượt quá 255 ký tự")
    private String name;

    @Size(max = 50, message = "Đơn vị đo không vượt quá 50 ký tự")
    private String unit;

    private String description;

    private Double minRange;

    private Double maxRange;
}
