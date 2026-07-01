package com.iot.authservice.controller;

import com.iot.authservice.common.BaseResponse;
import com.iot.authservice.dto.request.LoginRequestDTO;
import com.iot.authservice.dto.request.RegisterRequestDTO;
import com.iot.authservice.dto.response.RegisterResponseDTO;
import com.iot.authservice.dto.response.TokenResponse;
import com.iot.authservice.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    // ==========================================
    // 1. REGISTER & LOGIN
    // ==========================================

    @PostMapping("/register")
    public ResponseEntity<BaseResponse<String>> register(
            @Valid @RequestBody RegisterRequestDTO dto) {

        authService.register(dto);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new BaseResponse<>(null, "Created"));
    }
    @PostMapping("/login")
    public ResponseEntity<TokenResponse> login(@Valid @RequestBody LoginRequestDTO request) {
        // 1. Lấy kết quả từ Service
        TokenResponse tokenResponse = authService.login(request);


        // 3. Trả về
        return ResponseEntity.ok(tokenResponse);
    }

    // ==========================================
    // 2. TOKEN MANAGEMENT
    // ==========================================

    @PostMapping("/refresh-token")
    public ResponseEntity<TokenResponse> refreshToken(@RequestParam("refreshToken") String refreshToken) {
        TokenResponse response = authService.refreshToken(refreshToken);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/logout")
    public ResponseEntity<String> logout(@RequestParam("refreshToken") String refreshToken) {
        authService.logout(refreshToken);
        return ResponseEntity.ok("Logged out successfully");
    }

    @PostMapping("/verify-token")
    public ResponseEntity<Map<String, Object>> verifyToken(@RequestParam("accessToken") String accessToken) {
        Map<String, Object> introspectionResult = authService.verifyToken(accessToken);
        return ResponseEntity.ok(introspectionResult);
    }

    @GetMapping("/register/status/{userId}")
    public ResponseEntity<RegisterResponseDTO> checkRegisterStatus(@PathVariable String userId) {
        RegisterResponseDTO response = authService.changeStatus(userId);
        return ResponseEntity.ok(response);
    }
}