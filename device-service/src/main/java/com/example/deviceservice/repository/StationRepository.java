package com.example.deviceservice.repository;

import com.example.deviceservice.entity.Station;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;


public interface StationRepository extends JpaRepository<Station, String>, JpaSpecificationExecutor<Station> {

    boolean existsById(String id);

    boolean existsByStationCodeAndIsDeletedFalse(String stationCode);

    java.util.Optional<Station> findByStationCode(String stationCode);

    boolean existsByNameAndIsDeletedFalse(String name);

    boolean existsByLongitudeAndLatitudeAndIsDeletedFalse(Double longitude, Double latitude);

    java.util.Optional<Station> findByNameIgnoreCaseAndIsDeletedFalse(String name);
}
