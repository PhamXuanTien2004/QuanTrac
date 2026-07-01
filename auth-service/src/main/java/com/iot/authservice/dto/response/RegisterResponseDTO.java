package com.iot.authservice.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class RegisterResponseDTO {
    private String userId;
    private String username;
    private Status status;
    private String message;

    public enum Status {
        PENDING, SUCCESS, FAILED
    }

}