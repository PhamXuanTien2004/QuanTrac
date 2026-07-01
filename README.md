# Hệ Thống Quan Trắc Và Theo Dõi Dữ Liệu Từ Xa Theo Thời Gian Thực (V5)

Hệ thống được thiết kế theo kiến trúc **Microservices (Spring Boot 3.3.5 / Java 21)** kết hợp mô hình **Kiến trúc hướng sự kiện (Event-Driven Architecture - EDA)** qua Apache Kafka và bộ đệm bộ nhớ RAM Redis, đảm bảo khả năng chịu tải cao, mở rộng linh hoạt và bảo mật tập trung bằng Keycloak (IAM).

---

## 1. Sơ đồ Kiến trúc & Luồng Dữ liệu (EDA & Saga)

```text
  [Thiết bị giả lập Python]
            │
    (MQTTS - Cổng 1883)
            ▼
    [Mosquitto Broker]
            │
            ▼
    [ingestion-service] <───(Đọc Cache < 1ms)───> [Redis Cache (6379)]
            │                                              ▲ (Đồng bộ Kafka)
            ├───(Ghi thô)───> [InfluxDB V2 (8086)]         │
            │                   ├── telemetry_raw          │
            │                   └── telemetry_alerts       │
            │                                              │
            └───(Bắn sự kiện chuẩn hóa)                    │
                        ▼                                  │
          [Kafka Topic: telemetry-normalized]              │
                        │                                  │
                        ▼                                  │
    [alert-service] ──(Lưu sự cố)──> [MySQL (iot_alert_db)]│
                                                           │
                                                           │
  [Web Portal] ➜ [gateway-service (8080)]                  │
                        │                                  │
                        ▼                                  │
  [device-service] ──(Cấu hình thiết bị) ──(MySQL) ────────┘
```

### Luồng nghiệp vụ cốt lõi:
1.  **Nạp và phân loại dữ liệu (Telemetry Ingestion):** Gateway thu thập dữ liệu từ các cảm biến lồng nhau gửi lên Mosquitto. `ingestion-service` nhận tin, kiểm tra tính hợp lệ qua Redis, ghi nhận dữ liệu thô vào InfluxDB (ghi đồng thời 2 bucket `telemetry_raw` và `telemetry_alerts` nếu phát hiện vượt ngưỡng) và đẩy sự kiện lên Kafka.
2.  **Đồng bộ cấu hình dựa trên sự kiện (Event-Driven Metadata Sync):** Khi có thay đổi thiết bị trên `device-service`, hệ thống bắn sự kiện sang Kafka để `ingestion-service` tự động làm mới bộ đệm Redis, đạt mức **0 cuộc gọi HTTP** ở luồng nhận tin thời gian thực.
3.  **Hoàn tác Saga bất đồng bộ (Saga Compensating Transaction):** Khi đăng ký tài khoản mới, nếu `user-service` gặp sự cố không thể đồng bộ dữ liệu vào MySQL, một thông điệp rollback sẽ được bắn lên Kafka để `auth-service` tự động thu hồi và xóa tài khoản lỗi khỏi Keycloak.

---

## 2. Danh mục cổng dịch vụ & Bản đồ Mạng nội bộ

Hệ thống vận hành theo mô hình Lai (Hybrid): Toàn bộ hạ tầng nằm trong Docker, các dịch vụ nghiệp vụ Java chạy trực tiếp trên máy vật lý kết nối vào cổng Docker map ra ngoài.

### Tầng hạ tầng (Layer 1 - Docker Containers)
| Dịch vụ | Ảnh Docker (Image) | Cổng trong Container | Cổng máy vật lý (Host) | Nhiệm vụ |
| :--- | :--- | :--- | :--- | :--- |
| **`mysql-vti`** | `mysql:8.0` | `3306` | `3307` | Lưu trữ dữ liệu quan hệ nghiệp vụ |
| **`vti-keycloak`** | `keycloak:22.0.5` | `8080` | `8082` | Quản lý định danh và cấp phát Token JWT |
| **`redis`** | `redis:7.0-alpine` | `6379` | `6379` | Lưu trữ bộ đệm cấu hình & Rate Limiter |
| **`iot-influxdb`** | `influxdb:2.7-alpine` | `8086` | `8086` | Cơ sở dữ liệu chuỗi thời gian |
| **`iot-mosquitto`** | `eclipse-mosquitto:2` | `1883` | `1883` | MQTT Broker tiếp nhận dữ liệu cảm biến |
| **`kafka`** | `wurstmeister/kafka` | `29092` | `9092` | Message Broker xương sống của hệ thống |
| **`vti-elasticsearch`**| `elasticsearch:7.17.10` | `9200` | `9200` | Công cụ lưu trữ và tìm kiếm Log tập trung |
| **`kb-container`** | `kibana:7.17.10` | `5601` | `5601` | Giao diện trực quan hóa Log hệ thống |
| **`iot-phpmyadmin`** | `phpmyadmin:latest` | `80` | `8085` | Giao diện quản lý CSDL MySQL trực quan |
| **`kafka-ui`** | `provectuslabs/kafka-ui` | `8080` | `8081` | Giao diện quản lý các Kafka Topics |
| **`redis-insight-ui`** | `redisinsight:1.14.0` | `8001` | `8001` | Giao diện kiểm tra bộ đệm Redis RAM |

