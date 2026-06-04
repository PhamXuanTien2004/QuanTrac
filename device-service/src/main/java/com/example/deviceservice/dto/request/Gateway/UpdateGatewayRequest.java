package com.example.deviceservice.dto.request.Gateway;

import com.example.deviceservice.entity.Status;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateGatewayRequest {

    @NotBlank(message = "ID thiết bị cần sửa không được để trống")
    private String id;

    private String stationId;

    @Size(max = 100, message = "Mã Gateway không được vượt quá 100 ký tự")
    private String code;

    @Size(max = 100, message = "Số Serial không được vượt quá 100 ký tự")
    private String serialNumber;

    private Status status;

    private String model;
    private String firmwareVersion;

    @Pattern(regexp = "^$|^(([0-9]|[1-9][0-9]|1[0-9]{2}|2[0-4][0-9]|25[0-5])\\.){3}([0-9]|[1-9][0-9]|1[0-9]{2}|2[0-4][0-9]|25[0-5])$",
            message = "Địa chỉ IP không đúng định dạng IPv4")
    private String ipAddress;

    @Pattern(regexp = "^$|^([0-9A-Fa-f]{2}[:-]){5}([0-9A-Fa-f]{2})$",
            message = "Địa chỉ MAC không đúng định dạng")
    private String macAddress;

    private Boolean isDeleted;

    private Instant lastSeen;
}