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
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import org.springframework.data.domain.Pageable;
import java.time.Instant;

@Service
@RequiredArgsConstructor
public class StationServiceImpl implements StationService {
    private final StationRepository stationRepository;
    private final StationMapper stationMapper;

    @Override
    @Transactional
    public Station create(CreateStationRequest request) {
        // Kiểm tra mã trạm đã tồn tại chưa
        if (stationRepository.existsByStationCode(request.getStationCode())) {
            throw new ApplicationException("Mã trạm đã tồn tại trên hệ thống!");
        }

        // Kiểm tra tên trạm
        if (stationRepository.existsByName(request.getName())){
            throw  new ApplicationException("Tên trạm đã tồn tại trên hệ thống");
        }

        // Kiểm tra kinh độ và vĩ độ
        if (stationRepository.existsByLongitudeAndLatitude(request.getLongitude(), request.getLatitude())){
            throw  new ApplicationException("Kinh độ và Vĩ độ của trạm đã tồn tại trên hệ thống");
        }
        Station station = stationMapper.toEntity(request);

        station.setIsDeleted(false);
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
        if (station.getIsDeleted() == true){
            throw  new ApplicationException("Đã xóa Station id:" + id);
        }
        station.setIsDeleted(true);

        return stationRepository.save(station);
    }


}
