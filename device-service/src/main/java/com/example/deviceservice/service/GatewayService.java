package com.example.deviceservice.service;


import com.example.deviceservice.dto.request.Gateway.CreateGatewayRequest;
import com.example.deviceservice.dto.request.Gateway.GatewayFilterRequest;
import com.example.deviceservice.dto.request.Gateway.UpdateGatewayRequest;

import com.example.deviceservice.dto.response.Gateway.GatewayResponse;
import com.example.deviceservice.entity.Gateway;
import com.example.deviceservice.repository.GatewayRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface GatewayService {
    GatewayResponse createGateway(CreateGatewayRequest createGatewayRequest);
    GatewayResponse updateGateway(UpdateGatewayRequest request);
    void deleteGateway(String id) ;
    GatewayResponse getGatewayById(String id) ;
    Page<GatewayResponse> filterGateways(GatewayFilterRequest request, Pageable pageable);

}
