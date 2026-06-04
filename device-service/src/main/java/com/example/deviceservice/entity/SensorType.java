package com.example.deviceservice.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

@Entity
@Getter
@Setter
@Table(name = "sensor_types")
@NoArgsConstructor
@AllArgsConstructor
public class SensorType extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", length = 36, nullable = false)
    private String id;

    @NotBlank(message = "Mã Sensor Type không được để trống")
    @Size(max = 100, message = "Mã Sensor Type không được vượt quá 100 ký tự")
    @Column(name = "code", length = 100, nullable = false, unique = true)
    private String code;

    @NotBlank(message = "Tên Sensor Types không được để trống")
    @Size(max = 255, message = "Tên Sensor Types không được vượt quá 255 ký tự")
    @Column(name = "name", length = 255, nullable = false)
    private String name;

    @Size(max = 50, message = "Đơn vị đo - Unit không được vượt quá 50 ký tự")
    @Column(name = "unit", length = 50)
    private String unit;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "min_range")
    private Double minRange;

    @Column(name = "max_range")
    private Double maxRange;

}