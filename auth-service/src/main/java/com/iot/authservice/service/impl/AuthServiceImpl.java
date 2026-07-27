package com.iot.authservice.service.impl;

import com.iot.authservice.clients.StationClient;
import com.iot.authservice.clients.dto.response.StationResponse;
import com.iot.authservice.dto.event.UserConfirmEvent;
import com.iot.authservice.dto.event.UserEvent;
import com.iot.authservice.dto.request.*;
import com.iot.authservice.dto.response.RegisterResponseDTO;
import com.iot.authservice.dto.response.TokenResponse;
import com.iot.authservice.service.AuthService;
import jakarta.ws.rs.core.Response;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.UsersResource;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import javax.management.relation.RoleNotFoundException;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {
    private final Keycloak keycloak;
    private final StationClient stationClient;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final org.springframework.data.redis.core.StringRedisTemplate redisTemplate;
    private final com.fasterxml.jackson.databind.ObjectMapper objectMapper;

    @Value("${keycloak.auth-service-url}")
    private String serverUrl;

    @Value("${keycloak.resource}")
    private String clientId;

    @Value("${keycloak.realm}")
    private String realm;

    @Value("${keycloak.credentials.secret}")
    private String clientSecret;

    private final RestTemplate restTemplate = new RestTemplate();

    // Tên topic dùng chung cho các sự kiện của User
    private static final String USER_TOPIC = "user-events";
    private static final String OTP_TOPIC = "otp-normalized";

    @Override
    public RegisterResponseDTO register(RegisterRequestDTO dto) {
        // 1. Kiểm tra vai trò để kiểm soát yêu cầu trạm (Station Name / ID)
        List<String> requestedRoles = dto.getRoles();
        String primaryRole = (requestedRoles != null && !requestedRoles.isEmpty()) ? requestedRoles.get(0) : "Staff";
        boolean isAdminRole = primaryRole.equalsIgnoreCase("Admin") || primaryRole.equalsIgnoreCase("ROLE_ADMIN");

        String targetStationId = dto.getStationId();
        String inputStationName = dto.getStationName();

        // Manager và Staff BẮT BUỘC phải chọn Tên trạm hoặc Mã trạm
        if (!isAdminRole) {
            if ((inputStationName == null || inputStationName.trim().isEmpty()) && 
                (targetStationId == null || targetStationId.trim().isEmpty())) {
                throw new RuntimeException("Tài khoản Manager và Staff bắt buộc phải cung cấp Tên trạm quan trắc (stationName)!");
            }
        }

        // 2. Tìm kiếm và Kiểm tra sự tồn tại của Trạm qua Tên Trạm (hoặc ID)
        if (inputStationName != null && !inputStationName.trim().isEmpty()) {
            StationResponse stationResponse = stationClient.getByStationName(inputStationName.trim());
            if (stationResponse == null) {
                log.error("Trạm quan trắc với tên '{}' không tồn tại!", inputStationName);
                throw new RuntimeException("Trạm quan trắc với tên '" + inputStationName + "' không tồn tại trên hệ thống! Vui lòng kiểm tra lại.");
            }
            targetStationId = stationResponse.getEffectiveId();
            log.info("Tìm thấy trạm '{}' -> ID = {}", inputStationName, targetStationId);
        } else if (targetStationId != null && !targetStationId.trim().isEmpty()) {
            StationResponse stationResponse = stationClient.getByStationId(targetStationId.trim());
            if (stationResponse == null && !isAdminRole) {
                log.error("Trạm quan trắc với ID '{}' không tồn tại!", targetStationId);
                throw new RuntimeException("Trạm quan trắc với ID '" + targetStationId + "' không tồn tại trên hệ thống!");
            }
        }
        
        dto.setStationId(targetStationId); // Cập nhật ID đúng

        // 3. Sinh mã OTP và lưu vào Redis
        String otp = String.format("%06d", new java.util.Random().nextInt(999999));
        String redisKey = "register:otp:" + dto.getEmail();
        
        try {
            String dtoJson = objectMapper.writeValueAsString(dto);
            redisTemplate.opsForValue().set(redisKey, otp + "|||" + dtoJson, 5, java.util.concurrent.TimeUnit.MINUTES);
            
            // 4. Gửi OTP qua Kafka tới notification-service
            com.iot.authservice.dto.event.OtpEvent otpEvent = com.iot.authservice.dto.event.OtpEvent.builder()
                .email(dto.getEmail())
                .phone(dto.getPhone())
                .otp(otp)
                .fullName(dto.getLastName() + " " + dto.getFirstName())
                .build();
                
            kafkaTemplate.send(OTP_TOPIC, dto.getEmail(), otpEvent);
            log.info("Đã gửi OTP cho {}: {}", dto.getEmail(), otp);
            
        } catch (Exception e) {
            log.error("Lỗi khi xử lý OTP cho email: {}", dto.getEmail(), e);
            throw new RuntimeException("Không thể xử lý yêu cầu đăng ký lúc này. Vui lòng thử lại sau.");
        }

        return RegisterResponseDTO.builder()
                .username(dto.getUsername())
                .status(RegisterResponseDTO.Status.PENDING)
                .message("Mã xác nhận (OTP) đã được gửi đến Email của bạn. Vui lòng kiểm tra và nhập mã để hoàn tất đăng ký.")
                .build();
    }

    @Override
    public RegisterResponseDTO verifyOtp(com.iot.authservice.dto.request.VerifyOtpRequestDTO request) {
        String redisKey = "register:otp:" + request.getEmail();
        String storedData = redisTemplate.opsForValue().get(redisKey);
        
        if (storedData == null) {
            throw new RuntimeException("Mã OTP đã hết hạn hoặc không tồn tại.");
        }
        
        String[] parts = storedData.split("\\|\\|\\|");
        if (parts.length != 2) {
            throw new RuntimeException("Dữ liệu không hợp lệ.");
        }
        
        String storedOtp = parts[0];
        String dtoJson = parts[1];
        
        if (!storedOtp.equals(request.getOtp())) {
            throw new RuntimeException("Mã OTP không chính xác.");
        }
        
        // OTP đúng, thực hiện tạo Keycloak User
        try {
            RegisterRequestDTO dto = objectMapper.readValue(dtoJson, RegisterRequestDTO.class);
            
            UserRepresentation user = new UserRepresentation();
            user.setEnabled(true);
            user.setEmail(dto.getEmail());
            user.setFirstName(dto.getFirstName());
            user.setLastName(dto.getLastName());
            user.setUsername(dto.getUsername());

            CredentialRepresentation credential = new CredentialRepresentation();
            credential.setTemporary(false);
            credential.setType(CredentialRepresentation.PASSWORD);
            credential.setValue(dto.getPassword());
            user.setCredentials(Collections.singletonList(credential));

            if (dto.getStationId() != null && !dto.getStationId().trim().isEmpty()) {
                user.setAttributes(Map.of("station_id", List.of(dto.getStationId())));
            }

            UsersResource usersResource = keycloak.realm(realm).users();
            Response response = usersResource.create(user);

            if (response.getStatus() != 201) {
                String errorBody = response.readEntity(String.class);
                if (errorBody != null && errorBody.contains("same email")) {
                    throw new RuntimeException("Email đã được sử dụng!");
                } else if (errorBody != null && errorBody.contains("same username")) {
                    throw new RuntimeException("Username đã tồn tại!");
                }
                throw new RuntimeException("Tạo tài khoản Keycloak thất bại.");
            }

            String userId = response.getLocation().getPath().replaceAll(".*/([^/]+)$", "$1");
            
            String primaryRole = (dto.getRoles() != null && !dto.getRoles().isEmpty()) ? dto.getRoles().get(0) : "Staff";
            assignClientRole(userId, primaryRole);

            // Gửi sự kiện cho user-service
            UserEvent event = UserEvent.builder()
                    .occurredAt(Instant.now())
                    .eventType("CREATE")
                    .userId(userId)
                    .username(dto.getUsername())
                    .email(dto.getEmail())
                    .fullName(dto.getLastName() + " " + dto.getFirstName())
                    .phone(dto.getPhone())
                    .stationId(dto.getStationId())
                    .role(primaryRole.startsWith("ROLE_") ? primaryRole : "ROLE_" + primaryRole.toUpperCase())
                    .notificationMethod(dto.getNotificationMethod() != null ? dto.getNotificationMethod() : "ALL")
                    .build();

            kafkaTemplate.send(USER_TOPIC, userId, event);
            
            // Xóa Redis key
            redisTemplate.delete(redisKey);

            return RegisterResponseDTO.builder()
                    .userId(userId)
                    .username(dto.getUsername())
                    .status(RegisterResponseDTO.Status.ACTIVE)
                    .message("Xác thực thành công. Tài khoản đã được tạo.")
                    .build();
                    
        } catch (Exception e) {
            log.error("Lỗi khi xác thực OTP và tạo tài khoản", e);
            throw new RuntimeException("Lỗi: " + e.getMessage());
        }
    }


    private void assignClientRole(String userId, String roleName) {

        log.info("realm = {}", realm);
        log.info("clientId = {}", clientId);

        List<ClientRepresentation> clients =
                keycloak.realm(realm)
                        .clients()
                        .findAll();

        log.info("===== ALL CLIENTS =====");
        clients.forEach(c ->
                log.info("id={}, clientId={}, name={}",
                        c.getId(),
                        c.getClientId(),
                        c.getName()));

        List<ClientRepresentation> result =
                keycloak.realm(realm)
                        .clients()
                        .findByClientId(clientId);

        log.info("findByClientId({}) => {}", clientId, result.size());

        // Nếu không tìm thấy thì báo lỗi rõ ràng
        if (result.isEmpty()) {
            throw new RuntimeException("Client '" + clientId + "' not found");
        }

        String clientUuid = result.get(0).getId();
        log.info("clientUuid = {}", clientUuid);

        List<RoleRepresentation> clientRoles = keycloak.realm(realm)
                .clients()
                .get(clientUuid)
                .roles()
                .list();

        RoleRepresentation role = clientRoles.stream()
                .filter(r -> r.getName().equalsIgnoreCase(roleName)
                        || r.getName().equalsIgnoreCase("ROLE_" + roleName)
                        || r.getName().replace("ROLE_", "").equalsIgnoreCase(roleName))
                .findFirst()
                .orElse(null);

        if (role == null) {
            log.error("Role '{}' không tìm thấy trong Client '{}'", roleName, clientId);
            throw new RuntimeException("Role '" + roleName + "' không tồn tại trên Keycloak!");
        }

        log.info("Found matching Keycloak role: {}", role.getName());

        keycloak.realm(realm)
                .users()
                .get(userId)
                .roles()
                .clientLevel(clientUuid)
                .add(List.of(role));

        log.info("Assigned role {} to {}", role.getName(), userId);
    }

    @Override
    public TokenResponse login(LoginRequestDTO dto) {
        String tokenUrl = serverUrl + "/realms/" + realm + "/protocol/openid-connect/token";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> map = new LinkedMultiValueMap<>();
        map.add("grant_type", "password");
        map.add("client_id", clientId);
        map.add("client_secret", clientSecret);
        map.add("username", dto.getUsername());
        map.add("password", dto.getPassword());

        try {
            HttpEntity<MultiValueMap<String, String>> entity = new HttpEntity<>(map, headers);
            ResponseEntity<TokenResponse> response = restTemplate.postForEntity(tokenUrl, entity, TokenResponse.class);
            return response.getBody();
        } catch (Exception ex) {
            log.error("Login failed for user: {}", dto.getUsername(), ex);
            throw new RuntimeException("Username or password is incorrect");
        }
    }

    @Override
    public TokenResponse refreshToken(String refreshToken) {
        String tokenUrl = serverUrl + "/realms/" + realm + "/protocol/openid-connect/token";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> map = new LinkedMultiValueMap<>();
        map.add("grant_type", "refresh_token");
        map.add("client_id", clientId);
        map.add("client_secret", clientSecret);
        map.add("refresh_token", refreshToken);

        HttpEntity<MultiValueMap<String, String>> entity = new HttpEntity<>(map, headers);
        ResponseEntity<TokenResponse> response = restTemplate.postForEntity(tokenUrl, entity, TokenResponse.class);
        return response.getBody();
    }

    @Override
    public void logout(String refreshToken) {
        String logoutUrl = serverUrl + "/realms/" + realm + "/protocol/openid-connect/logout";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> map = new LinkedMultiValueMap<>();
        map.add("client_id", clientId);
        map.add("client_secret", clientSecret);
        map.add("refresh_token", refreshToken);

        try {
            HttpEntity<MultiValueMap<String, String>> entity = new HttpEntity<>(map, headers);
            restTemplate.postForEntity(logoutUrl, entity, String.class);
            log.info("Successfully logged out user session via refresh token.");
        } catch (Exception e) {
            log.error("Failed to logout session", e);
            throw new RuntimeException("Logout failed");
        }
    }

    @Override
    public Map<String, Object> verifyToken(String accessToken) {
        String introspectUrl = serverUrl + "/realms/" + realm + "/protocol/openid-connect/token/introspect";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> map = new LinkedMultiValueMap<>();
        map.add("client_id", clientId);
        map.add("client_secret", clientSecret);
        map.add("token", accessToken);

        try {
            HttpEntity<MultiValueMap<String, String>> entity = new HttpEntity<>(map, headers);
            ResponseEntity<Map> response = restTemplate.postForEntity(introspectUrl, entity, Map.class);
            return (Map<String, Object>) response.getBody();
        } catch (Exception e) {
            log.error("Token introspection failed", e);
            return Map.of("active", false);
        }
    }

    @Override
    public RegisterResponseDTO changeStatus(String id) {
        try {
            // Đọc trực tiếp dữ liệu cấu trúc thời gian thực từ Keycloak Server
            UserRepresentation userRep = keycloak.realm(realm).users().get(id).toRepresentation();

            // Nếu consumer đã nhận được ACCEPTED từ user-service và kích hoạt thành true
            if (userRep != null && userRep.isEnabled()) {
                return RegisterResponseDTO.builder()
                        .userId(id)
                        .username(userRep.getUsername())
                        .status(RegisterResponseDTO.Status.SUCCESS)
                        .message("Tài khoản hồ sơ đã đồng bộ hệ thống thành công!")
                        .build();
            }
        } catch (Exception e) {
            // Khối này chạy khi user bị Consumer xóa bỏ (Do user-service ném lỗi REJECTED)
            log.error("Không tìm thấy hoặc lỗi truy vấn User trên Keycloak với ID: {} (Có thể đã bị xóa Rollback)", id);
            return RegisterResponseDTO.builder()
                    .userId(id)
                    .status(RegisterResponseDTO.Status.FAILED)
                    .message("Đăng ký thất bại: Hệ thống hồ sơ trục trặc dữ liệu, tiến trình đã bị hoàn tác!")
                    .build();
        }

        // Nếu user vẫn tồn tại nhưng enabled = false -> Vẫn báo về cho Client là đang xử lý ngầm
        return RegisterResponseDTO.builder()
                .userId(id)
                .status(RegisterResponseDTO.Status.PENDING)
                .message("Hệ thống đang tiến hành cấu hình đồng bộ hồ sơ, vui lòng đợi...")
                .build();
    }

    @Override
    public Map<String, Object> updateUser(String userId, com.iot.authservice.dto.request.UserUpdateDTO dto) {
        try {
            UsersResource usersResource = keycloak.realm(realm).users();
            UserRepresentation user = usersResource.get(userId).toRepresentation();
            if (user == null) {
                throw new RuntimeException("Không tìm thấy người dùng!");
            }

            user.setEmail(dto.getEmail());
            user.setFirstName(dto.getFirstName());
            user.setLastName(dto.getLastName());

            if (dto.getStationId() != null && !dto.getStationId().trim().isEmpty()) {
                user.setAttributes(Map.of("station_id", List.of(dto.getStationId())));
            }

            // Update user in Keycloak
            usersResource.get(userId).update(user);

            // Remove all existing client roles
            String clientUuid = keycloak.realm(realm).clients().findByClientId(clientId).get(0).getId();
            List<RoleRepresentation> existingRoles = usersResource.get(userId).roles().clientLevel(clientUuid).listAll();
            usersResource.get(userId).roles().clientLevel(clientUuid).remove(existingRoles);

            // Add new role
            String primaryRole = (dto.getRoles() != null && !dto.getRoles().isEmpty()) ? dto.getRoles().get(0) : "Staff";
            assignClientRole(userId, primaryRole);

            // Bắn sự kiện UPDATE
            UserEvent event = UserEvent.builder()
                    .occurredAt(Instant.now())
                    .eventType("UPDATE")
                    .userId(userId)
                    .username(user.getUsername())
                    .email(dto.getEmail())
                    .fullName(dto.getLastName() + " " + dto.getFirstName())
                    .phone(dto.getPhone())
                    .stationId(dto.getStationId())
                    .role(primaryRole.startsWith("ROLE_") ? primaryRole : "ROLE_" + primaryRole.toUpperCase())
                    .notificationMethod(dto.getNotificationMethod() != null ? dto.getNotificationMethod() : "ALL")
                    .build();

            kafkaTemplate.send(USER_TOPIC, userId, event);

            return Map.of("success", true, "message", "Cập nhật thành công!");
        } catch (Exception e) {
            log.error("Lỗi khi cập nhật người dùng", e);
            throw new RuntimeException("Cập nhật người dùng thất bại: " + e.getMessage());
        }
    }
}