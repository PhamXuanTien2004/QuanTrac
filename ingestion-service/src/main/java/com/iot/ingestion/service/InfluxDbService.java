package com.iot.ingestion.service;

import com.influxdb.client.InfluxDBClient;
import com.influxdb.client.domain.WritePrecision;
import com.influxdb.client.write.Point;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
@RequiredArgsConstructor
@Slf4j
public class InfluxDbService {

    private final InfluxDBClient influxDBClient;

    @Value("${influxdb.bucket}")
    private String bucket;

    @Value("${influxdb.org}")
    private String orgName;

    public void writeTelemetry(String sensorId, String stationId, String type, Double value, Instant timestamp) {
        try {
            // Thiết kế Point tối ưu Line Protocol để lưu trữ chuỗi thời gian
            Point point = Point.measurement("telemetry_reading")
                    .addTag("station_id", stationId)
                    .addTag("sensor_id", sensorId)
                    .addTag("sensor_type", type)
                    .addField("value", value)
                    .addField("status_code", 200)
                    .time(timestamp, WritePrecision.NS);

            // Ghi dữ liệu bất đồng bộ tối ưu hóa tài nguyên hệ thống
            influxDBClient.getWriteApiBlocking().writePoint(bucket, orgName, point);
            log.info("Đã ghi InfluxDB thành công: Sensor={} | Val={}", sensorId, value);
        } catch (Exception e) {
            log.error("Ghi InfluxDB thất bại cho Sensor: " + sensorId, e);
        }
    }
}