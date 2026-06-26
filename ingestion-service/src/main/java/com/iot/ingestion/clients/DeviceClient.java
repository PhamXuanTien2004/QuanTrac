package com.iot.ingestion.clients;

import com.iot.ingestion.clients.dto.request.BatchSensorVerifyRequest;
import com.iot.ingestion.clients.dto.response.DeviceResponse;
import com.iot.ingestion.clients.dto.response.GatewayResponse;

import java.util.List;

public interface DeviceClient {
    GatewayResponse findGatewayById(String gatewayId);
    List<DeviceResponse> verifySensorsBatch(BatchSensorVerifyRequest request);
}