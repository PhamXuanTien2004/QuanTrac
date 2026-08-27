package com.iot.userservice.repository;

import com.iot.userservice.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;

public interface UserRepository extends JpaRepository<User,String>, JpaSpecificationExecutor<User> {
    List<User> findByStationId(String stationId);
    List<User> findByRole(String role);
    List<User> findByStationIdAndRole(String stationId, String role);
    java.util.Optional<User> findByUsername(String username);

    List<User> findByDeletedAtIsNull();
    List<User> findByStationIdAndDeletedAtIsNull(String stationId);

    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.transaction.annotation.Transactional
    @org.springframework.data.jpa.repository.Query("UPDATE User u SET u.deletedAt = CURRENT_TIMESTAMP WHERE u.id = :id")
    void softDelete(String id);
}
