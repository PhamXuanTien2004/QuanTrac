package com.example.deviceservice.repository.specification;

import com.example.deviceservice.dto.request.Gateway.GatewayFilterRequest;
import com.example.deviceservice.entity.Gateway;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import java.util.ArrayList;
import java.util.List;

public class GatewaySpecification {

    public static Specification<Gateway> filterWithRequest(GatewayFilterRequest request) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            // 1. Luôn luôn chỉ lấy các thiết bị CHƯA BỊ XÓA MỀM
            predicates.add(criteriaBuilder.equal(root.get("isDeleted"), false));

            // 2. Lọc theo stationId (Khóa ngoại)
            if (request.getStationId() != null && !request.getStationId().isBlank()) {
                predicates.add(criteriaBuilder.equal(root.get("station").get("id"), request.getStationId()));
            }

            // 3. Tìm kiếm gần đúng theo Code (LIKE %code%)
            if (request.getCode() != null && !request.getCode().isBlank()) {
                predicates.add(criteriaBuilder.like(root.get("code"), "%" + request.getCode().trim() + "%"));
            }

            // 4. Tìm kiếm gần đúng theo Serial Number
            if (request.getSerialNumber() != null && !request.getSerialNumber().isBlank()) {
                predicates.add(criteriaBuilder.like(root.get("serialNumber"), "%" + request.getSerialNumber().trim() + "%"));
            }

            // 5. Lọc chính xác theo Model
            if (request.getModel() != null && !request.getModel().isBlank()) {
                predicates.add(criteriaBuilder.equal(root.get("model"), request.getModel().trim()));
            }

            // 6. Lọc chính xác theo IP Address
            if (request.getIpAddress() != null && !request.getIpAddress().isBlank()) {
                predicates.add(criteriaBuilder.equal(root.get("ipAddress"), request.getIpAddress().trim()));
            }

            // 7. Lọc chính xác theo Trạng thái (ONLINE/OFFLINE)
            if (request.getStatus() != null && !request.getStatus().isBlank()) {
                predicates.add(criteriaBuilder.equal(root.get("status"), request.getStatus()));
            }

            // Sắp xếp mặc định: Thiết bị mới tạo lên đầu
            query.orderBy(criteriaBuilder.desc(root.get("createdDate")));

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }
}