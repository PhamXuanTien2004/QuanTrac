package com.iot.userservice.event.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class UserCreatedEvent {

    private String eventType;
    private String userId;
    private String username;
    private String email;
    private String fullName;
    private String phone;
    private String stationId;
    private String role;
    private String notificationMethod;
    private Instant occurredAt;
}
