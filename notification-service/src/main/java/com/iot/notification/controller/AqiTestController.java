package com.iot.notification.controller;

import com.iot.notification.job.AqiCalculationJob;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/notification/aqi")
@RequiredArgsConstructor
public class AqiTestController {

    private final AqiCalculationJob aqiCalculationJob;

    @PostMapping("/calculate")
    public ResponseEntity<String> triggerAqiCalculation() {
        // Chạy job thủ công không đợi cron
        aqiCalculationJob.calculateAqi();
        return ResponseEntity.ok("Đã kích hoạt tiến trình tính toán AQI thủ công thành công!");
    }
}
