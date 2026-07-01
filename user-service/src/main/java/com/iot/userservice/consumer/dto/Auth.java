package com.iot.userservice.consumer.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
@JsonIgnoreProperties(ignoreUnknown = true)
public class Auth {
    private String eventType;
    private String userId;
    private String username;
    private String email;
    private String fullName;
    private String phone;
    private String stationId;
}
