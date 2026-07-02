package com.iot.userservice.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.iot.userservice.event.dto.UserCreatedEvent;
import com.iot.userservice.repository.UserRepository;
import com.iot.userservice.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.DltHandler;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.RetryableTopic;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.retry.annotation.Backoff;
import org.springframework.stereotype.Component;

import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class AuthRegisterConsumer {

    private final UserService userService;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private static final String CONFIRM_TOPIC = "user-confirm-topic";

    @KafkaListener(topics = "user-events", groupId = "user-service-group")
    @RetryableTopic(
            attempts = "4",
            backoff = @Backoff(delay = 2000, multiplier = 2.0)
    )
    public void consumeUserCreated(String message) {
//        log.info("[TEST-BUG] Ép hệ thống ném lỗi để kiểm tra luồng Saga Rollback");
//        throw new RuntimeException("Cố tình gây lỗi kết nối Database MySQL!");
        try {
            log.info("[KAFKA-CONSUMER] === NHẬN SỰ KIỆN TỪ KAFKA ===");
            log.info("[KAFKA-CONSUMER] Message thô nhận được: {}", message);
            UserCreatedEvent userCreatedEvent = objectMapper.readValue(message, UserCreatedEvent.class);

            if (userRepository.existsById(userCreatedEvent.getUserId())) {
                log.warn("[KAFKA-CONSUMER] Người dùng có ID '{}' đã tồn tại. Bỏ qua tránh trùng lặp!", userCreatedEvent.getUserId());
                return;
            }

            if ("CREATE".equals(userCreatedEvent.getEventType())) {
                // 1. Lưu CSDL
                userService.create(userCreatedEvent);
                log.info("[KAFKA-CONSUMER] Đồng bộ thành công người dùng '{}' vào CSDL.", userCreatedEvent.getUsername());

                Map<String, Object> successEvent = Map.of(
                        "keycloakId", userCreatedEvent.getUserId(),
                        "status", "ACCEPTED"
                );
                kafkaTemplate.send(CONFIRM_TOPIC, userCreatedEvent.getUserId(), successEvent);

                log.info("[KAFKA-PRODUCER] === ĐÃ BẮN MESSAGE CONFIRM (SUCCESS) CHO AUTH ===");
            }
        } catch (Exception e) {
            log.error("[KAFKA-CONSUMER-ERROR] Lưu Database thất bại! Kích hoạt tự động thử lại...", e);
            throw new RuntimeException("Database error", e);
        }
    }

    @DltHandler
    public void handleDlt(String message) {
        log.error("[KAFKA-DLT] !!! HẾT LẦN RETRY VẪN LỖI DB !!!");
        try {
            log.info("[KAFKA-DLT] Message thô nhận được: {}", message);

            // 1. Vòng lặp giải mã (Unescape) chuỗi JSON nếu bị bọc nhiều lần
            com.fasterxml.jackson.databind.JsonNode node = objectMapper.readTree(message);
            while (node.isTextual()) {
                String unescapedString = node.asText();
                node = objectMapper.readTree(unescapedString);
            }

            // 2. Kiểm tra và lấy thông tin userId một cách an toàn
            if (node.has("userId")) {
                String userId = node.get("userId").asText();

                Map<String, Object> rejectEvent = Map.of(
                        "keycloakId", userId,
                        "status", "REJECTED"
                );

                // 3. Bắn tin nhắn Rollback lên kafka
                kafkaTemplate.send(CONFIRM_TOPIC, userId, rejectEvent);
                log.info("[KAFKA-PRODUCER] === ĐÃ BẮN MESSAGE CONFIRM (REJECTED) CHO AUTH ĐỂ SAGA ROLLBACK ===");
            } else {
                log.error("[KAFKA-DLT-FATAL] Không tìm thấy trường 'userId' trong JSON payload sau khi parse! JSON: {}", node.toString());
            }

        } catch (Exception ex) {
            log.error("[KAFKA-DLT-FATAL] Không thể gửi tin nhắn rollback!", ex);
        }
    }
}