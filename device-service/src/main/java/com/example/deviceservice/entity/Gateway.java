package com.example.deviceservice.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Getter
@Setter
@Table(name = "gateways")
@NoArgsConstructor
@AllArgsConstructor
public class Gateway extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", length = 36, nullable = false)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "station_id", nullable = false, foreignKey = @ForeignKey(name = "FK_GATEWAY_STATION"))
    private Station station;

    @NotBlank(message = "Mã Gateway không được để trống")
    @Size(max = 100)
    @Column(name = "gateway_code", length = 100, nullable = false, unique = true)
    private String code;

    @Size(max = 100)
    @Column(name = "serial_number", length = 100, unique = true)
    private String serialNumber;

    @Size(max = 100)
    @Column(name = "model", length = 100)
    private String model;

    @Size(max = 100)
    @Column(name = "firmware_version", length = 100)
    private String firmwareVersion;

    @Pattern(regexp = "^$|^(([0-9]|[1-9][0-9]|1[0-9]{2}|2[0-4][0-9]|25[0-5])\\.){3}([0-9]|[1-9][0-9]|1[0-9]{2}|2[0-4][0-9]|25[0-5])$",
            message = "Địa chỉ IP không đúng định dạng IPv4")
    @Column(name = "ip_address", length = 100)
    private String ipAddress;

    @Pattern(regexp = "^$|^([0-9A-Fa-f]{2}[:-]){5}([0-9A-Fa-f]{2})$",
            message = "Địa chỉ MAC không đúng định dạng (VD: 00:1A:3F:F1:4C:C6)")
    @Column(name = "mac_address", length = 100)
    private String macAddress;

    @Column(name = "last_seen")
    private Instant lastSeen;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 50, nullable = false)
    private Status status;
}