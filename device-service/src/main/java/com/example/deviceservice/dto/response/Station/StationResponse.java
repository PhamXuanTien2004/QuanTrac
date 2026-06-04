package com.example.deviceservice.dto.response.Station;

import com.example.deviceservice.entity.Status;
import lombok.*;

import java.time.Instant;
import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StationResponse {
    private String id;

    private String stationCode;

    private String name;

    private String description;

    private String address;

    private Double latitude;

    private Double longitude;

    private LocalDate installationDate;

    private Status status;

    private Boolean isDeleted;

    private String createdBy;

    private Instant createdDate;

    private String lastModifiedBy;

    private Instant lastModifiedDate;
}