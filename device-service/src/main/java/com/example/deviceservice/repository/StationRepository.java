package com.example.deviceservice.repository;

import com.example.deviceservice.entity.Station;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;


public interface StationRepository extends JpaRepository<Station, String>, JpaSpecificationExecutor<Station> {

    boolean existsById(String id);

    boolean existsByStationCode(String stationCode);

    boolean existsByName(String name);

    boolean existsByLongitudeAndLatitude(Double longitude, Double latitude);
}
