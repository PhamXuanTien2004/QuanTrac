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

    private String stationName;

    @Size(max = 100, message = "Mã Gateway không được vượt quá 100 ký tự")
    private String code;

    private Status status;

    private Boolean isDeleted;

    private Instant lastSeen;
}