package com.example.deviceservice.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.LocalDate;

@Entity
@Getter
@Setter
@Table(name = "stations")
@NoArgsConstructor
@AllArgsConstructor
public class Station extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", length = 36, nullable = false)
    private String id;

    @NotBlank(message = "Mã trạm không được để trống")
    @Size(max = 100, message = "Mã trạm không được vượt quá 100 ký tự")
    @Column(name = "station_code", length = 100, nullable = false, unique = true)
    private String stationCode;

    @NotBlank(message = "Tên trạm không được để trống")
    @Size(max = 255, message = "Tên trạm không được vượt quá 255 ký tự")
    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "address", columnDefinition = "TEXT")
    private String address;

    @Min(value = -90, message = "Vĩ độ không được nhỏ hơn -90")
    @Max(value = 90, message = "Vĩ độ không được lớn hơn 90")
    @Column(name = "latitude")
    private Double latitude;

    @Min(value = -180, message = "Kinh độ không được nhỏ hơn -180")
    @Max(value = 180, message = "Kinh độ không được lớn hơn 180")
    @Column(name = "longitude")
    private Double longitude;

    @PastOrPresent(message = "Ngày lắp đặt không được là ngày trong tương lai")
    @Column(name = "installation_date")
    private LocalDate installationDate;

    @NotNull(message = "Trạng thái không được để trống")
    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 50)
    private Status status = Status.ACTIVE;
}