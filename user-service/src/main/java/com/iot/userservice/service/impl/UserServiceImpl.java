package com.iot.userservice.service.impl;

import com.iot.userservice.entity.User;
import com.iot.userservice.event.dto.UserCreatedEvent;
import com.iot.userservice.repository.UserRepository;
import com.iot.userservice.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {
    private final UserRepository userRepository;

    @Override
    public User create(UserCreatedEvent event) {
        User userEntity = User.builder()
                .id(event.getUserId())
                .username(event.getUsername())
                .email(event.getEmail())
                .fullName(event.getFullName())
                .phone(event.getPhone())
                .stationId(event.getStationId())
                .role(event.getRole())
                .status(User.UserStatus.ACTIVE)
                .notificationMethod(event.getNotificationMethod() != null ? User.NotificationMethod.valueOf(event.getNotificationMethod().toUpperCase()) : User.NotificationMethod.ALL)
                .build();
        return userRepository.save(userEntity);
    }

    @Override
    public User update(UserCreatedEvent event) {
        User userEntity = userRepository.findById(event.getUserId())
                .orElseThrow(() -> new RuntimeException("Người dùng không tồn tại"));
        
        userEntity.setEmail(event.getEmail());
        userEntity.setFullName(event.getFullName());
        userEntity.setPhone(event.getPhone());
        userEntity.setStationId(event.getStationId());
        userEntity.setRole(event.getRole());
        userEntity.setNotificationMethod(event.getNotificationMethod() != null ? User.NotificationMethod.valueOf(event.getNotificationMethod().toUpperCase()) : User.NotificationMethod.ALL);
        
        return userRepository.save(userEntity);
    }

    @Override
    public java.util.List<User> findAll() {
        return userRepository.findByDeletedAtIsNull();
    }

    @Override
    public java.util.List<User> findByStationId(String stationId) {
        return userRepository.findByStationIdAndDeletedAtIsNull(stationId);
    }

    @Override
    public void delete(String id) {
        log.info("Soft deleting user with id: {}", id);
        userRepository.softDelete(id);
    }
}
