package com.example.deviceservice.repository;

import com.example.deviceservice.entity.Gateway;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface GatewayRepository extends JpaRepository<Gateway, String>, JpaSpecificationExecutor<Gateway> {

    boolean existsByCode(String code);

    boolean existsBySerialNumber(String serialNumber);

    boolean existsByCodeAndIdNot(String code, String id);

    boolean existsBySerialNumberAndIdNot(String serialNumber, String id);
}