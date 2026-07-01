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

    @Override
    public RegisterResponseDTO register(RegisterRequestDTO dto) {
        // 1. Chuẩn bị dữ liệu UserRepresentation
        UserRepresentation user = new UserRepresentation();
        user.setEnabled(false);
        user.setEmail(dto.getEmail());
        user.setFirstName(dto.getFirstName());
        user.setLastName(dto.getLastName());
        user.setUsername(dto.getUsername());

        CredentialRepresentation credential = new CredentialRepresentation();
        credential.setTemporary(false);
        credential.setType(CredentialRepresentation.PASSWORD);
        credential.setValue(dto.getPassword());
        user.setCredentials(Collections.singletonList(credential));

        // 2. Validate Station ID chéo sang Device Service
        StationResponse stationResponse = stationClient.getByStationId(dto.getStationId());
        if (stationResponse == null) {
            log.error("station id '{}' not found", dto.getStationId());
            throw new RuntimeException("Station ID không tồn tại trên hệ thống!");
        }

        // 3. Gọi Keycloak tạo User MỚI (Chưa gán role ở đây)
        UsersResource usersResource = keycloak.realm(realm).users();
        Response response = usersResource.create(user);

        log.info("Create user status = {}", response.getStatus());

        if (response.getStatus() != 201) {
            log.error("Body = {}", response.readEntity(String.class));
            throw new RuntimeException("Đăng ký tài khoản trên Keycloak thất bại!");
        }

        // Lấy userId do Keycloak vừa sinh ra
        String userId = response.getLocation().getPath().replaceAll(".*/([^/]+)$", "$1");

        try {
            List<String> requestedRoles = dto.getRoles();

            if (requestedRoles == null || requestedRoles.isEmpty()) {
                log.info("Không có role nào được truyền lên, gán role mặc định: Staff");
                assignClientRole(userId, "Staff");
            } else {
                for (String roleName : requestedRoles) {
                    try {
                        assignClientRole(userId, roleName);
                    } catch (Exception e) {
                        log.warn("Không thể gán role '{}' cho user {}: {}", roleName, userId, e.getMessage());
                        throw new RoleNotFoundException("Role not found");
                    }
                }
            }

            // Đóng gói thông tin sự kiện
            UserEvent event = UserEvent.builder()
                    .occurredAt(Instant.now())
                    .eventType("CREATE")
                    .userId(userId)
                    .username(dto.getUsername())
                    .fullName(dto.getLastName() + dto.getFirstName())
                    .phone(dto.getPhone())
                    .stationId(dto.getStationId())
                    .build();

            // trước khi báo thành công cho người dùng. Nếu Kafka lỗi, nhảy vào khối catch thực hiện Rollback xóa Keycloak.
            kafkaTemplate.send(USER_TOPIC, userId, event).get(3, java.util.concurrent.TimeUnit.SECONDS);
            log.info("[KAFKA-PUBLISHED] Đã gửi sự kiện tạo User lên Kafka thành công cho userId: {}", userId);

            return RegisterResponseDTO.builder()
                    .userId(userId) // Sử dụng biến userId thực tế vừa lấy từ Keycloak ở trên
                    .username(dto.getUsername())
                    .status(RegisterResponseDTO.Status.PENDING)
                    .message("Tài khoản đã được khởi tạo. Đang tiến hành đồng bộ dữ liệu hồ sơ...")
                    .build();

        } catch (Exception e) {
            // Rollback: Nếu lưu DB lỗi, Kafka lỗi, hoặc gán Role lỗi -> Xóa user trên Keycloak
            log.error("Failed to process local DB, Kafka, or Roles. Rolling back Keycloak user: {}", userId, e);
            try {
                usersResource.get(userId).remove(); // Bù đắp xóa tài khoản lỗi
            } catch (Exception ex) {
                log.error("Lỗi nghiêm trọng! Không thể thực hiện rollback xóa Keycloak cho userId: {}", userId, ex);
            }
            throw new RuntimeException("Registration failed, rolled back successfully.");
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

        RoleRepresentation role =
                keycloak.realm(realm)
                        .clients()
                        .get(clientUuid)
                        .roles()
                        .get(roleName)
                        .toRepresentation();

        log.info("role = {}", role.getName());

        keycloak.realm(realm)
                .users()
                .get(userId)
                .roles()
                .clientLevel(clientUuid)
                .add(List.of(role));

        log.info("Assigned role {} to {}", roleName, userId);
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
}