package com.iot.authservice.config;

import com.iot.authservice.dto.event.UserEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.UsersResource;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import jakarta.ws.rs.core.Response;
import java.time.Instant;
import java.util.Collections;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class KeycloakAdminSeeder implements CommandLineRunner {

    private final Keycloak keycloak;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${keycloak.realm}")
    private String realm;

    @Value("${keycloak.resource}")
    private String clientId;

    private static final String USER_TOPIC = "user-events";

    @Override
    public void run(String... args) {
        log.info("[ADMIN-SEEDER] Đang kiểm tra tài khoản hệ thống trên Keycloak...");
        
        try {
            UsersResource usersResource = keycloak.realm(realm).users();
            List<UserRepresentation> existingAdmins = usersResource.search("admin", true);
            
            UserRepresentation user = null;
            String userId = null;

            if (existingAdmins != null && !existingAdmins.isEmpty()) {
                log.info("[ADMIN-SEEDER] Tài khoản 'admin' đã tồn tại. Đang tiến hành reset lại mật khẩu chuẩn...");
                user = existingAdmins.get(0);
                userId = user.getId();
                
                CredentialRepresentation credential = new CredentialRepresentation();
                credential.setTemporary(false);
                credential.setType(CredentialRepresentation.PASSWORD);
                credential.setValue("admin");
                
                usersResource.get(userId).resetPassword(credential);
                log.info("[ADMIN-SEEDER] Đã reset mật khẩu thành công!");

                // Xóa bỏ mọi Required Actions (nếu có) để tránh lỗi "Account is not fully set up"
                user.setRequiredActions(Collections.emptyList());
                user.setEmailVerified(true);
                usersResource.get(userId).update(user);
            } else {
                log.info("[ADMIN-SEEDER] Không tìm thấy tài khoản 'admin'. Đang tiến hành tạo mới tự động...");

                // 1. Khởi tạo tài khoản trên Keycloak
                user = new UserRepresentation();
                user.setEnabled(true);
                user.setUsername("admin");
                user.setEmail("admin@system.local");
                user.setFirstName("System");
                user.setLastName("Administrator");
                user.setEmailVerified(true);
                user.setRequiredActions(Collections.emptyList());

                CredentialRepresentation credential = new CredentialRepresentation();
                credential.setTemporary(false);
                credential.setType(CredentialRepresentation.PASSWORD);
                credential.setValue("admin");
                user.setCredentials(Collections.singletonList(credential));

                Response response = usersResource.create(user);
                if (response.getStatus() != 201) {
                    log.error("[ADMIN-SEEDER] Tạo tài khoản Keycloak thất bại! Mã lỗi: {}", response.getStatus());
                    return;
                }

                // Lấy ID do Keycloak vừa sinh ra
                userId = response.getLocation().getPath().replaceAll(".*/([^/]+)$", "$1");
                log.info("[ADMIN-SEEDER] Đã tạo thành công tài khoản 'admin' với UUID: {}", userId);
            }

            // 2. Gán Role Admin cho tài khoản
            try {
                List<ClientRepresentation> allClients = keycloak.realm(realm).clients().findAll();
                ClientRepresentation targetClient = allClients.stream()
                        .filter(c -> clientId.equals(c.getClientId()))
                        .findFirst()
                        .orElse(null);

                if (targetClient == null) {
                    log.error("[ADMIN-SEEDER] Không tìm thấy client {}. Các client hiện có trong realm '{}': {}", 
                        clientId, realm, allClients.stream().map(ClientRepresentation::getClientId).toList());
                    return;
                }
                String clientUuid = targetClient.getId();
                
                // Tự động kiểm tra và tạo các Role cơ bản nếu chưa tồn tại
                seedClientRoles(clientUuid);

                List<RoleRepresentation> clientRoles = keycloak.realm(realm).clients().get(clientUuid).roles().list();
                
                RoleRepresentation adminRole = clientRoles.stream()
                        .filter(r -> r.getName().equalsIgnoreCase("Admin") || r.getName().equalsIgnoreCase("ROLE_ADMIN"))
                        .findFirst()
                        .orElse(null);

                if (adminRole != null) {
                    keycloak.realm(realm).users().get(userId).roles().clientLevel(clientUuid).add(List.of(adminRole));
                    log.info("[ADMIN-SEEDER] Đã gán thành công Client Role [Admin] cho tài khoản 'admin'.");
                } else {
                    log.error("[ADMIN-SEEDER] Không tìm thấy role Admin trong client {}", clientId);
                }
            } catch (Exception e) {
                log.error("[ADMIN-SEEDER] Lỗi khi gán quyền Keycloak: {}", e.getMessage());
            }

            // 3. Bắn Kafka Event (CREATE) để user-service tự động tạo vào MySQL
            UserEvent event = UserEvent.builder()
                    .occurredAt(Instant.now())
                    .eventType("CREATE")
                    .userId(userId)
                    .username("admin")
                    .email("admin@system.local")
                    .fullName("System Administrator")
                    .phone("0999999999")
                    .role("ROLE_ADMIN")
                    .notificationMethod("ALL")
                    .build();

            kafkaTemplate.send(USER_TOPIC, userId, event);
            log.info("[ADMIN-SEEDER] Đã gửi thông báo khởi tạo (CREATE) sang user-service qua Kafka để đồng bộ MySQL!");

        } catch (Exception ex) {
            log.error("[ADMIN-SEEDER] Quá trình tạo dữ liệu mồi gặp sự cố: {}", ex.getMessage());
        }
    }

    private void seedClientRoles(String clientUuid) {
        List<String> targetRoles = List.of("ROLE_ADMIN", "ROLE_MANAGER", "ROLE_STAFF");
        List<RoleRepresentation> existingRoles = keycloak.realm(realm).clients().get(clientUuid).roles().list();
        List<String> existingRoleNames = existingRoles.stream().map(RoleRepresentation::getName).toList();

        for (String roleName : targetRoles) {
            if (!existingRoleNames.contains(roleName)) {
                log.info("[ADMIN-SEEDER] Client Role '{}' không tồn tại. Đang tạo mới...", roleName);
                RoleRepresentation role = new RoleRepresentation();
                role.setName(roleName);
                role.setDescription("Tự động tạo cho " + roleName);
                role.setClientRole(true);
                keycloak.realm(realm).clients().get(clientUuid).roles().create(role);
                log.info("[ADMIN-SEEDER] Đã tạo thành công Role '{}'.", roleName);
            }
        }
    }
}
