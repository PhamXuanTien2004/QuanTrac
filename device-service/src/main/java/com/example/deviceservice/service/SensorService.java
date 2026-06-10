package com.example.deviceservice.service;

import com.example.deviceservice.dto.request.Sensor.SensorCreateDTO;
import com.example.deviceservice.dto.request.Sensor.SensorSearchRequest;
import com.example.deviceservice.dto.request.Sensor.SensorUpdateDTO;
import com.example.deviceservice.dto.response.SensorResponseDTO;
import com.example.deviceservice.entity.Sensor;
import org.springframework.data.domain.Page;

public interface SensorService {
    Sensor create (SensorCreateDTO sensorCreateDTO);
    Sensor update (SensorUpdateDTO sensorUpdateDTO);
    Page<SensorResponseDTO> filter (SensorSearchRequest sensorSearchRequest);
    Sensor delete (String id);
}
