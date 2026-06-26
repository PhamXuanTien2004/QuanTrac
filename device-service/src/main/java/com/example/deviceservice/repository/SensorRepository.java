package com.example.deviceservice.repository;

import com.example.deviceservice.entity.Sensor;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SensorRepository extends JpaRepository<Sensor, String>, JpaSpecificationExecutor<Sensor> {
    boolean existsBySensorCodeAndIsDeletedFalse(String sensorCode);
    @Query("SELECT s FROM Sensor s " +
            "WHERE s.id IN :sensorIds " +
            "AND s.gatewayId = :gatewayId " +
            "AND s.isDeleted = false " +
            "AND s.status = com.example.deviceservice.entity.Status.ACTIVE")
    List<Sensor> validateSensorsBatch(
            @Param("gatewayId") String gatewayId,
            @Param("sensorIds") List<String> sensorIds
    );
}
