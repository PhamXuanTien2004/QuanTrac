package com.iot.ingestion.clients.dto.response;

import lombok.*;
import java.time.Instant;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GatewayResponse {
    private String id;
    private String stationId;
    private String code;
    private String serialNumber;
    private String model;
    private String firmwareVersion;
    private String ipAddress;
    private String macAddress;
    private Instant lastSeen;
    private String status; // Sử dụng kiểu String để dễ map trạng thái ACTIVE/INACTIVE
    private Boolean isDeleted;
}