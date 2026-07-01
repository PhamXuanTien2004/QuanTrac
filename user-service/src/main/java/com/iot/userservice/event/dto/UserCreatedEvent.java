package com.iot.userservice.event.dto;

import lombok.Data;

import java.time.Instant;

@Data
public class UserCreatedEvent {

    private String eventType;
    private String userId;
    private String username;
    private String email;
    private String fullName;
    private String phone;
    private String stationId;
    private Instant occurredAt;
}
