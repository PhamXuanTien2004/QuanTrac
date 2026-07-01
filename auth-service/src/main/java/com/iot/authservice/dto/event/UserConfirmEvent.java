package com.iot.authservice.dto.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserConfirmEvent {
    private String keycloakId;
    private Status status;

    public enum Status{
        ACCEPTED, REJECTED
    }
}
