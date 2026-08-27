package com.iot.notification.config;

import com.influxdb.client.InfluxDBClient;
import com.influxdb.client.InfluxDBClientFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class InfluxDbConfig {

    @Value("${influxdb.url:http://localhost:8086}")
    private String url;

    @Value("${influxdb.token:my-token}")
    private String token;

    @Value("${influxdb.org:iot_org}")
    private String org;

    @Value("${influxdb.bucket:telemetry_raw}")
    private String bucket;

    @Bean
    public InfluxDBClient influxDBClient() {
        return InfluxDBClientFactory.create(url, token.toCharArray(), org, bucket);
    }
}
