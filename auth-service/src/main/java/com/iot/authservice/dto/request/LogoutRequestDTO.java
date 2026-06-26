package com.iot.authservice.dto.request;

import lombok.Data;

@Data
public class LogoutRequestDTO {
    private String refreshToken;
}
