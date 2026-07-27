package com.iot.notification.controller;

import com.iot.notification.dto.BaseResponse;
import com.iot.notification.dto.ReportRequest;
import com.iot.notification.service.ReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/report")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;

    @PostMapping("/export")
    public ResponseEntity<BaseResponse<String>> exportReport(@RequestBody ReportRequest request) {
        reportService.generateAndSendReport(request);
        return ResponseEntity.ok(BaseResponse.<String>builder()
                .success(true)
                .message("Báo cáo đang được xử lý và sẽ gửi qua email!")
                .data("OK")
                .build());
    }
}
