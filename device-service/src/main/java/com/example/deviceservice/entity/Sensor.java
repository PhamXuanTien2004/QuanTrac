package com.example.deviceservice.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

import java.time.Instant;
import java.time.LocalDate;


@Entity
@Table(name = "sensors", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"sensor_code", "deleted_at"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Sensor extends BaseEntity{

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", length = 36, nullable = false)
    private String id;

    @Column(name = "gateway_id", length = 36, nullable = false)
    @NotBlank
    private String gatewayId;

    @Column(name = "sensor_type_id", length = 36, nullable = false)
    @NotBlank
    private String sensorTypeId;

    @Column(name = "sensor_code", length = 100, nullable = false)
    @NotBlank
    private String sensorCode;

    @Column(name = "name", length = 255)
    private String name;

    @Column(name = "model", length = 100)
    private String model;

    @Column(name = "manufacturer", length = 255)
    private String manufacturer;

    @Column(name = "installation_date")
    private Instant installationDate;

    @Column(name = "calibration_date")
    private Instant calibrationDate;


    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 50, nullable = false)
    private Status status;

}
