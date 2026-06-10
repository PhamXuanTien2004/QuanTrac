package com.example.deviceservice.dto.response;

import com.example.deviceservice.entity.Status;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

import java.time.Instant;
import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StationResponse {
    @NotBlank
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