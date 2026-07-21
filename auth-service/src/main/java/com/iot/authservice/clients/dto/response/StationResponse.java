package com.iot.authservice.clients.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class StationResponse {
    private String id;
    private String stationId;
    private String name;
    private String stationName;

    public String getEffectiveId() {
        return (id != null && !id.isEmpty()) ? id : stationId;
    }

    public String getEffectiveName() {
        return (name != null && !name.isEmpty()) ? name : stationName;
    }
}
