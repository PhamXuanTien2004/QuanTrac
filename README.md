# QuanTrac - IoT Environmental Monitoring System

Complete implementation of a real-time environmental monitoring platform with sensor data ingestion, alert management, and live dashboard.

## Architecture Overview

```
Sensors (MQTT) → Mosquitto Broker → Data Service → MySQL/InfluxDB → Dashboard
                                  ↓
                                Alert Service → Kafka → Notification Service
```

## Project Structure

```
├── device-service/          # Core device management service (Station, Gateway, Sensor, SensorType)
├── data-service/           # Data ingestion & real-time streaming service (NEW)
├── mosquitto/              # MQTT broker configuration
├── test-gateway/           # Sensor simulation & testing
├── node-red/              # Optional workflow automation
├── dashboard/             # React frontend (Phase 3)
├── docker-compose.yml     # Full stack orchestration
└── pom.xml               # Maven monorepo parent
```

## Quick Start - Development

### 1. Start Infrastructure Stack

```bash
# Navigate to project root
cd /f/TIEN_LFS/QuanTrac

# Start all services (MySQL, MQTT, InfluxDB, Grafana, Kafka)
docker-compose up -d

# Verify services are running
docker-compose ps
```

### 2. Run Sensor Simulator

Opens 3 simulated sensors sending data to MQTT every 5 seconds.

```bash
cd test-gateway

# Install dependencies
pip install -r requirements.txt

# Run simulator
python sensor_simulator.py
```

Expected output:
```
✓ Connected to MQTT broker at localhost:1883

Sensor Simulator started - publishing to localhost:1883
Sensors: 3
Gateway: gateway-001

Published TEMP_001: 23.45 °C -> sensor/gateway-001/TEMP_001/data
Published HUM_001: 62.30 % -> sensor/gateway-001/HUM_001/data
Published PRES_001: 1013.25 hPa -> sensor/gateway-001/PRES_001/data
```

### 3. Build & Run Data Service

**Prerequisites:**
- Java 17+
- Maven 3.9+

```bash
cd data-service

# Build
./mvnw clean package

# Run
java -jar target/data-service-*.jar

# Service runs on http://localhost:8081
```

### 4. Access Services

| Service | URL | Login |
|---------|-----|-------|
| Grafana | http://localhost:3000 | admin / admin123 |
| InfluxDB | http://localhost:8086 | admin / admin123456 |
| Data Service API | http://localhost:8081 | - |
| Device Service API | http://localhost:8080 | - |

## API Endpoints

### Data Service (Port 8081)

```
GET    /api/v1/sensors/{sensorId}/latest              # Get latest reading
GET    /api/v1/sensors/{sensorId}/history             # Get historical data (paginated)
GET    /api/v1/sensors/{sensorId}/history/range       # Get data by date range
WS     /ws/sensors/{sensorId}                         # WebSocket for real-time updates

POST   /api/v1/sensor-thresholds                      # Create threshold
GET    /api/v1/sensor-thresholds/sensor/{sensorId}   # Get threshold
PUT    /api/v1/sensor-thresholds/{id}                # Update threshold
DELETE /api/v1/sensor-thresholds/{id}                # Delete threshold
```

### Device Service (Port 8080)

```
POST   /api/v1/stations                  # Create station
POST   /api/v1/gateways                 # Create gateway
POST   /api/v1/sensors                  # Create sensor
POST   /api/v1/sensor-types             # Create sensor type

POST   /api/v1/stations/filter          # Filter with pagination
POST   /api/v1/gateways/filter          # Filter with pagination
POST   /api/v1/sensors/filter           # Filter with pagination
POST   /api/v1/sensor-types/filter      # Filter with pagination
```

## MQTT Message Format

Topic: `sensor/{gateway_id}/{sensor_code}/data`

Payload:
```json
{
  "sensorId": "uuid",
  "sensorCode": "TEMP_001",
  "sensorType": "TEMPERATURE",
  "value": 25.5,
  "unit": "°C",
  "timestamp": 1686470400000,
  "quality": 100,
  "gatewayId": "gateway-001",
  "stationId": "station-001"
}
```

## Database Schema

### MySQL (device_service_db)
- stations
- gateways
- sensors
- sensor_types
- sensor_data_cache (data-service)
- sensor_thresholds (data-service)

### InfluxDB (sensor-data bucket)
- Measurement: `sensor_reading`
- Tags: sensor_id, sensor_code, sensor_type, gateway_id, station_id
- Fields: value, quality

### Kafka Topics
- `sensor-data` - Raw sensor readings for processing
- `alert-trigger` - Alert events for notification service

## Testing Data Flow

### Manual Test: Publish to MQTT

```bash
# Using mosquitto_pub
mosquitto_pub -h localhost -t "sensor/gateway-001/TEST_001/data" \
  -m '{
    "sensorId": "test-sensor-id",
    "sensorCode": "TEST_001",
    "sensorType": "TEST",
    "value": 42.0,
    "unit": "unit",
    "timestamp": 1686470400000,
    "quality": 100,
    "gatewayId": "gateway-001",
    "stationId": "station-001"
  }'
```

### Verify Data Ingestion

```bash
# Check MySQL cache
SELECT COUNT(*) FROM sensor_data_cache;

# Check InfluxDB via API
curl http://localhost:8086/api/v1/query?db=sensor-data \
  -d 'q=SELECT * FROM sensor_reading LIMIT 10'
```

### Test WebSocket Connection

```bash
# Using websocat or similar
websocat ws://localhost:8081/ws/sensors/test-sensor-id
```

## Phase 2 - Alert Service (Upcoming)

- Monitor sensor thresholds
- Publish alert events to Kafka
- Alert state machine (Normal → Warning → Critical)

## Phase 3 - Dashboard (Upcoming)

- React + TypeScript frontend
- Real-time WebSocket updates
- Station map visualization
- Sensor monitoring cards
- Alert notification center
- Threshold configuration UI

## Troubleshooting

### MQTT Connection Issues
```bash
# Check Mosquitto container logs
docker-compose logs mosquitto

# Test MQTT connection
mosquitto_sub -h localhost -t "sensor/+"
```

### Data Service not starting
```bash
# Check logs
docker-compose logs data-service

# Ensure databases are created
# MySQL should auto-create via Hibernate ddl-auto: update
```

### No data in InfluxDB
```bash
# Verify bucket and token
curl http://localhost:8086/api/v2/buckets -H "Authorization: Token my-super-token"
```

## Next Steps

1. Complete Phase 2 (Alert Service)
2. Add user authentication (JWT/Spring Security)
3. Build React Dashboard
4. Configure Nginx/API Gateway
5. Deploy to production

## Contributing

See CLAUDE.md for development guidelines.

## License

MIT
