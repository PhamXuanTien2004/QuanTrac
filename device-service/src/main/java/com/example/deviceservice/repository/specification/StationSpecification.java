package com.example.deviceservice.repository.specification;

import com.example.deviceservice.dto.request.Station.FilterStationRequest;
import com.example.deviceservice.entity.Station;
import org.springframework.data.jpa.domain.Specification;

public class StationSpecification {

    public static Specification<Station> filterWithRequest(FilterStationRequest request) {
        return BaseSpecification.<Station>notDeleted()
                .and(BaseSpecification.equal("id", request.getId()))
                .and(BaseSpecification.like("name", request.getName()))
                .and(BaseSpecification.like("stationCode", request.getStationCode()))
                .and(BaseSpecification.equal("status", request.getStatus()));
    }
}