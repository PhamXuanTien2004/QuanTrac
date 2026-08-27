package com.example.deviceservice.repository.specification;

import com.example.deviceservice.entity.BaseEntity;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Root;
import org.springframework.data.jpa.domain.Specification;

public class BaseSpecification {

    // 1. Bộ lọc mặc định: Loại bỏ các bản ghi đã bị xóa mềm (Áp dụng đa hình cho mọi thực thể con của BaseEntity)
    public static <T extends BaseEntity<?>> Specification<T> notDeleted() {
        return (root, query, cb) -> cb.isNull(root.get("deletedAt"));
    }

    // 2. Bộ lọc so khớp chính xác (Equal) cho bất kỳ trường dữ liệu nào của bất kỳ thực thể nào
    public static <T> Specification<T> equal(String field, Object value) {
        return (root, query, cb) -> {
            if (value == null || (value instanceof String && ((String) value).isBlank())) {
                return null;
            }
            return cb.equal(getFieldPath(root, field), value);
        };
    }

    // 3. Bộ lọc tìm kiếm gần đúng (Like) cho bất kỳ thuộc tính String nào (Tự chuyển về chữ thường để so sánh không phân biệt hoa thường)
    public static <T> Specification<T> like(String field, String value) {
        return (root, query, cb) -> {
            if (value == null || value.isBlank()) {
                return null;
            }
            return cb.like(cb.lower(getFieldPath(root, field).as(String.class)), "%" + value.trim().toLowerCase() + "%");
        };
    }

    // Hàm bổ trợ (Helper) dùng để phân tích đường dẫn thuộc tính lồng nhau (VD: "station.id" hoặc "gateway.station.name")
    private static <T> Path<?> getFieldPath(Root<T> root, String field) {
        if (!field.contains(".")) {
            return root.get(field);
        }
        String[] parts = field.split("\\.");
        Path<?> path = root.get(parts[0]);
        for (int i = 1; i < parts.length; i++) {
            path = path.get(parts[i]);
        }
        return path;
    }
}