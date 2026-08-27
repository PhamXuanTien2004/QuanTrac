package com.iot.notification.repository;

import com.iot.notification.entity.AlertHistory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AlertHistoryRepository extends JpaRepository<AlertHistory, Long> {
    List<AlertHistory> findTop20ByStationIdOrderByTimestampDesc(String stationId);
    List<AlertHistory> findByStationIdAndTimestampBetweenOrderByTimestampAsc(String stationId, java.time.Instant start, java.time.Instant end);
    Page<AlertHistory> findByStationIdAndTimestampBetweenOrderByTimestampDesc(String stationId, java.time.Instant start, java.time.Instant end, Pageable pageable);
}
