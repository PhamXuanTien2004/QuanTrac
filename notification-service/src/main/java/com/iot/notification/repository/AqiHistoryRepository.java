package com.iot.notification.repository;

import com.iot.notification.entity.AqiHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.List;
import org.springframework.data.jpa.repository.Query;

@Repository
public interface AqiHistoryRepository extends JpaRepository<AqiHistory, Long> {
    Optional<AqiHistory> findFirstByStationIdOrderByCalculatedAtDesc(String stationId);
    List<AqiHistory> findByStationIdAndCalculatedAtBetweenOrderByCalculatedAtDesc(String stationId, java.time.Instant start, java.time.Instant end);
    List<AqiHistory> findByStationIdAndCalculatedAtBetweenOrderByCalculatedAtAsc(String stationId, java.time.Instant start, java.time.Instant end);

    @Query("SELECT a FROM AqiHistory a WHERE a.calculatedAt = (SELECT MAX(b.calculatedAt) FROM AqiHistory b WHERE b.stationId = a.stationId)")
    List<AqiHistory> findLatestAqiForAllStations();
}
