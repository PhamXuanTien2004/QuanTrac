package com.iot.authservice.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.iot.authservice.dto.event.UserConfirmEvent;
import com.iot.authservice.entity.UserRegistrationState;
import com.iot.authservice.repository.UserRegistrationStateRepository;
import com.iot.authservice.service.AuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class UserConfirmConsumer {

    private final Keycloak keycloak;
    private final ObjectMapper objectMapper;
    private final AuthService authService;
    private final UserRegistrationStateRepository userRegistrationStateRepository;

    @Value("${keycloak.realm}")
    private String realm;

    @KafkaListener(topics = "user-confirm-topic", groupId = "auth-confirm-group")
    public void consumeConfirm(String message) {
        try {
            log.info("[SAGA-CONFIRM] === NHẬN YÊU CẦU ĐỒNG BỘ SAGA === ");
            log.info(" - Bản tin JSON nhận về: {}", message);

            UserConfirmEvent event = objectMapper.readValue(message, UserConfirmEvent.class);
            String userId = event.getKeycloakId();

            // 1. KỊCH BẢN THẤT BẠI: Xóa tài khoản lỗi
            if (event.getStatus() == UserConfirmEvent.Status.REJECTED) {
                keycloak.realm(realm).users().get(userId).remove();
                log.info("[SAGA-ROLLBACK-SUCCESS] Đã xóa tài khoản lỗi khỏi Keycloak. ID: {}", userId);
                
                userRegistrationStateRepository.findById(userId).ifPresent(state -> {
                    state.setStatus(UserRegistrationState.RegistrationStatus.FAILED);
                    userRegistrationStateRepository.save(state);
                });
            }

            // 2. KỊCH BẢN THÀNH CÔNG: Đã lưu Database thành công
            if (event.getStatus() == UserConfirmEvent.Status.ACCEPTED) {
                userRegistrationStateRepository.findById(userId).ifPresent(state -> {
                    state.setDbSynced(true);
                    
                    if (state.isOtpVerified()) {
                        state.setStatus(UserRegistrationState.RegistrationStatus.COMPLETED);
                        
                        // Kích hoạt tài khoản trên Keycloak
                        UserRepresentation userRep = keycloak.realm(realm).users().get(userId).toRepresentation();
                        userRep.setEnabled(true);
                        userRep.setEmailVerified(true);
                        keycloak.realm(realm).users().get(userId).update(userRep);
                        
                        log.info("[SAGA-COMMIT-SUCCESS] Đồng bộ database thành công & OTP đã xác thực. Đã kích hoạt trạng thái ACTIVE cho User ID: {}", userId);
                    } else {
                        log.info("[SAGA-COMMIT-PARTIAL] Đồng bộ database thành công cho User ID: {}. Đang chờ người dùng nhập OTP.", userId);
                    }
                    
                    userRegistrationStateRepository.save(state);
                });
            }

        } catch (Exception e) {
            log.error("[SAGA-CONFIRM-FATAL-ERROR] Lỗi xử lý confirm trạng thái trên Keycloak!", e);
        }
    }
}
