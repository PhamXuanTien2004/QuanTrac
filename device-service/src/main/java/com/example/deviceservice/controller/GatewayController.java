package com.example.deviceservice.controller;

import com.example.deviceservice.common.BaseResponse;
import com.example.deviceservice.dto.request.Gateway.CreateGatewayRequest;
import com.example.deviceservice.dto.request.Gateway.GatewayFilterRequest;
import com.example.deviceservice.dto.request.Gateway.UpdateGatewayRequest;
import com.example.deviceservice.dto.response.GatewayResponse;
import com.example.deviceservice.service.GatewayService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/gateways")
@RequiredArgsConstructor
public class GatewayController {

    private final GatewayService gatewayService;

    // 1. API Tạo mới Gateway
    @PostMapping
    public ResponseEntity<BaseResponse<GatewayResponse>> create(@Valid @RequestBody CreateGatewayRequest request) {
        GatewayResponse gatewayResponse = gatewayService.createGateway(request);
        BaseResponse<GatewayResponse> response = BaseResponse.success(gatewayResponse);
        response.setMessage("Created Gateway successfully");

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // 2. API Cập nhật Gateway
    @PutMapping("/update")
    public ResponseEntity<BaseResponse<GatewayResponse>> update(@Valid @RequestBody UpdateGatewayRequest request) {
        GatewayResponse gatewayResponse = gatewayService.updateGateway(request);
        BaseResponse<GatewayResponse> response = BaseResponse.success(gatewayResponse);
        response.setMessage("Updated Gateway successfully");

        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    // 3. API Lấy chi tiết Gateway theo ID
    @GetMapping("/{id}")
    public ResponseEntity<BaseResponse<GatewayResponse>> getById(@PathVariable String id) {
        GatewayResponse gatewayResponse = gatewayService.getGatewayById(id);
        BaseResponse<GatewayResponse> response = BaseResponse.success(gatewayResponse);
        response.setMessage("Retrieved Gateway detail successfully");

        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    // 4. API Xóa mềm Gateway
    @DeleteMapping("/{id}")
    public ResponseEntity<BaseResponse<Void>> delete(@PathVariable String id) {
        gatewayService.deleteGateway(id);

        BaseResponse<Void> response = BaseResponse.success(null);
        response.setMessage("Soft deleted Gateway successfully");

        return ResponseEntity.ok(BaseResponse.success(null));
    }

    // 5. API Tìm kiếm/Lọc động Gateway
    @PostMapping("/filter")
    public ResponseEntity<BaseResponse<Page<GatewayResponse>>> filter(GatewayFilterRequest filterRequest) {

        Page<GatewayResponse> responsePage = gatewayService.filterGateways(filterRequest);

        BaseResponse<Page<GatewayResponse>> response = BaseResponse.success(responsePage);
        response.setMessage("Thực hiện tìm kiếm thành công");

        return ResponseEntity.ok(response);
    }
}