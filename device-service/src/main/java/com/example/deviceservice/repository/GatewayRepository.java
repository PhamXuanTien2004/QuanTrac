package com.example.deviceservice.repository;

import com.example.deviceservice.entity.Gateway;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface GatewayRepository extends JpaRepository<Gateway, String>, JpaSpecificationExecutor<Gateway> {

    boolean existsByCodeAndIsDeletedFalse(String code);

    boolean existsBySerialNumberAndIsDeletedFalse(String serialNumber);

    boolean existsByCodeAndIdNotAndIsDeletedFalse(String code, String id);

    boolean existsBySerialNumberAndIdNotAndIsDeletedFalse(String serialNumber, String id);

    java.util.Optional<Gateway> findByCodeIgnoreCase(String code);

    java.util.List<Gateway> findAllByStationId(String stationId);
}