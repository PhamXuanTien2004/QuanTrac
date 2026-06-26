package com.example.deviceservice.repository.specification;

import com.example.deviceservice.dto.request.Sensor.SensorSearchRequest;
import com.example.deviceservice.entity.Sensor;
import org.springframework.data.jpa.domain.Specification;

public class SensorSpecification {

    public static Specification<Sensor> filterWithRequest(SensorSearchRequest request) {
        return BaseSpecification.<Sensor>notDeleted()
                .and(BaseSpecification.equal("gatewayId", request.getGatewayId()))
                .and(BaseSpecification.equal("sensorTypeId", request.getSensorTypeId()))
                .and(BaseSpecification.like("name", request.getName()))
                .and(BaseSpecification.like("sensorCode", request.getSensorCode()))
                .and(BaseSpecification.equal("status", request.getStatus()));
    }
}