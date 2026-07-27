package com.iot.userservice.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@Setter
@Table(name = "users")
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User extends BaseEntity {
    @Id
    @Column(name = "id", length = 36, nullable = false)
    private String id;

    @Column(name = "username", length = 100, nullable = false, unique = true)
    private String username;

    @Column(name = "email", length = 100)
    private String email;

    @Column(name = "full_name", length = 255)
    private String fullName;

    @Column(name = "phone", length = 20)
    private String phone;

    @Column(name = "station_id", length = 36)
    private String stationId;

    @Column(name = "role", length = 50)
    private String role;

    @Enumerated(EnumType.STRING)
    @Column(name = "notification_method", length = 20)
    private NotificationMethod notificationMethod = NotificationMethod.NONE;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20, nullable = false)
    private UserStatus status = UserStatus.PENDING;

    public enum NotificationMethod {
        EMAIL, SMS, ALL, NONE
    }

    public enum UserStatus {
        PENDING, ACTIVE, FAILED
    }

}
