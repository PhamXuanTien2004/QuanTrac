package com.iot.authservice.repository;

import com.iot.authservice.entity.UserRegistrationState;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRegistrationStateRepository extends JpaRepository<UserRegistrationState, String> {
}
