package com.iot.notification.service;

import com.iot.notification.client.UserClient;
import com.iot.notification.client.DeviceClient;
import com.iot.notification.dto.AlertEvent;
import com.iot.notification.dto.BaseResponse;
import com.iot.notification.dto.UserDto;
import com.iot.notification.dto.SensorDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.TimeUnit;

import com.iot.notification.repository.AlertHistoryRepository;
import com.iot.notification.entity.AlertHistory;
import java.time.Instant;
import java.util.Map;
import org.springframework.kafka.core.KafkaTemplate;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private final StringRedisTemplate redisTemplate;
    private final UserClient userClient;
    private final DeviceClient deviceClient;
    private final JavaMailSender mailSender;
    private final AlertHistoryRepository alertHistoryRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    private static final String RATE_LIMIT_PREFIX = "alert:rate_limit:";
    private static final long RATE_LIMIT_MINUTES = 5;

    public void processAlert(AlertEvent event) {
        if (event == null || event.getSensorId() == null) return;

        // Lấy thông tin chi tiết cảm biến để tính độ chênh lệch
        String sensorName = event.getSensorId(); // Default to ID
        Double difference = null;
        try {
            BaseResponse<SensorDto> sensorResp = deviceClient.getSensorById(event.getSensorId());
            if (sensorResp != null && sensorResp.getData() != null) {
                SensorDto sensorDto = sensorResp.getData();
                sensorName = sensorDto.getName();

            }
        } catch (Exception e) {
            log.warn("Failed to fetch sensor details for ID: {}", event.getSensorId(), e);
        }

        // 1. Lưu vào Database (Luôn lưu bất kể có bị giới hạn hay không)
        AlertHistory alertHistory = AlertHistory.builder()
                .stationId(event.getStationId())
                .sensorId(event.getSensorId())
                .sensorType(event.getSensorTypeCode())
                .value(event.getValue())
                .unit("") // Không có thông tin unit trong AlertEvent hiện tại
                .message("Cảnh báo: Cảm biến " + event.getSensorTypeCode() + " vượt ngưỡng (" + event.getValue() + ")")
                .timestamp(Instant.now())
                .difference(difference)
                .build();
        try {
            alertHistoryRepository.save(alertHistory);
        } catch (Exception e) {
            log.error("Failed to save alert history", e);
        }

        String redisKey = RATE_LIMIT_PREFIX + event.getSensorId();
        
        // 2. Check Rate Limit (5 minutes) cho việc gửi thông báo
        Boolean exists = redisTemplate.hasKey(redisKey);
        if (Boolean.TRUE.equals(exists)) {
            log.info("Rate limit hit for sensor {}. Skipping sending alert notification.", event.getSensorId());
            return;
        }

        log.info("Processing valid alert for sensor {}: value={} status={}", 
                 event.getSensorId(), event.getValue(), event.getStatus());

        // 2. Fetch Users for the Station
        try {
            BaseResponse<List<UserDto>> response = userClient.getUsersByStation(event.getStationId());
            if (response != null && response.getData() != null && !response.getData().isEmpty()) {
                List<UserDto> users = response.getData();
                
                // 3. Dispatch Notifications based on user preference
                for (UserDto user : users) {
                    String method = user.getNotificationMethod() != null ? user.getNotificationMethod() : "NONE";
                    
                    if ("EMAIL".equalsIgnoreCase(method) || "ALL".equalsIgnoreCase(method)) {
                        sendEmail(user, event, sensorName);
                    }
                    if ("SMS".equalsIgnoreCase(method) || "ALL".equalsIgnoreCase(method)) {
                        sendSms(user, event, sensorName);
                    }
                }
            }
        } catch (Exception e) {
            log.error("Failed to fetch users from user-service for station {}", event.getStationId(), e);
            return; // Don't set redis key if failed to send
        }

        // 4. Update Rate Limit Key in Redis
        redisTemplate.opsForValue().set(redisKey, "sent", RATE_LIMIT_MINUTES, TimeUnit.MINUTES);
    }

    private void sendEmail(UserDto user, AlertEvent event, String sensorName) {
        String email = user.getEmail();
        if (email == null || !email.contains("@")) return;

        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom("px.tien.2004@gmail.com"); // Đặt email người gửi (hiển thị trên Mailpit)
            message.setTo(email);
            message.setSubject("[QUANTRAC] CẢNH BÁO CẢM BIẾN " + event.getSensorTypeCode());
            message.setText(String.format("Kính gửi %s,\n\nCảm biến %s tại trạm của bạn đã ghi nhận giá trị bất thường: %.2f.\nTrạng thái: %s.\nThời gian: %s\n\nVui lòng kiểm tra hệ thống.", 
                    user.getFullName() != null ? user.getFullName() : user.getUsername(),
                    sensorName, event.getValue(), event.getStatus(), event.getTimestamp()));
            
            mailSender.send(message);
            log.info("Sent EMAIL alert to {}", email);
        } catch (Exception e) {
            log.error("Failed to send EMAIL to {}", email, e);
        }
    }

    private void sendSms(UserDto user, AlertEvent event, String sensorName) {
        String phone = user.getPhone();
        if (phone == null || phone.isEmpty()) return;

        try {
            // MOCK SMS SENDING (e.g. via Twilio or ESMS)
            String smsBody = String.format("[QUANTRAC] Canh bao! Cam bien %s co gia tri bat thuong: %.2f.", sensorName, event.getValue());
            log.info("MOCK - Sent SMS to {} with body: {}", phone, smsBody);
            // Twilio.init(ACCOUNT_SID, AUTH_TOKEN);
            // Message.creator(new PhoneNumber(phone), new PhoneNumber(FROM_NUMBER), smsBody).create();
        } catch (Exception e) {
            log.error("Failed to send SMS to {}", phone, e);
        }
    }

    public void sendOtpEmail(com.iot.notification.dto.OtpEvent event) {
        if (event == null || event.getEmail() == null || !event.getEmail().contains("@")) return;
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom("px.tien.2004@gmail.com"); // Đặt email người gửi (hiển thị trên Mailpit)
            message.setTo(event.getEmail());
            message.setSubject("[QUANTRAC] MÃ XÁC NHẬN ĐĂNG KÝ");
            message.setText(String.format("Kính gửi %s,\n\nMã xác nhận (OTP) của bạn là: %s\n\nMã này sẽ hết hạn trong 5 phút.\nVui lòng không chia sẻ mã này cho bất kỳ ai.", 
                    event.getFullName(), event.getOtp()));
            
            mailSender.send(message);
            log.info("Sent OTP EMAIL to {}", event.getEmail());
        } catch (Exception e) {
            log.error("Failed to send OTP EMAIL to {}", event.getEmail(), e);
        }
    }

    public void processAqiAlert(String stationId, Integer aqiValue, String aqiLevel, String pollutant) {
        if (stationId == null) return;

        // 1. Lưu vào Database
        AlertHistory alertHistory = AlertHistory.builder()
                .stationId(stationId)
                .sensorId("AQI") // Dummy ID for AQI
                .sensorType("SYSTEM")
                .value(Double.valueOf(aqiValue))
                .unit("AQI")
                .message("Cảnh báo AQI: Trạm " + stationId + " có chất lượng không khí ở mức " + aqiLevel + " (AQI: " + aqiValue + ", Chất ô nhiễm chính: " + pollutant + ")")
                .timestamp(Instant.now())
                .difference(null)
                .build();
        try {
            alertHistoryRepository.save(alertHistory);
        } catch (Exception e) {
            log.error("Failed to save AQI alert history", e);
        }

        String redisKey = RATE_LIMIT_PREFIX + "AQI_" + stationId;
        
        // 2. Check Rate Limit (e.g. 60 minutes for AQI to avoid spam)
        Boolean exists = redisTemplate.hasKey(redisKey);
        if (Boolean.TRUE.equals(exists)) {
            log.info("Rate limit hit for AQI alert at station {}. Skipping sending email.", stationId);
            return;
        }

        log.info("Processing AQI alert for station {}: AQI={} Level={}", stationId, aqiValue, aqiLevel);

        // 3. Publish to Kafka for Realtime (WebSocket) and Downlink (MQTT)
        try {
            Map<String, Object> alertPayload = Map.of(
                "stationId", stationId,
                "aqiValue", aqiValue,
                "level", aqiLevel,
                "pollutant", pollutant,
                "command", "AQI_BAD",
                "timestamp", Instant.now().toString()
            );
            kafkaTemplate.send("aqi.alert", stationId, alertPayload);
            log.info("Published aqi.alert event to Kafka for station {}", stationId);
        } catch (Exception e) {
            log.error("Failed to publish aqi.alert to Kafka", e);
        }

        // 4. Fetch Users for the Station
        try {
            BaseResponse<List<UserDto>> response = userClient.getUsersByStation(stationId);
            if (response != null && response.getData() != null && !response.getData().isEmpty()) {
                List<UserDto> users = response.getData();
                for (UserDto user : users) {
                    String method = user.getNotificationMethod() != null ? user.getNotificationMethod() : "NONE";
                    if ("EMAIL".equalsIgnoreCase(method) || "ALL".equalsIgnoreCase(method)) {
                        sendAqiEmail(user, stationId, aqiValue, aqiLevel, pollutant);
                    }
                }
            }
        } catch (Exception e) {
            log.error("Failed to fetch users from user-service for station {}", stationId, e);
            return; 
        }

        // 5. Update Rate Limit Key in Redis (60 mins)
        redisTemplate.opsForValue().set(redisKey, "sent", 60, TimeUnit.MINUTES);
    }

    private void sendAqiEmail(UserDto user, String stationId, Integer aqiValue, String aqiLevel, String pollutant) {
        String email = user.getEmail();
        if (email == null || !email.contains("@")) return;

        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom("px.tien.2004@gmail.com");
            message.setTo(email);
            message.setSubject("[QUANTRAC] CẢNH BÁO CHẤT LƯỢNG KHÔNG KHÍ (AQI)");
            message.setText(String.format("Kính gửi %s,\n\nTrạm quan trắc của bạn đang ghi nhận mức độ ô nhiễm không khí tăng cao.\n\n- Chỉ số AQI: %d\n- Mức cảnh báo: %s\n- Chất ô nhiễm chính: %s\n\nVui lòng kiểm tra lại hệ thống và có biện pháp xử lý kịp thời.", 
                    user.getFullName() != null ? user.getFullName() : user.getUsername(),
                    aqiValue, aqiLevel, pollutant));
            
            mailSender.send(message);
            log.info("Sent AQI EMAIL alert to {}", email);
        } catch (Exception e) {
            log.error("Failed to send AQI EMAIL to {}", email, e);
        }
    }
}
