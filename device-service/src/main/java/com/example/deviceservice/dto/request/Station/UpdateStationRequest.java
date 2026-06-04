package com.example.deviceservice.dto.request.Station;

import com.example.deviceservice.entity.Status;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class UpdateStationRequest {
    @NotBlank
    private String id;

    private String name;

    private String description;

    private String address;

    private Double latitude;

    private Double longitude;

    private LocalDate installationDate;

    private Status status;

    private Boolean isDeleted;
}
