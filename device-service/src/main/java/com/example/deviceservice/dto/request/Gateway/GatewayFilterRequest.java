package com.example.deviceservice.dto.request.Gateway;

import com.example.deviceservice.dto.request.BaseSearchRequest;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GatewayFilterRequest extends BaseSearchRequest {
    private String id;
    private String stationId;
    private String code;
    private String serialNumber;
    private String model;
    private String ipAddress;
    private String status;
}