package com.example.deviceservice.dto.request.Station;

import com.example.deviceservice.entity.Status;
import jakarta.validation.constraints.*;
import lombok.*;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateStationRequest {

    @NotBlank(message = "Mã trạm không được để trống")
    @Size(max = 100, message = "Mã trạm không được vượt quá 100 ký tự")
    @Pattern(regexp = "^[A-Z0-BA-Z0-9_-]+$", message = "Mã trạm chỉ được chứa ký tự chữ, số, dấu gạch nối (-) hoặc gạch dưới (_)")
    private String stationCode;

    @NotBlank(message = "Tên trạm không được để trống")
    @Size(max = 255, message = "Tên trạm không được vượt quá 255 ký tự")
    private String name;

    private String description;

    private String address;

    @Min(value = -90, message = "Vĩ độ không được nhỏ hơn -90")
    @Max(value = 90, message = "Vĩ độ không được lớn hơn 90")
    private Double latitude;

    @Min(value = -180, message = "Kinh độ không được nhỏ hơn -180")
    @Max(value = 180, message = "Kinh độ không được lớn hơn 180")
    private Double longitude;

    @PastOrPresent(message = "Ngày lắp đặt không được là ngày trong tương lai")
    private LocalDate installationDate;

    @NotNull(message = "Trạng thái không được để trống")
    private Status status;
}