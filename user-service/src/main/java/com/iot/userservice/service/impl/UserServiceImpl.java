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
                .fullName(event.getFullName())
                .phone(event.getPhone())
                .stationId(event.getStationId())
                .role(event.getRole())
                .status(User.UserStatus.ACTIVE)
                .build();
        return userRepository.save(userEntity);
    }

    @Override
    public java.util.List<User> findAll() {
        return userRepository.findAll();
    }

    @Override
    public java.util.List<User> findByStationId(String stationId) {
        return userRepository.findByStationId(stationId);
    }
}