### Tầng Ứng dụng (Layer 2 - Spring Boot Local Services)
| Tên Dịch vụ | Cổng chạy máy thật | Đăng ký Eureka | Nhiệm vụ |
| :--- | :--- | :--- | :--- |
| **`discovery-service`** | `8761` | Bản thân Server | Điều phối và phát hiện các Microservices |
| **`gateway-service`** | `8080` | `GATEWAY-SERVICE` | Cửa ngõ API, xác thực JWT, chặn Rate Limiting |
| **`auth-service`** | `8081` | `AUTH-SERVICE` | Wrapper Keycloak, đăng ký, đăng nhập, Saga rollback |
| **`device-service`** | `8280` | `DEVICE-SERVICE` | Quản lý Trạm, Cảm biến, Loại cảm biến (CRUD, Filter) |
| **`ingestion-service`** | `8380` | `INGESTION-SERVICE` | Tiếp nhận MQTT, validate qua Redis, ghi InfluxDB & Kafka |

---

## 3. Đặc tả thiết kế Cơ sở dữ liệu nghiệp vụ (MySQL 8.x)

Hệ thống áp dụng cơ chế tự động khởi tạo database khi khởi chạy (`createDatabaseIfNotExist=true`) và tích hợp cờ xóa mềm (`isDeleted`) ở lớp thực thể.

### Cơ sở dữ liệu: `iot_device_db`
*   **Bảng `stations`:** Quản lý thông tin trạm vật lý lắp đặt.
*   **Bảng `sensor_types`:** Quản lý danh mục loại cảm biến (Nhiệt độ, độ ẩm, CO2...) và dải đo lý thuyết.
*   **Bảng `gateways`:** Quản lý bo mạch vi điều khiển trung tâm (Gateway) lắp tại trạm.
*   **Bảng `sensors`:** Quản lý các cảm biến đo đạc vật lý được cắm vào Gateway.

### Cơ sở dữ liệu: `iot_user_db`
*   **Bảng `users`:** Quản lý thông tin hồ sơ phẳng của nhân viên và mối quan hệ phân quyền theo trạm (`stationId`), đồng bộ bất đồng bộ từ Keycloak qua Kafka.

---

## 4. Hướng dẫn vận hành hệ thống

### Bước 1: Khởi động Hạ tầng Docker
Di chuyển vào thư mục dự án và chạy lệnh khởi chạy các container ngầm:
```bash
docker compose up -d
```
*Đảm bảo toàn bộ 11 container ở Layer 1 hiển thị trạng thái `Up` khỏe mạnh trước khi đi tiếp.*

### Bước 2: Thiết lập cấu hình Realm trên Keycloak
1.  Truy cập trang quản trị: `http://localhost:8082` (User: `admin` / Pass: `password`).
2.  Tạo mới một Realm có tên là: `iot-realm`.
3.  Tạo mới một Client OpenID Connect tên là: `web-portal` (Standard Flow enabled, Web Origins: `*`, Redirect URIs: `*`).

### Bước 3: Khởi chạy các dịch vụ Spring Boot cục bộ
Mở dự án trên IntelliJ và khởi chạy các dịch vụ theo đúng thứ tự ưu tiên sau:
1.  **`discovery-service`** (Đợi Eureka lên cổng `8761`).
2.  **`gateway-service`** ( Gateway mở cổng trung tâm `8080`).
3.  **`auth-service`** (Cổng `8081`).
4.  **`user-service`** (Cổng `8085`).
5.  **`device-service`** (Cổng `8280`).
6.  **`ingestion-service`** (Cổng `8380`).

---

## 5. Kịch bản kiểm thử liên kết tích hợp (Integration Testing)

Hệ thống cung cấp sẵn một công cụ giả lập thiết bị đa kịch bản viết bằng Python: `mqtt_simulator_v4.py`.

### Các bước tiến hành chạy thử:
1.  Mở Postman, đăng nhập tài khoản Keycloak để lấy Access Token JWT.
2.  Gửi yêu cầu khởi tạo Trạm, Loại cảm biến và Thiết bị cảm biến hợp lệ xuống CSDL qua Gateway tại đầu cổng `http://localhost:8080/api/v1/stations`.
3.  Di chuyển vào thư mục chứa file python và chạy tệp tin giả lập:
    ```bash
    python mqtt_simulator_v4.py
    ```

### 4 Kịch bản kiểm thử chạy song song trong Simulator:
*   **Kịch bản 1 (HỢP LỆ):** Gửi dữ liệu đúng ID. Kết quả mong đợi: `ingestion-service` in log `[INGESTION-SUCCESS]`, dữ liệu được lưu vào InfluxDB và bắn thành công lên Kafka topic `telemetry-normalized`.
*   **Kịch bản 2 (SAI GATEWAY ID):** Gửi Gateway ID giả mạo. Kết quả: Hệ thống chặn đứng và hủy toàn bộ gói tin ngay lập tức tại Gateway Validation.
*   **Kịch bản 3 (SAI SENSOR ID):** Gửi Sensor ID không tồn tại. Kết quả: Hệ thống báo lỗi xác thực lô cảm biến và hủy gói.
*   **Kịch bản 4 (VƯỢT NGƯỠNG AN TOÀN):** Gửi giá trị đo nằm ngoài dải `10.5` đến `45.5`. Kết quả: Hệ thống ghi nhận giá trị lỗi thô vào cả 2 Buckets `telemetry_raw` và `telemetry_alerts`, đồng thời chuyển hướng phát sự kiện cảnh báo sang Kafka topic `alert-normalized`.

---

