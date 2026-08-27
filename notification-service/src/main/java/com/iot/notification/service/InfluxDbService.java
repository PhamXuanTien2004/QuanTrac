package com.iot.notification.service;

import com.influxdb.client.InfluxDBClient;
import com.influxdb.query.FluxRecord;
import com.influxdb.query.FluxTable;
import com.iot.notification.dto.TelemetryResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class InfluxDbService {

    private final InfluxDBClient influxDBClient;

    @Value("${influxdb.bucket:telemetry_raw}")
    private String bucket;

    @Value("${influxdb.org:iot_org}")
    private String orgName;

    public List<TelemetryResponse> getLatestTelemetry(String stationId) {
        String flux = String.format(
            "from(bucket: \"%s\")\n" +
            "  |> range(start: -24h)\n" +
            "  |> filter(fn: (r) => r._measurement == \"telemetry_reading\" and r.station_id == \"%s\")\n" +
            "  |> filter(fn: (r) => r._field == \"value\")\n" +
            "  |> last()",
            bucket, stationId
        );
        return executeQuery(flux);
    }

    public List<TelemetryResponse> getHistoricalTelemetry(String stationId, Instant startTime, Instant endTime) {
        String flux = String.format(
            "from(bucket: \"%s\")\n" +
            "  |> range(start: %s, stop: %s)\n" +
            "  |> filter(fn: (r) => r._measurement == \"telemetry_reading\" and r.station_id == \"%s\")\n" +
            "  |> filter(fn: (r) => r._field == \"value\")\n" +
            "  |> yield(name: \"sort\")",
            bucket, startTime.toString(), endTime.toString(), stationId
        );
        return executeQuery(flux);
    }

    public List<TelemetryResponse> getHourlyAggregatedTelemetry(String stationId, Instant startTime, Instant endTime) {
        String flux = String.format(
            "from(bucket: \"%s\")\n" +
            "  |> range(start: %s, stop: %s)\n" +
            "  |> filter(fn: (r) => r._measurement == \"telemetry_reading\" and r.station_id == \"%s\")\n" +
            "  |> filter(fn: (r) => r._field == \"value\")\n" +
            "  |> aggregateWindow(every: 1h, fn: mean, createEmpty: false)\n" +
            "  |> yield(name: \"mean\")",
            bucket, startTime.toString(), endTime.toString(), stationId
        );
        return executeQuery(flux);
    }

    private List<TelemetryResponse> executeQuery(String flux) {
        List<TelemetryResponse> results = new ArrayList<>();
        java.util.concurrent.CountDownLatch latch = new java.util.concurrent.CountDownLatch(1);
        try {
            influxDBClient.getQueryApi().query(flux, orgName, 
                (cancellable, fluxRecord) -> {
                    results.add(TelemetryResponse.builder()
                            .sensorId((String) fluxRecord.getValueByKey("sensor_id"))
                            .sensorType((String) fluxRecord.getValueByKey("sensor_type"))
                            .value(((Number) fluxRecord.getValue()).doubleValue())
                            .timestamp(fluxRecord.getTime())
                            .build());
                },
                (error) -> {
                    log.error("Error during InfluxDB query callback", error);
                    latch.countDown();
                },
                () -> {
                    latch.countDown();
                }
            );
            latch.await(5, java.util.concurrent.TimeUnit.SECONDS);
        } catch (Throwable e) {
            log.error("Query InfluxDB failed (Throwable caught): " + flux, e);
        }
        return results;
    }
}
