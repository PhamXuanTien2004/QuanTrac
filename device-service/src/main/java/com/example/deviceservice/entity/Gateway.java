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
@Table(name = "gateways", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"gateway_code", "deleted_at"})
})
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
    @Column(name = "gateway_code", length = 100, nullable = false)
    private String code;

    @Column(name = "last_seen")
    private Instant lastSeen;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 50, nullable = false)
    private Status status;
}