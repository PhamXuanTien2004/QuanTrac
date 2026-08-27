package com.iot.notification.controller;

import com.iot.notification.dto.BaseResponse;
import com.iot.notification.entity.AlertHistory;
import com.iot.notification.repository.AlertHistoryRepository;
import com.iot.notification.repository.AqiHistoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import com.iot.notification.job.AqiCalculationJob;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final AlertHistoryRepository alertHistoryRepository;
    private final AqiHistoryRepository aqiHistoryRepository;
    private final AqiCalculationJob aqiCalculationJob;

    @PostMapping("/test/trigger-aqi")
    public ResponseEntity<BaseResponse<String>> triggerAqiCalculation() {
        aqiCalculationJob.calculateAqi();
        return ResponseEntity.ok(BaseResponse.<String>builder()
                .success(true)
                .message("Đã kích hoạt tính toán AQI thủ công")
                .data("Thành công")
                .build());
    }

    @GetMapping("/alerts/station/{stationId}")
    public ResponseEntity<BaseResponse<List<AlertHistory>>> getAlertHistoryByStation(@PathVariable String stationId) {
        List<AlertHistory> alerts = alertHistoryRepository.findTop20ByStationIdOrderByTimestampDesc(stationId);
        return ResponseEntity.ok(BaseResponse.<List<AlertHistory>>builder()
                .success(true)
                .message("Lấy lịch sử thành công")
                .data(alerts)
                .build());
    }

    @PostMapping("/alerts/filter")
    public ResponseEntity<BaseResponse<org.springframework.data.domain.Page<AlertHistory>>> filterAlerts(
            @RequestBody com.iot.notification.dto.AlertFilterRequest request) {
        
        org.springframework.data.domain.Pageable pageable = org.springframework.data.domain.PageRequest.of(
                request.getPage(), request.getSize());
        
        org.springframework.data.domain.Page<AlertHistory> pageResult = alertHistoryRepository
                .findByStationIdAndTimestampBetweenOrderByTimestampDesc(
                        request.getStationId(), 
                        request.getStartDate(), 
                        request.getEndDate(), 
                        pageable);
                        
        return ResponseEntity.ok(BaseResponse.<org.springframework.data.domain.Page<AlertHistory>>builder()
                .success(true)
                .message("Lọc cảnh báo thành công")
                .data(pageResult)
                .build());
    }

    @GetMapping("/aqi/station/{stationId}/latest")
    public ResponseEntity<BaseResponse<com.iot.notification.entity.AqiHistory>> getLatestAqi(@PathVariable String stationId) {
        java.util.Optional<com.iot.notification.entity.AqiHistory> latestAqi = 
                aqiHistoryRepository.findFirstByStationIdOrderByCalculatedAtDesc(stationId);
                
        return ResponseEntity.ok(BaseResponse.<com.iot.notification.entity.AqiHistory>builder()
                .success(true)
                .message("Lấy AQI thành công")
                .data(latestAqi.orElse(null))
                .build());
    }

    @GetMapping("/aqi/station/{stationId}/history")
    public ResponseEntity<BaseResponse<List<com.iot.notification.entity.AqiHistory>>> getAqiHistory(
            @PathVariable String stationId,
            @RequestParam @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE_TIME) java.time.Instant startTime,
            @RequestParam @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE_TIME) java.time.Instant endTime) {
        
        List<com.iot.notification.entity.AqiHistory> history = aqiHistoryRepository.findByStationIdAndCalculatedAtBetweenOrderByCalculatedAtDesc(stationId, startTime, endTime);
        
        return ResponseEntity.ok(BaseResponse.<List<com.iot.notification.entity.AqiHistory>>builder()
                .success(true)
                .message("Lấy AQI lịch sử thành công")
                .data(history)
                .build());
    }

    @GetMapping("/aqi/latest")
    public ResponseEntity<BaseResponse<List<com.iot.notification.entity.AqiHistory>>> getLatestAqiAll() {
        List<com.iot.notification.entity.AqiHistory> latestAqis = aqiHistoryRepository.findLatestAqiForAllStations();
                
        return ResponseEntity.ok(BaseResponse.<List<com.iot.notification.entity.AqiHistory>>builder()
                .success(true)
                .message("Lấy AQI thành công")
                .data(latestAqis)
                .build());
    }
}
