package com.example.deviceservice.service.impl;

import com.example.deviceservice.common.GenericSpecification;
import com.example.deviceservice.dto.request.Gateway.CreateGatewayRequest;
import com.example.deviceservice.dto.request.Gateway.GatewayFilterRequest;
import com.example.deviceservice.dto.request.Gateway.UpdateGatewayRequest;
import com.example.deviceservice.dto.response.Gateway.GatewayResponse;
import com.example.deviceservice.entity.Gateway;
import com.example.deviceservice.entity.Station;
import com.example.deviceservice.entity.Status;
import com.example.deviceservice.exception.ApplicationException;
import com.example.deviceservice.mapper.GatewayMapper;
import com.example.deviceservice.repository.GatewayRepository;
import com.example.deviceservice.repository.StationRepository;
import com.example.deviceservice.repository.specification.GatewaySpecification;
import com.example.deviceservice.service.GatewayService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class GatewayServiceImpl implements GatewayService {

    private final GatewayRepository gatewayRepository;
    private final GatewayMapper gatewayMapper;
    private final StationRepository stationRepository;

    @PersistenceContext
    private final EntityManager entityManager;

    @Override
    @Transactional
    public GatewayResponse createGateway(CreateGatewayRequest request) {
        Station station = stationRepository.findById(request.getStationId())
                .orElseThrow(() -> new ApplicationException("Không tìm thấy Trạm vật lý với ID: " + request.getStationId()));

        if (gatewayRepository.existsByCode(request.getCode())) {
            throw new ApplicationException("Mã Gateway [" + request.getCode() + "] này đã tồn tại!");
        }

        if (request.getSerialNumber() != null && gatewayRepository.existsBySerialNumber(request.getSerialNumber())) {
            throw new ApplicationException("Số Serial thiết bị [" + request.getSerialNumber() + "] này đã tồn tại!");
        }

        Gateway gateway = gatewayMapper.fromCreate(request);
        gateway.setStation(station);
        gateway.setStatus(Status.OFFLINE);
        gateway.setLastSeen(Instant.now());
        Gateway savedGateway = gatewayRepository.saveAndFlush(gateway);
        entityManager.refresh(savedGateway); // Ép đồng bộ dữ liệu Audit "admin" & createdDate ngược lên Java

        return gatewayMapper.toResponse(savedGateway);
    }

    @Override
    @Transactional
    public GatewayResponse updateGateway(UpdateGatewayRequest request) {
        Gateway gateway = gatewayRepository.findById(request.getId())
                .orElseThrow(() -> new ApplicationException("Không tìm thấy thiết bị Gateway với ID: " + request.getId()));

        // Kiểm tra trùng mã code loại trừ chính nó
        if (request.getCode() != null && !request.getCode().equals(gateway.getCode())) {
            if (gatewayRepository.existsByCodeAndIdNot(request.getCode(), request.getId())) {
                throw new ApplicationException("Mã Gateway [" + request.getCode() + "] đã được sử dụng bởi thiết bị khác!");
            }
        }

        // Kiểm tra trùng Serial phần cứng loại trừ chính nó
        if (request.getSerialNumber() != null && !request.getSerialNumber().equals(gateway.getSerialNumber())) {
            if (gatewayRepository.existsBySerialNumberAndIdNot(request.getSerialNumber(), request.getId())) {
                throw new ApplicationException("Số Serial [" + request.getSerialNumber() + "] đã được cấu hình trên hệ thống!");
            }
        }

        // Nếu có nhu cầu đổi Trạm (Station) sở hữu
        if (request.getStationId() != null && !request.getStationId().equals(gateway.getStation().getId())) {
            Station newStation = stationRepository.findById(request.getStationId())
                    .orElseThrow(() -> new ApplicationException("Không tìm thấy Trạm mới với ID: " + request.getStationId()));
            gateway.setStation(newStation);
        }

        if (gateway.getStatus() == Status.ONLINE && request.getStatus() == Status.OFFLINE) {
            request.setLastSeen(Instant.now());
        }

        gatewayMapper.updateGatewayFromRequest(request, gateway);

        Gateway savedGateway = gatewayRepository.saveAndFlush(gateway);

        entityManager.refresh(savedGateway);

        return gatewayMapper.toResponse(savedGateway);
    }

    @Override
    @Transactional
    public void deleteGateway(String id) {
        Gateway gateway = gatewayRepository.findById(id)
                .orElseThrow(() -> new ApplicationException("Không tìm thấy thiết bị Gateway với ID: " + id));

        gateway.setIsDeleted(true);
        gateway.setStatus(Status.OFFLINE);
        gateway.setLastSeen(Instant.now());
        gatewayRepository.save(gateway);
    }

    @Override
    public GatewayResponse getGatewayById(String id) {
        Gateway gateway = gatewayRepository.findById(id)
                .orElseThrow(() -> new ApplicationException("Không tìm thấy thiết bị Gateway với ID: " + id));
        return gatewayMapper.toResponse(gateway);
    }

    @Override
    public Page<GatewayResponse> filterGateways(GatewayFilterRequest request, Pageable pageable) {
        // 1. Lưu lại giá trị stationId ra một biến tạm
        String targetStationId = request.getStationId();

        // 2. Đặt trường này về null để GenericSpecification KHÔNG quét trúng và gây lỗi sập hệ thống nữa
        request.setStationId(null);

        // 3. Cho GenericSpecification quét tự động các trường cơ bản còn lại (code, model, status...)
        Specification<Gateway> spec = GenericSpecification.searchByDto(request);

        // 4. Tự viết logic JOIN sang bảng Station để lọc theo ID trạm chuẩn JPA Criteria
        if (targetStationId != null && !targetStationId.trim().isEmpty()) {
            spec = spec.and((root, query, criteriaBuilder) ->
                    // Tương đương câu lệnh SQL: WHERE gateways.station_id = ?
                    criteriaBuilder.equal(root.get("station").get("id"), targetStationId)
            );
        }

        // 5. Quét dữ liệu dưới DB bằng bộ Specification đã được gia cố
        Page<Gateway> gateways = gatewayRepository.findAll(spec, pageable);

        // (Otion) Trả lại dữ liệu ban đầu cho request nếu cần dùng tiếp phía sau
        request.setStationId(targetStationId);

        // 6. Map danh sách sang Response DTO và trả về
        return gateways.map(gatewayMapper::toResponse);
    }

//    public Instant lastOnline ( Status status) {
//        if (status == Status.OFFLINE) {
//            return Instant.now();
//        }
//        return Instant.now();
//    }
}