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
import com.example.deviceservice.repository.specification.GatewaySpecification;
import com.example.deviceservice.service.GatewayService;
import com.example.deviceservice.service.SensorService;
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
    private final SensorService sensorService;

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    @Transactional
    public GatewayResponse createGateway(CreateGatewayRequest request) {
        Station station = stationRepository.findByNameIgnoreCaseAndDeletedAtIsNull(request.getStationName())
                .orElseThrow(() -> new ApplicationException("Không tìm thấy Trạm vật lý với tên: " + request.getStationName()));

        java.util.Optional<Gateway> existingGatewayOpt = gatewayRepository.findByCodeIgnoreCase(request.getCode());

        if (existingGatewayOpt.isPresent()) {
            Gateway existingGateway = existingGatewayOpt.get();
            if (!existingGateway.isDeleted()) {
                throw new ApplicationException("Mã Gateway [" + request.getCode() + "] này đã tồn tại!");
            }
            existingGateway.setStation(station);
            existingGateway.setStatus(Status.OFFLINE);
            existingGateway.setLastSeen(Instant.now());
            existingGateway.setDeletedAt(null);

            Gateway savedGateway = gatewayRepository.saveAndFlush(existingGateway);
            entityManager.refresh(savedGateway);
            return gatewayMapper.toResponse(savedGateway);
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
            if (gatewayRepository.existsByCodeAndIdNotAndDeletedAtIsNull(request.getCode(), request.getId())) {
                throw new ApplicationException("Mã Gateway " + request.getCode() + " này đã tồn tại!");
            }
            gateway.setCode(request.getCode());
        }

        // Nếu có nhu cầu đổi Trạm (Station) sở hữu
        if (request.getStationName() != null && !request.getStationName().equals(gateway.getStation().getName())) {
            Station newStation = stationRepository.findByNameIgnoreCaseAndDeletedAtIsNull(request.getStationName())
                    .orElseThrow(() -> new ApplicationException("Không tìm thấy Trạm mới với Tên: " + request.getStationName()));
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
        if (gateway.isDeleted()){
            throw new ApplicationException("Đã xóa Gateway id: " + id);
        }
        gateway.setDeletedAt(Instant.now());
        gateway.setStatus(Status.OFFLINE);
        gateway.setLastSeen(Instant.now());
        gatewayRepository.save(gateway);

        // Cascade soft delete to all sensors
        com.example.deviceservice.dto.request.Sensor.SensorSearchRequest ssReq = new com.example.deviceservice.dto.request.Sensor.SensorSearchRequest();
        ssReq.setGatewayId(id);
        sensorService.filter(ssReq)
                     .getContent()
                     .forEach(sensor -> sensorService.delete(sensor.getId()));
    }

    @Override
    public GatewayResponse findById(String id) {
        Gateway gateway = gatewayRepository.findById(id)
                .orElseThrow(() -> new ApplicationException("Không tìm thấy thiết bị Gateway với ID: " + id));
        return gatewayMapper.toResponse(gateway);
    }

    @Override
    public Page<GatewayResponse> filterGateways(GatewayFilterRequest request) {

        Specification<Gateway> spec = GatewaySpecification.filterWithRequest(request);

        Pageable pageable = PageRequest.of(
                request.getPage(),
                request.getSize(),
                Sort.by(Sort.Direction.fromString(request.getSortDir()), request.getSortBy())
        );

        return gatewayRepository.findAll(spec, pageable).map(gatewayMapper::toResponse);
    }
    
}