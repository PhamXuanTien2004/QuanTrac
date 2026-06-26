package com.iot.authservice.service;


import com.iot.authservice.dto.request.LoginRequestDTO;
import com.iot.authservice.dto.request.RegisterRequestDTO;
import com.iot.authservice.dto.request.TokenResponse;
import com.iot.authservice.dto.request.UserUpdateRequest;
import com.iot.authservice.entity.User;

import java.util.Map;

public interface AuthService {
    User register(RegisterRequestDTO request);
    TokenResponse login(LoginRequestDTO request);
    TokenResponse refreshToken(String refreshToken);
    void logout(String refreshToken);
    Map<String, Object> verifyToken(String accessToken);

    // Quản lý người dùng (CRUD)
    User updateUser(String userId, UserUpdateRequest request);
    User getUserById(String userId);
    void deleteUser(String userId);

}