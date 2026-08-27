package com.iot.notification.client;

import com.iot.notification.dto.BaseResponse;
import com.iot.notification.dto.SensorDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "device-service")
public interface DeviceClient {

    @GetMapping("/api/v1/sensors/{id}")
    BaseResponse<SensorDto> getSensorById(@PathVariable("id") String id);

    @GetMapping("/api/v1/stations")
    BaseResponse<java.util.List<com.iot.notification.dto.StationDto>> getAllStations();
}
