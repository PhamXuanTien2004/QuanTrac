package com.iot.userservice.controller;

import com.iot.userservice.common.BaseResponse;
import com.iot.userservice.entity.User;
import com.iot.userservice.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping
    public ResponseEntity<BaseResponse<List<User>>> getAllUsers() {
        List<User> users = userService.findAll();
        BaseResponse<List<User>> response = BaseResponse.success(users);
        response.setMessage("Lấy danh sách người dùng thành công");
        return ResponseEntity.ok(response);
    }

    @GetMapping("/station/{stationId}")
    public ResponseEntity<BaseResponse<List<User>>> getUsersByStation(@PathVariable("stationId") String stationId) {
        List<User> users = userService.findByStationId(stationId);
        BaseResponse<List<User>> response = BaseResponse.success(users);
        response.setMessage("Lấy danh sách người dùng theo trạm thành công");
        return ResponseEntity.ok(response);
    }
}
