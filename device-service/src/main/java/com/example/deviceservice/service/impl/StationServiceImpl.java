package com.example.deviceservice.service.impl;

import com.example.deviceservice.common.GenericSpecification;
import com.example.deviceservice.dto.request.Station.CreateStationRequest;
import com.example.deviceservice.dto.request.Station.FilterStationRequest;
import com.example.deviceservice.dto.request.Station.UpdateStationRequest;
import com.example.deviceservice.dto.response.StationResponse;
import com.example.deviceservice.entity.Station;
import com.example.deviceservice.exception.ApplicationException;
import com.example.deviceservice.mapper.StationMapper;
import com.example.deviceservice.repository.StationRepository;
import com.example.deviceservice.service.StationService;
import com.example.deviceservice.service.GatewayService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import org.springframework.data.domain.Pageable;
import java.time.Instant;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class StationServiceImpl implements StationService {
    private final StationRepository stationRepository;
    private final StationMapper stationMapper;
    private final GatewayService gatewayService;

    @Override
    @Transactional
    public Station create(CreateStationRequest request) {
        Optional<Station> existingStationOpt = stationRepository.findByStationCode(request.getStationCode());

        if (existingStationOpt.isPresent()) {
            Station existingStation = existingStationOpt.get();
            if (!existingStation.isDeleted()) {
                throw new ApplicationException("Mã trạm đã tồn tại trên hệ thống!");
            }

            // Restore logic
            if (!existingStation.getName().equals(request.getName()) && stationRepository.existsByNameAndDeletedAtIsNull(request.getName())) {
                throw new ApplicationException("Tên trạm đã tồn tại trên hệ thống");
            }
            if ((!existingStation.getLongitude().equals(request.getLongitude()) || !existingStation.getLatitude().equals(request.getLatitude())) &&
                stationRepository.existsByLongitudeAndLatitudeAndDeletedAtIsNull(request.getLongitude(), request.getLatitude())) {
                throw new ApplicationException("Kinh độ và Vĩ độ của trạm đã tồn tại trên hệ thống");
            }

            existingStation.setName(request.getName());
            existingStation.setDescription(request.getDescription());
            existingStation.setAddress(request.getAddress());
            existingStation.setLatitude(request.getLatitude());
            existingStation.setLongitude(request.getLongitude());
            existingStation.setInstallationDate(request.getInstallationDate());
            existingStation.setStatus(request.getStatus());
            existingStation.setDeletedAt(null);
            
            return stationRepository.save(existingStation);
        }

        // Kiểm tra tên trạm
        if (stationRepository.existsByNameAndDeletedAtIsNull(request.getName())){
            throw  new ApplicationException("Tên trạm đã tồn tại trên hệ thống");
        }

        // Kiểm tra kinh độ và vĩ độ
        if (stationRepository.existsByLongitudeAndLatitudeAndDeletedAtIsNull(request.getLongitude(), request.getLatitude())){
            throw  new ApplicationException("Kinh độ và Vĩ độ của trạm đã tồn tại trên hệ thống");
        }
        
        Station station = stationMapper.toEntity(request);

        station.setDeletedAt(null);
        station.setCreatedDate(Instant.now());

        return stationRepository.save(station);
    }

    @Override
    public Page<StationResponse> filter(FilterStationRequest filter) {
        Specification<Station> spec = GenericSpecification.searchByDto(filter);

        Pageable pageable = PageRequest.of(
                filter.getPage(),
                filter.getSize(),
                Sort.by(Sort.Direction.fromString(filter.getSortDir()), filter.getSortBy())
        );

        return stationRepository.findAll(spec, pageable).map(stationMapper::toResponse);
    }

    @Override
    public java.util.List<StationResponse> findAll() {
        return stationRepository.findAll()
                .stream()
                .filter(station -> !station.isDeleted())
                .map(stationMapper::toResponse)
                .collect(java.util.stream.Collectors.toList());
    }


    @Override
    @Transactional
    public StationResponse update(UpdateStationRequest updateStationRequest) {
        Station station = stationRepository.findById(updateStationRequest.getId())
                .orElseThrow(() -> new ApplicationException("Station not found with id:" +  updateStationRequest.getId()));

        // 1. Cập nhật dữ liệu từ Request vào Entity
        stationMapper.updateStationFromRequest(updateStationRequest, station);

        // 2. Lưu vào DB
        Station savedStation = stationRepository.save(station);

        // 3. Map Entity sang DTO rồi mới trả về
        return stationMapper.toDTO(savedStation);
    }

    @Override
    @Transactional
    public Station deleteById(String id) {
        Station station = stationRepository.findById(id)
                .orElseThrow(() -> new ApplicationException("Station not found with id:" + id));
        if (station.isDeleted()){
            throw  new ApplicationException("Đã xóa Station id:" + id);
        }
        station.setDeletedAt(Instant.now());

        Station savedStation = stationRepository.save(station);

        // Cascade soft delete to all gateways
        com.example.deviceservice.dto.request.Gateway.GatewayFilterRequest gwReq = new com.example.deviceservice.dto.request.Gateway.GatewayFilterRequest();
        gwReq.setStationId(id);
        gatewayService.filterGateways(gwReq)
                      .getContent()
                      .forEach(gw -> gatewayService.deleteGateway(gw.getId()));

        return savedStation;
    }

    @Override
    @Transactional
    public Station findById(String id) {
        Station station = stationRepository.findById(id)
                .orElseThrow(() -> new ApplicationException("Station not found with id:" + id));
        return station;
    }

    @Override
    public StationResponse findByName(String name) {
        Station station = stationRepository.findByNameIgnoreCaseAndDeletedAtIsNull(name)
                .orElseThrow(() -> new ApplicationException("Trạm quan trắc với tên '" + name + "' không tồn tại trên hệ thống!"));
        return stationMapper.toResponse(station);
    }


}
