package com.example.deviceservice.repository;

import com.example.deviceservice.entity.Sensor;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface SensorRepository extends JpaRepository<Sensor, String>, JpaSpecificationExecutor<Sensor> {
    boolean existsBySensorCodeAndIsDeletedFalse(String sensorCode);
}
