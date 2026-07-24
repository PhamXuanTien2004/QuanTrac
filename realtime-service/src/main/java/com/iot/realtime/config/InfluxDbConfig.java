package com.iot.realtime.config;

import com.influxdb.client.InfluxDBClient;
import com.influxdb.client.InfluxDBClientFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class InfluxDbConfig {

    @Value("${influxdb.url}")
    private String url;

    @Value("${influxdb.token}")
    private String token;

    @Value("${influxdb.org}")
    private String org;

    @Value("${influxdb.bucket}")
    private String bucket;

    @Bean
    public InfluxDBClient influxDBClient() {
        // Spring Boot sẽ gọi hàm này khi khởi động để tạo ra đối tượng InfluxDBClient
        // và lưu vào Application Context.
        return InfluxDBClientFactory.create(url, token.toCharArray(), org, bucket);
    }
}