package com.iot.realtime.controller;

import com.iot.realtime.dto.TelemetryResponse;
import com.iot.realtime.service.InfluxDbService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/api/v1/telemetry")
@RequiredArgsConstructor
public class TelemetryController {

    private final InfluxDbService influxDbService;

    @GetMapping("/realtime")
    public ResponseEntity<List<TelemetryResponse>> getRealtimeTelemetry(@RequestParam String stationId) {
        List<TelemetryResponse> latestData = influxDbService.getLatestTelemetry(stationId);
        return ResponseEntity.ok(latestData);
    }

    @GetMapping("/history")
    public ResponseEntity<List<TelemetryResponse>> getHistoricalTelemetry(
            @RequestParam String stationId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant startTime,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant endTime) {
        
        List<TelemetryResponse> historicalData = influxDbService.getHistoricalTelemetry(stationId, startTime, endTime);
        return ResponseEntity.ok(historicalData);
    }
}
