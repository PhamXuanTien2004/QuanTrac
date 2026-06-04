package com.example.deviceservice.entity;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener; // 🌟 Cực kỳ quan trọng

import java.time.Instant;

@MappedSuperclass
@Getter
@Setter
@EntityListeners(AuditingEntityListener.class) // 🌟 2. BẮT BUỘC: Lắng nghe sự kiện để tự chèn dữ liệu
public abstract class BaseEntity<S> {

    @Column(name = "is_deleted", nullable = false)
    private Boolean isDeleted = false; // 🌟 3. Gán mặc định false ngay tại Entity, khỏi lo bị null!

    @CreatedBy
    @Column(name = "created_by", updatable = false) // updatable = false để khi update không bị mất tên người tạo gốc
    private String createdBy;

    @CreatedDate
    @Column(name = "created_date", updatable = false)
    private Instant createdDate;

    @LastModifiedBy
    @Column(name = "last_modified_by")
    private String lastModifiedBy;

    @LastModifiedDate
    @Column(name = "last_modified_date")
    private Instant lastModifiedDate;
}