package com.iot.userservice.service;

import com.iot.userservice.entity.User;
import com.iot.userservice.event.dto.UserCreatedEvent;

import java.util.List;

public interface UserService {
    User create (UserCreatedEvent userCreatedEvent);
    List<User> findAll();
    List<User> findByStationId(String stationId);
}
