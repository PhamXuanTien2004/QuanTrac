package com.iot.authservice.dto.event;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;

@Data
@Builder
public class UserEvent {
    private String eventType;
    private String userId;
    private String username;
    private String fullName;
    private String phone;
    private String stationId;
    private String role;
    @JsonFormat(
            shape = JsonFormat.Shape.STRING,
            pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX",
            timezone = "Asia/Ho_Chi_Minh")
    private Instant occurredAt;
}