package com.example.deviceservice.service.impl;

import com.example.deviceservice.common.GenericSpecification;
import com.example.deviceservice.dto.request.Gateway.CreateGatewayRequest;
import com.example.deviceservice.dto.request.Gateway.GatewayFilterRequest;
import com.example.deviceservice.dto.request.Gateway.UpdateGatewayRequest;
import com.example.deviceservice.dto.response.GatewayResponse;
import com.example.deviceservice.entity.Gateway;
import com.example.deviceservice.entity.Station;
import com.example.deviceservice.entity.Status;
import com.example.deviceservice.exception.ApplicationException;
import com.example.deviceservice.mapper.GatewayMapper;
import com.example.deviceservice.repository.GatewayRepository;
import com.example.deviceservice.repository.StationRepository;
import com.example.deviceservice.service.GatewayService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

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
        if (gateway.getIsDeleted() == true){
            throw new ApplicationException("Đã xóa Gateway id: " + id);
        }
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
    public Page<GatewayResponse> filterGateways(GatewayFilterRequest request) {

        // 3. Cho GenericSpecification quét tự động các trường cơ bản còn lại (code, model, status...)
        Specification<Gateway> spec = GenericSpecification.searchByDto(request);

        Pageable pageable = PageRequest.of(
                request.getPage(),
                request.getSize(),
                Sort.by(Sort.Direction.fromString(request.getSortDir()), request.getSortBy())
        );

        return gatewayRepository.findAll(spec, pageable).map(gatewayMapper::toResponse);
    }
    
}