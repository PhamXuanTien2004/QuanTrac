package com.example.deviceservice.repository.specification;

import com.example.deviceservice.dto.request.Gateway.GatewayFilterRequest;
import com.example.deviceservice.entity.Gateway;
import org.springframework.data.jpa.domain.Specification;

public class GatewaySpecification {

    public static Specification<Gateway> filterWithRequest(GatewayFilterRequest request) {
        return BaseSpecification.<Gateway>notDeleted() // Tự động lọc isDeleted = false
                .and(BaseSpecification.equal("id", request.getId()))
                .and(BaseSpecification.equal("station.id", request.getStationId())) // Lọc khóa ngoại lồng nhau
                .and(BaseSpecification.like("code", request.getCode()))
                .and(BaseSpecification.equal("status", request.getStatus()));
    }
}