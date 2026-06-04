package com.example.deviceservice.controller;

import com.example.deviceservice.common.BaseResponse;
import com.example.deviceservice.dto.request.Station.CreateStationRequest;
import com.example.deviceservice.dto.request.Station.FilterStationRequest;
import com.example.deviceservice.dto.request.Station.UpdateStationRequest;
import com.example.deviceservice.dto.response.Station.StationFilterResponse;
import com.example.deviceservice.dto.response.Station.StationResponse;
import com.example.deviceservice.entity.Station;
import com.example.deviceservice.mapper.StationMapper;
import com.example.deviceservice.service.StationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/stations")
public class StationController {
    private final StationService stationService;
    private final StationMapper  stationMapper;

    @PostMapping
    public ResponseEntity<BaseResponse<Station>> create(@Valid @RequestBody CreateStationRequest createStationRequest){
        Station stationResponse = stationService.create(createStationRequest);
        BaseResponse<Station> response = BaseResponse.success(stationResponse);
        response.setMessage("success");
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/filter")
    public ResponseEntity<BaseResponse<Page<StationResponse>>> filter(
            @RequestBody FilterStationRequest filterRequest,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        // 1. Tạo đối tượng Pageable từ page và size
        Pageable pageable = PageRequest.of(page, size);

        // 2. Truyền CẢ 2 tham số (filterRequest và pageable) vào service
        Page<StationResponse> responseData = stationService.filter(filterRequest, pageable);

        return ResponseEntity.ok(BaseResponse.success(responseData));
    }

    @PutMapping("/{id}")
    public ResponseEntity<BaseResponse<StationResponse>> updateStation(
            @PathVariable String id,
            @RequestBody UpdateStationRequest request) {

        request.setId(id); // Set ID từ URL vào Request dto

        StationResponse updatedStationDTO = stationService.update(request);

        return ResponseEntity.ok(BaseResponse.success(updatedStationDTO));
    }

    // Xóa mềm
    @DeleteMapping("/{id}")
    public ResponseEntity<BaseResponse<Void>> delete(@PathVariable String id) {
        stationService.deleteById(id);
        return ResponseEntity.ok(BaseResponse.success(null)); // Xóa thành công ko cần trả data
    }

}
