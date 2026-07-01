package com.iot.authservice.service;


import com.iot.authservice.dto.request.LoginRequestDTO;
import com.iot.authservice.dto.request.RegisterRequestDTO;
import com.iot.authservice.dto.response.RegisterResponseDTO;
import com.iot.authservice.dto.response.TokenResponse;

import java.util.Map;

public interface AuthService {
    RegisterResponseDTO register(RegisterRequestDTO dto);
    TokenResponse login(LoginRequestDTO request);
    TokenResponse refreshToken(String refreshToken);
    void logout(String refreshToken);
    Map<String, Object> verifyToken(String accessToken);
    RegisterResponseDTO changeStatus(String id);


}