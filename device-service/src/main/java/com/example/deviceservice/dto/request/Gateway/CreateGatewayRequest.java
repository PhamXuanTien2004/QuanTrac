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

    @NotBlank(message = "Tên Trạm sở hữu (stationName) không được để trống")
    @Size(max = 255, message = "Tên Trạm không được vượt quá 255 ký tự")
    private String stationName;

    @NotBlank(message = "Mã Gateway không được để trống")
    @Size(max = 100, message = "Mã Gateway không được vượt quá 100 ký tự")
    private String code;
}