package com.iot.ingestion.clients.impl;

import com.iot.ingestion.clients.DeviceClient;
import com.iot.ingestion.clients.dto.request.BatchSensorVerifyRequest;
import com.iot.ingestion.clients.dto.request.GatewayFilter;
import com.iot.ingestion.clients.dto.response.DeviceResponse;
import com.iot.ingestion.clients.dto.response.GatewayResponse;
import com.iot.ingestion.common.BaseResponse;
import com.iot.ingestion.exception.ApplicationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.List;

@Component
@Slf4j
@RequiredArgsConstructor
public class DeviceClientImpl implements DeviceClient {

    private final WebClient.Builder webClientBuilder;

    @Value("${client.device.uri}")
    private String deviceUri;


    @Override
    public GatewayResponse findGatewayById(String gatewayId) {
        try {
            BaseResponse<GatewayResponse> response = webClientBuilder.build()
                    .get()
                    // Gọi sang GET http://device-service/api/v1/gateways/{gatewayId}
                    .uri(deviceUri + "/gateways/" + gatewayId)
                    .retrieve()
                    .onStatus(status -> status.is5xxServerError(), clientResponse ->
                            Mono.error(new ApplicationException("Device Service bị lỗi hệ thống (5xx)"))
                    )
                    .bodyToMono(new ParameterizedTypeReference<BaseResponse<GatewayResponse>>() {})
                    .timeout(Duration.ofSeconds(2)) // Đợi tối đa 2 giây
                    .block(); // Chuyển sang đồng bộ (blocking)

            if (response != null && response.getData() != null) {
                return response.getData();
            }
        } catch (Exception e) {
            log.error("Lỗi khi xác thực Gateway ID {} qua WebClient: {}", gatewayId, e.getMessage());
        }
        return null;
    }

    @Override
    public List<DeviceResponse> verifySensorsBatch(BatchSensorVerifyRequest request) {
        try {
            BaseResponse<List<DeviceResponse>> response = webClientBuilder.build()
                    .post()
                    .uri(deviceUri + "/sensors/batch-verify") // Gọi tới API xác thực lô mới
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<BaseResponse<List<DeviceResponse>>>() {})
                    .timeout(Duration.ofSeconds(3))
                    .block();

            if (response != null && response.getData() != null) {
                return response.getData();
            }
        } catch (Exception e) {
            log.error("Xác thực lô cảm biến thất bại qua WebClient: {}", e.getMessage());
        }
        return List.of(); // Trả về danh sách rỗng nếu xảy ra lỗi mạng
    }
}