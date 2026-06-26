package com.iot.alert.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "alert_logs")
@Data
public class AlertLog extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "station_id", length = 36, nullable = false)
    private String stationId;

    @Column(name = "sensor_id", length = 50, nullable = false)
    private String sensorId;

    @Column(name = "metric_name", length = 50)
    private String metricName;

    @Column(name = "violated_value")
    private Double violatedValue;

    @Column(name = "severity", length = 20)
    private String severity = "CRITICAL"; // Mặc định là CRITICAL khi vượt ngưỡng

    @Column(name = "message", columnDefinition = "TEXT")
    private String message;

    @Column(name = "triggered_at")
    private LocalDateTime triggeredAt;

    @Column(name = "is_resolved")
    private Boolean isResolved = false;

    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;

    @Column(name = "resolved_by", length = 100)
    private String resolvedBy;

    @Column(name = "resolution_note", columnDefinition = "TEXT")
    private String resolutionNote;
}