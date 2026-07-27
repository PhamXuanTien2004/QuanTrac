package com.iot.notification.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SensorDto {
    private String id;
    private String name;
    private Double minValue;
    private Double maxValue;
}
