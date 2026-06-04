package com.example.deviceservice.common;

import org.springframework.data.jpa.domain.Specification;
import jakarta.persistence.criteria.Predicate;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

public class GenericSpecification {
    public static <T, D> Specification<T> searchByDto(D dto) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            Field[] fields = dto.getClass().getDeclaredFields();

            for (Field field : fields) {
                try {
                    field.setAccessible(true);
                    Object value = field.get(dto);

                    // Nếu trường có giá trị, tự động build Predicate tương ứng
                    if (value != null && !value.toString().trim().isEmpty()) {
                        if (field.getType() == String.class) {
                            predicates.add(cb.like(cb.lower(root.get(field.getName())), "%" + value.toString().toLowerCase() + "%"));
                        } else {
                            predicates.add(cb.equal(root.get(field.getName()), value));
                        }
                    }
                } catch (IllegalAccessException e) {
                    // Xử lý ngoại lệ log nếu cần
                }
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}