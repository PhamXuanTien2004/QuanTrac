package com.iot.notification.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

@Entity
@Table(name = "aqi_history")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AqiHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "station_id", nullable = false)
    private String stationId;

    @Column(name = "aqi_value", nullable = false)
    private Integer aqiValue;

    @Column(name = "main_pollutant")
    private String mainPollutant;

    @Column(name = "level")
    private String level; // VD: Tốt, Kém, Xấu...

    @Column(name = "calculated_at", nullable = false)
    @CreationTimestamp
    private Instant calculatedAt;
}
