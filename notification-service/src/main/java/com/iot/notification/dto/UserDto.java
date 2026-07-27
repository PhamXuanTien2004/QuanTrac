package com.iot.notification.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserDto {
    private String id;
    private String username;
    private String fullName;
    private String email;
    private String phone;
    private String stationId;
    private String role;
    private String notificationMethod; // EMAIL, SMS, ALL, NONE
}
