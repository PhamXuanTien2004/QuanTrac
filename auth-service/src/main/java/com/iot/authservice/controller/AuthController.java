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
    public ResponseEntity<BaseResponse<RegisterResponseDTO>> register(@RequestBody @Valid RegisterRequestDTO request) {
        RegisterResponseDTO response = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(BaseResponse.success(response));
    }

    @PostMapping("/verify-otp")
    public ResponseEntity<BaseResponse<RegisterResponseDTO>> verifyOtp(@RequestBody @Valid com.iot.authservice.dto.request.VerifyOtpRequestDTO request) {
        RegisterResponseDTO response = authService.verifyOtp(request);
        return ResponseEntity.ok(BaseResponse.success(response));
    }

    @PutMapping("/users/{userId}")
    public ResponseEntity<BaseResponse<java.util.Map<String, Object>>> updateUser(
            @PathVariable("userId") String userId,
            @RequestBody @Valid com.iot.authservice.dto.request.UserUpdateDTO request) {
        java.util.Map<String, Object> response = authService.updateUser(userId, request);
        return ResponseEntity.ok(BaseResponse.success(response));
    }

    @DeleteMapping("/users/{userId}")
    public ResponseEntity<BaseResponse<Void>> deleteUser(@PathVariable("userId") String userId) {
        authService.deleteUser(userId);
        BaseResponse<Void> response = new BaseResponse<>();
        response.setMessage("Soft deleted User successfully");
        return ResponseEntity.ok(response);
    }

    @PostMapping("/login")
    public ResponseEntity<BaseResponse<TokenResponse>> login(
            @Valid @RequestBody LoginRequestDTO request) {

        TokenResponse tokenResponse = authService.login(request);

        return ResponseEntity.ok(new BaseResponse<>(tokenResponse, "Login successfully"));
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