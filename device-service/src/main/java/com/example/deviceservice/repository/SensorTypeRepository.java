package com.example.deviceservice.repository;

import com.example.deviceservice.entity.SensorType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface SensorTypeRepository extends JpaRepository<SensorType, String>, JpaSpecificationExecutor<SensorType> {
}
