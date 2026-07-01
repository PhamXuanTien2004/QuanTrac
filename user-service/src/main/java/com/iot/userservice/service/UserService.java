package com.iot.userservice.service;

import com.iot.userservice.entity.User;
import com.iot.userservice.event.dto.UserCreatedEvent;

public interface UserService {
    User create (UserCreatedEvent userCreatedEvent);
}
