package com.example.deviceservice.repository.specification;

import com.example.deviceservice.dto.request.SensorType.SensorTypeSearchRequest;
import com.example.deviceservice.entity.SensorType;
import org.springframework.data.jpa.domain.Specification;

public class SensorTypeSpecification extends BaseSpecification {
    public static Specification<SensorType> filterWithrequest(SensorTypeSearchRequest sensorTypeSearchRequest) {
        return BaseSpecification.<SensorType>notDeleted()
                .and(BaseSpecification.equal("id",  sensorTypeSearchRequest.getId()))
                .and(BaseSpecification.equal("name", sensorTypeSearchRequest.getName()))
                .and(BaseSpecification.equal("unit", sensorTypeSearchRequest.getUnit()))
                .and(BaseSpecification.equal("minRange", sensorTypeSearchRequest.getMinRange()))
                .and(BaseSpecification.equal("maxRange", sensorTypeSearchRequest.getMaxRange()));

    }
}
