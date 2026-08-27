package com.example.deviceservice.dto.response; // hoặc package hiện tại của bạn

import com.example.deviceservice.entity.Status;
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
    private Instant lastSeen;
    private Status status;
    private Boolean isDeleted;
    private String createdBy;
    private Instant createdDate;
    private String lastModifiedBy;
    private Instant lastModifiedDate;
}