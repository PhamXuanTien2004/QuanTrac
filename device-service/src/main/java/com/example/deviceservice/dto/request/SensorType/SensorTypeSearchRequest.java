package com.example.deviceservice.dto.request.SensorType;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SensorTypeSearchRequest {
    private String keyword; // Tìm kiếm chung theo code hoặc name
    private String unit;    // Lọc theo đơn vị đo

    // Các trường phân trang mặc định
    private int page = 0;
    private int size = 10;
    private String sortBy = "createdDate";
    private String sortDir = "desc";
}