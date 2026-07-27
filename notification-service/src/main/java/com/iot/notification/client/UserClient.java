package com.iot.notification.client;

import com.iot.notification.dto.BaseResponse;
import com.iot.notification.dto.UserDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;

@FeignClient(name = "user-service")
public interface UserClient {

    @GetMapping("/api/v1/users/station/{stationId}")
    BaseResponse<List<UserDto>> getUsersByStation(@PathVariable("stationId") String stationId);
}
