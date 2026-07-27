package com.iot.notification.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReportRequest {
    private String stationId;
    private String stationName;
    private Instant startDate;
    private Instant endDate;
    private String format; // "EXCEL" or "PDF"
    private String emailTo;
}
