package com.example.deviceservice.common;

import org.springframework.data.jpa.domain.Specification;
import jakarta.persistence.criteria.Predicate;
import org.springframework.util.ObjectUtils;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

public class GenericSpecification {

    public static <T> Specification<T> searchByDto(Object searchDto) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (searchDto == null) {
                return cb.conjunction();
            }

            // Quét tất cả các thuộc tính của DTO (bao gồm cả class con và class cha)
            Class<?> currentClass = searchDto.getClass();
            while (currentClass != null && currentClass != Object.class) {
                Field[] fields = currentClass.getDeclaredFields();

                for (Field field : fields) {
                    try {
                        field.setAccessible(true);
                        Object value = field.get(searchDto);

                        // Nếu trường không truyền lên (null hoặc rỗng) -> Bỏ qua không filter
                        if (ObjectUtils.isEmpty(value)) {
                            continue;
                        }

                        String fieldName = field.getName();

                        // Bỏ qua các trường cấu hình phân trang hệ thống của BaseSearchRequest
                        if (fieldName.equals("page") || fieldName.equals("size")
                                || fieldName.equals("sortBy") || fieldName.equals("sortDir")) {
                            continue;
                        }

                        // Cấu hình đặc biệt: Nếu truyền "keyword" -> Tìm kiếm theo Code HOẶC Tên
                        if (fieldName.equals("keyword")) {
                            String lowercaseValue = "%" + value.toString().toLowerCase() + "%";
                            Predicate searchInCode = cb.like(cb.lower(root.get("code")), lowercaseValue);
                            Predicate searchInName = cb.like(cb.lower(root.get("name")), lowercaseValue);
                            predicates.add(cb.or(searchInCode, searchInName));
                            continue;
                        }

                        // Xử lý mặc định dựa trên kiểu dữ liệu của trường trong DTO
                        if (field.getType().equals(String.class)) {
                            // Kiểu String -> Tự động dùng LIKE %keyword%
                            String lowercaseValue = "%" + value.toString().toLowerCase() + "%";
                            predicates.add(cb.like(cb.lower(root.get(fieldName)), lowercaseValue));
                        } else {
                            // Các kiểu khác (Long, Double, Boolean...) -> Tự động dùng dấu bằng (=)
                            predicates.add(cb.equal(root.get(fieldName), value));
                        }

                    } catch (IllegalAccessException e) {
                        // Thêm log nếu cần thiết để theo dõi lỗi Reflection
                    }
                }
                currentClass = currentClass.getSuperclass(); // Tiếp tục quét class cha (BaseSearchRequest)
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}