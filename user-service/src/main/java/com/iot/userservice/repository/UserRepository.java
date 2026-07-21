package com.iot.userservice.repository;

import com.iot.userservice.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;

public interface UserRepository extends JpaRepository<User,String>, JpaSpecificationExecutor<User> {
    List<User> findByStationId(String stationId);
    List<User> findByRole(String role);
    List<User> findByStationIdAndRole(String stationId, String role);
}
