package com.iot.notification.controller;

import com.iot.notification.dto.BaseResponse;
import com.iot.notification.entity.AlertHistory;
import com.iot.notification.repository.AlertHistoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final AlertHistoryRepository alertHistoryRepository;

    @GetMapping("/alerts/station/{stationId}")
    public ResponseEntity<BaseResponse<List<AlertHistory>>> getAlertHistoryByStation(@PathVariable String stationId) {
        List<AlertHistory> alerts = alertHistoryRepository.findTop20ByStationIdOrderByTimestampDesc(stationId);
        return ResponseEntity.ok(BaseResponse.<List<AlertHistory>>builder()
                .success(true)
                .message("Lấy lịch sử thành công")
                .data(alerts)
                .build());
    }
}
