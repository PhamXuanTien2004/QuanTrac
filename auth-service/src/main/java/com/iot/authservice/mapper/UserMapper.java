package com.iot.authservice.mapper;

import com.iot.authservice.dto.request.RegisterRequestDTO;
import com.iot.authservice.dto.request.UserRegistrationDTO;
import com.iot.authservice.entity.User;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface UserMapper {
    User createUser(RegisterRequestDTO userRegistrationDTO);
}

