package com.example.deviceservice.dto.request;

import lombok.Data;
import java.util.List;

@Data
public class BatchSensorVerifyRequest {
    private String gatewayId;
    private List<String> sensorIds;
}