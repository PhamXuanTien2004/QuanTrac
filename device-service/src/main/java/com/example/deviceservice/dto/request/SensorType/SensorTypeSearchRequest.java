package com.example.deviceservice.dto.request.SensorType;

import com.example.deviceservice.dto.request.BaseSearchRequest;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SensorTypeSearchRequest extends BaseSearchRequest {
    private String keyword; // Tìm kiếm chung theo code hoặc name
    private String unit;    // Lọc theo đơn vị đo

}