package com.example.deviceservice.dto.request.Gateway;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateGatewayRequest {

    @NotBlank(message = "ID của Trạm sở hữu (stationId) không được để trống")
    @Size(min = 36, max = 36, message = "ID của Trạm phải đúng định dạng UUID 36 ký tự")
    private String stationId;

    @NotBlank(message = "Mã Gateway không được để trống")
    @Size(max = 100, message = "Mã Gateway không được vượt quá 100 ký tự")
    private String code;

    @Size(max = 100, message = "Số Serial không được vượt quá 100 ký tự")
    private String serialNumber;

    @Size(max = 100, message = "Tên Model không được vượt quá 100 ký tự")
    private String model;

    @Size(max = 100, message = "Phiên bản Firmware không được vượt quá 100 ký tự")
    private String firmwareVersion;

    @Pattern(regexp = "^$|^(([0-9]|[1-9][0-9]|1[0-9]{2}|2[0-4][0-9]|25[0-5])\\.){3}([0-9]|[1-9][0-9]|1[0-9]{2}|2[0-4][0-9]|25[0-5])$",
            message = "Địa chỉ IP không đúng định dạng IPv4")
    private String ipAddress;

    @Pattern(regexp = "^$|^([0-9A-Fa-f]{2}[:-]){5}([0-9A-Fa-f]{2})$",
            message = "Địa chỉ MAC không đúng định dạng")
    private String macAddress;
}