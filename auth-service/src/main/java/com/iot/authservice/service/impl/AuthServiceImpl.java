package com.iot.authservice.service.impl;

import com.iot.authservice.dto.request.*;
import com.iot.authservice.entity.User;
import com.iot.authservice.mapper.UserMapper;
import com.iot.authservice.repository.UserRepository;
import com.iot.authservice.service.AuthService;
import jakarta.ws.rs.NotAuthorizedException;
import jakarta.ws.rs.core.Response;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.UsersResource;
import org.keycloak.representations.AccessTokenResponse;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.util.Collections;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {
    private final UserRepository userRepository;
    private final Keycloak keycloak;
    private final UserMapper userMapper;

    @Value("${keycloak.auth-service-url}")
    private String serverUrl;

    @Value("${keycloak.resource}")
    private String clientId;

    @Value("${keycloak.realm}")
    private String realm;

    @Value("${keycloak.credentials.secret}")
    private String clientSecret;

    private final RestTemplate restTemplate = new RestTemplate();

    @Override
    @Transactional
    public User register(RegisterRequestDTO dto) {
        // 1. Khởi tạo thông tin User cho Keycloak
        UserRepresentation user = new UserRepresentation();
        user.setEnabled(true);
        user.setEmail(dto.getEmail());
        user.setFirstName(dto.getFirstName());
        user.setLastName(dto.getLastName());
        user.setUsername(dto.getUsername());

        // 2. Thiết lập Password cho Keycloak
        CredentialRepresentation credential = new CredentialRepresentation();
        credential.setTemporary(false);
        credential.setType(CredentialRepresentation.PASSWORD);
        credential.setValue(dto.getPassword());
        user.setCredentials(Collections.singletonList(credential));

        user.singleAttribute("phone",  dto.getPhone());
        user.singleAttribute("stationId", dto.getStationId());

        UsersResource usersResource = keycloak.realm(realm).users();
        Response response = usersResource.create(user);

        if (response.getStatus() == 201) {
            String userId = response.getLocation().getPath().replaceAll(".*/([^/]+)$", "$1");

            // 4. Lưu vào Database Local
            User newUser = userMapper.createUser(dto);
            newUser.setId(userId);
            newUser.setUsername(dto.getUsername());
            newUser.setEmail(dto.getEmail());
            newUser.setFullName(dto.getLastName() + " " + dto.getFirstName());

            User savedUser = userRepository.save(newUser);
            log.info("Successfully saved user to local database: {}", savedUser.getUsername());

            return savedUser;
        } else if (response.getStatus() == 409) {
            log.error("User already exists in Keycloak: {}", dto.getUsername());
            throw new RuntimeException("Username or Email already exists");
        } else {
            log.error("Error creating user, status: {}", response.getStatus());
            throw new RuntimeException("Failed to create user in Keycloak");
        }
    }

    private void assignClientRole(String userId, String roleName) {
        // Bước 1: Tìm UUID của client 'backend'
        String clientUuid = keycloak.realm(realm).clients()
                .findByClientId(clientId).get(0).getId();

        // Bước 2: Lấy thông tin role 'ADMIN' từ client đó
        var roleRepresentation = keycloak.realm(realm).clients()
                .get(clientUuid).roles().get(roleName).toRepresentation();

        // Bước 3: Gán role vào cấp độ Client cho User
        keycloak.realm(realm).users().get(userId).roles()
                .clientLevel(clientUuid).add(Collections.singletonList(roleRepresentation));
    }

    @Override
    public TokenResponse login(LoginRequestDTO dto) {

        try (Keycloak keycloak = Keycloak.getInstance(
                serverUrl,
                realm,
                dto.getUsername(),
                dto.getPassword(),
                clientId,
                clientSecret)) {

            AccessTokenResponse response =
                    keycloak.tokenManager().getAccessToken();

            return TokenResponse.builder()
                    .accessToken(response.getToken())
                    .refreshToken(response.getRefreshToken())
                    .expiresIn(response.getExpiresIn())
                    .refreshExpiresIn(response.getRefreshExpiresIn())
                    .tokenType(response.getTokenType())
                    .build();

        } catch (NotAuthorizedException ex) {
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
        map.add("refresh_token", refreshToken);

        HttpEntity<MultiValueMap<String, String>> entity = new HttpEntity<>(map, headers);
        ResponseEntity<TokenResponse> response = restTemplate.postForEntity(tokenUrl, entity, TokenResponse.class);
        return response.getBody();
    }

    @Override
    public void logout(String refreshToken) {

    }

    @Override
    public Map<String, Object> verifyToken(String accessToken) {
        return Map.of();
    }

    @Override
    public User updateUser(String userId, UserUpdateRequest request) {
        return null;
    }

    @Override
    public User getUserById(String userId) {
        return null;
    }

    @Override
    public void deleteUser(String userId) {

    }

}

