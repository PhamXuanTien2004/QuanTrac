package com.iot.ingestion.clients.dto.request;

import lombok.Builder;
import lombok.Data;
import java.util.List;

@Data
@Builder
public class BatchSensorVerifyRequest {
    private String gatewayId;
    private List<String> sensorIds;
}