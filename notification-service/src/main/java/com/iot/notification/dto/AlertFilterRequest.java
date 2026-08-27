package com.iot.notification.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AlertFilterRequest {
    private String stationId;
    private Instant startDate;
    private Instant endDate;
    @Builder.Default
    private int page = 0;
    @Builder.Default
    private int size = 20;
}
