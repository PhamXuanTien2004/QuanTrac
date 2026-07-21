package com.iot.userservice.event.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConfirmEvent {
    private String userId;
    @Builder.Default
    private String status = "ACTIVE";

    public enum Status{
        ACCEPTED, REJECTED
    }
}
