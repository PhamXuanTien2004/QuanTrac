package com.example.deviceservice.dto.request.Station;

import com.example.deviceservice.entity.Status;
import jakarta.validation.constraints.*;
import lombok.*;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FilterStationRequest {
    private String id;

    private String stationCode;

    private String name;

    private LocalDate installationDate;

    private Status status;

    private Boolean isDeleted;
}
