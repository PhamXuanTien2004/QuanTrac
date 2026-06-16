# Luồng Dữ Liệu Cảm Biến Giả Lập - MQTT TLS → Node-Red → InfluxDB

## Tổng Quan Kiến Trúc

```
Python Sensor Simulator (TLS) → Mosquitto MQTT Broker (TLS) → Node-Red → InfluxDB → Grafana
```

## 1. Chuẩn Bị Môi Trường

### 1.1 Cài đặt Dependencies

```bash
cd test-gateway
pip install -r requirements.txt
```

### 1.2 Khởi động Docker Containers

```bash
docker-compose up -d
```

Kiểm tra containers đang chạy:
```bash
docker-compose ps
```

## 2. Chạy Sensor Simulator

### 2.1 Bắt đầu Script Giả Lập Cảm Biến

```bash
cd test-gateway
python sensor_simulator.py
```

**Kết quả mong đợi:**
```
============================================================
  SENSOR DATA SIMULATOR - MQTT TLS
============================================================
✓ Cấu hình TLS an toàn hoàn thành
▶ Đang kết nối tới localhost:8883 (TLS)...
✓ Đã kết nối an toàn với Mosquitto (TLS)
▶ Bắt đầu gửi dữ liệu mỗi 5s...

[14:30:45] ✓ Gửi 4 dữ liệu cảm biến
     Temperature Sensor 1: 23.45 °C
     Humidity Sensor 1: 65.23 %
     Temperature Sensor 2: 28.12 °C
     Pressure Sensor: 1013.45 hPa
```

## 3. Cấu Hình Node-Red

### 3.1 Truy Cập Node-Red
- URL: http://localhost:1880
- Username/Password: default (không cần nếu chưa cấu hình)

### 3.2 Import Flow

1. Vào **Menu** → **Import**
2. Sao chép nội dung từ file `node-red/sensor_data_flow.json`
3. Paste vào dialog và click **Import**

### 3.3 Cấu Hình MQTT Broker trong Node-Red

1. Tìm node **MQTT In** trong flow
2. Click nút **Broker** để cấu hình:
   - **Server**: `mosquitto` (hoặc `localhost`)
   - **Port**: `8883`
   - **Enable secure (SSL/TLS)**: ☑
   - **Enable strict certificate verification**: ☐ (tắt nếu gặp lỗi certificate)

### 3.4 Cấu Hình InfluxDB

1. Tìm node **InfluxDB** trong flow
2. Click nút **Config** để cấu hình:
   - **Hostname**: `influxdb`
   - **Port**: `8086`
   - **Database**: `sensor-data`
   - **InfluxDB Version**: `2.0`
   - **Token**: `my-super-token` (từ docker-compose.yml)
   - **Organization**: `iot-org`
   - **Bucket**: `sensor-data`

### 3.5 Deploy Flow
- Click **Deploy** button (góc trên bên phải)

## 4. Kiểm Chứng Dữ Liệu

### 4.1 Node-Red Debug Panel
1. Mở **Debug** panel (bên phải)
2. Xem các tin nhắn MQTT được nhận từ Mosquitto

### 4.2 InfluxDB
- URL: http://localhost:8086
- Username: `admin`
- Password: `admin123456`

Kiểm tra dữ liệu:
1. Vào **Data Explorer**
2. Chọn Bucket: `sensor-data`
3. Chọn Measurement: `sensor_data`

### 4.3 Grafana
- URL: http://localhost:3000
- Username: `admin`
- Password: `admin123`

## 5. Cấu Trúc Dữ Liệu MQTT

### Dữ liệu được gửi từ Python:

```json
{
  "batch_timestamp": "2026-06-11 14:30:45",
  "sensor_count": 4,
  "readings": [
    {
      "timestamp": "2026-06-11 14:30:45",
      "sensor_id": "sensor_001",
      "sensor_name": "Temperature Sensor 1",
      "location": "Room 1",
      "value": 23.45,
      "unit": "°C",
      "status": "OK"
    },
    ...
  ]
}
```

### Topic MQTT:
- **Topic**: `quantrac/data`
- **QoS**: 1 (At least once)
- **Frequency**: 5 giây/lần

## 6. Các Sensors được Giả Lập

| ID | Name | Location | Range |
|----|------|----------|-------|
| sensor_001 | Temperature Sensor 1 | Room 1 | 15-35°C |
| sensor_002 | Humidity Sensor 1 | Room 1 | 20-90% |
| sensor_003 | Temperature Sensor 2 | Room 2 | 15-35°C |
| sensor_004 | Pressure Sensor | Outside | 1000-1020 hPa |

## 7. Tùy Chỉnh

### Thay đổi tần suất gửi dữ liệu
Trong `sensor_simulator.py`, sửa:
```python
SEND_INTERVAL = 5  # Thay đổi từ 5 giây sang giá trị khác
```

### Thêm thêm sensors
Thêm vào danh sách `SENSORS`:
```python
SENSORS = [
    # ... sensors hiện có
    {"id": "sensor_005", "name": "CO2 Sensor", "location": "Room 1"},
]
```

### Thay đổi loại dữ liệu
Cập nhật logic trong hàm `generate_sensor_data()` để tạo dữ liệu khác.

## 8. Troubleshooting

### Lỗi TLS Certificate
- Kiểm tra các file certs tồn tại: `mosquitto/certs/`
  - `ca.crt`
  - `client.crt`
  - `client.key`
  - `server.crt`
  - `server.key`

### Lỗi Kết Nối Mosquitto
```bash
# Kiểm tra Mosquitto logs
docker logs mqtt-broker

# Kiểm tra port
netstat -an | grep 8883
```

### Node-Red không nhận dữ liệu
1. Kiểm tra MQTT broker configuration
2. Kiểm tra Debug panel cho errors
3. Xem Mosquitto logs

### InfluxDB không nhận dữ liệu
1. Kiểm tra InfluxDB configuration trong Node-Red
2. Xem Node-Red logs
3. Kiểm tra bucket tồn tại: `sensor-data`

## 9. Dừng Chương Trình

```bash
# Dừng sensor simulator
Ctrl+C

# Dừng Docker containers
docker-compose down
```

---

## Sơ đồ Flow Node-Red

```
[MQTT In: quantrac/data] 
  ↓
[JSON Parse]
  ↓
[Split Readings]
  ↓
[Transform: sensor_data → InfluxDB format]
  ↓
[Write InfluxDB]
```

Mỗi batch từ Python được split thành các reading riêng lẻ, transform thành format InfluxDB, và ghi vào database.
