package com.iot.authservice.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@Table(name = "user_registration_state")
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserRegistrationState {
    
    @Id
    @Column(name = "id", length = 36, nullable = false)
    private String id; // Keycloak User ID

    @Column(name = "username", length = 100, nullable = false)
    private String username;

    @Column(name = "email", length = 100)
    private String email;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20, nullable = false)
    private RegistrationStatus status;

    @Column(name = "is_db_synced", nullable = false)
    private boolean isDbSynced = false;

    @Column(name = "is_otp_verified", nullable = false)
    private boolean isOtpVerified = false;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public enum RegistrationStatus {
        PENDING, COMPLETED, FAILED
    }
}
