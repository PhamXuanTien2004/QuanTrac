package com.example.deviceservice.dto.request.Gateway;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GatewayFilterRequest {
    private String stationId;
    private String code;
    private String serialNumber;
    private String model;
    private String ipAddress;
    private String status;
}