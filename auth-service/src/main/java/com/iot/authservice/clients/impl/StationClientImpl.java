package com.iot.authservice.clients.impl;

import com.iot.authservice.clients.StationClient;
import com.iot.authservice.clients.dto.response.StationResponse;
import com.iot.authservice.common.BaseResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jboss.resteasy.spi.ApplicationException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;

import java.time.Duration;

@Component
@Slf4j
@RequiredArgsConstructor
public class StationClientImpl implements StationClient {

    private final WebClient.Builder webClientBuilder;

    @Value("${client.device.uri}")
    private String deviceUri;

    @Override
    public StationResponse getByStationId(String stationId) {
        try {
            BaseResponse<StationResponse> response = webClientBuilder.build()
                    .get()
                    // Gọi sang GET http://device-service/api/v1/gateways/{gatewayId}
                    .uri(deviceUri + "/stations/" + stationId)
                    .retrieve()
                    .onStatus(status -> status.is5xxServerError(), clientResponse ->
                            Mono.error(new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Device Service bị lỗi hệ thống (5xx)"))
                    )
                    .bodyToMono(new ParameterizedTypeReference<BaseResponse<StationResponse>>() {})
                    .timeout(Duration.ofSeconds(2)) // Đợi tối đa 2 giây
                    .block(); // Chuyển sang đồng bộ (blocking)

            if (response != null && response.getData() != null) {
                return response.getData();
            }
        } catch (Exception e) {
            log.error("Lỗi khi xác thực Station ID {} qua WebClient: {}", stationId, e.getMessage());
        }
        return null;
    }
}