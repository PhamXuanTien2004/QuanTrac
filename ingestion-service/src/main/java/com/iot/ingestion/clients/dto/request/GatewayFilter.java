package com.iot.ingestion.clients.dto.request;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GatewayFilter {
    private String gatewayId;
}
