================================================================================
          DỰ ÁN: HỆ THỐNG QUAN TRẮC VÀ THEO DÕI DỮ LIỆU TỪ XA THEO THỜI GIAN THỰC
                                  (BẢN CẬP NHẬT ĐẦY ĐỦ - V5)
================================================================================

1. TỔNG QUAN DỰ ÁN
--------------------------------------------------------------------------------
Dự án xây dựng một hệ thống Web Service nhằm thu thập, xử lý, giám sát và hiển thị 
dữ liệu từ các trạm quan trắc từ xa theo thời gian thực. Hệ thống sử dụng kiến trúc 
Microservices (Java Spring Boot) kết hợp mô hình Kiến trúc hướng sự kiện (EDA) qua 
Apache Kafka để đảm bảo tính mở rộng, bảo mật và khả năng chịu tải cao trong môi 
trường doanh nghiệp.

Hạ tầng lưu trữ sử dụng MySQL cho dữ liệu quan hệ nghiệp vụ, InfluxDB cho dữ liệu 
chuỗi thời gian tần suất cao và Redis cho lớp đệm lưu trữ cấu hình.

2. KIẾN TRÚC HỆ THỐNG TỔNG THỂ
--------------------------------------------------------------------------------
Hệ thống được chia thành 4 lớp (Layers) chính:

2.1. Lớp Thu thập dữ liệu (Edge / Device Layer)
- Thiết bị: Các cảm biến tại trạm quan trắc (đo nhiệt độ, độ ẩm, chất lượng không khí, v.v.).
- Giao thức: Truyền tải dữ liệu qua MQTT.
- Broker: Eclipse Mosquitto (Cấu hình chạy an toàn cho phép kết nối nặc danh qua lệnh 
  khởi chạy trực tiếp trong container mà không cần file cấu hình phụ bên ngoài).

2.2. Lớp Xử lý và Lưu trữ dữ liệu (Data Pipeline Layer)
- Ingestion Service: Một dịch vụ stateless viết bằng Java Spring Boot chuyên biệt. 
  Dịch vụ này kết nối trực tiếp đến MQTT Broker để tiêu thụ dữ liệu thô, xác thực 
  thiết bị qua Redis, chuẩn hóa định dạng (JSON), ghi vào InfluxDB và đẩy sự kiện vào Kafka.
- Message Broker: Apache Kafka (Zookeeper-based) đóng vai trò xương sống cho kiến trúc hướng 
  sự kiện (EDA). Dữ liệu nghiệp vụ truyền đi nội bộ qua Listener cổng 29092.
- Time-Series Database: InfluxDB lưu trữ dữ liệu telemetry thông qua 02 Bucket:
  + `telemetry_raw`: Lưu dữ liệu thô chi tiết trong vòng 30 ngày (Cổng 8086).
  + `telemetry_aggregated`: Lưu dữ liệu đã giảm mẫu trung bình theo giờ/ngày để phục 
    vụ truy vấn lịch sử dài hạn (365 ngày).

2.3. Lớp Quản lý Nghiệp vụ (Backend Microservices - Java Spring Boot)
Sử dụng Spring Cloud (Eureka Server, Spring Cloud Gateway) để điều phối hệ thống.
- Database per Service: Mỗi service quản lý một cơ sở dữ liệu MySQL riêng biệt được 
  tự động khởi tạo thông qua thuộc tính kết nối `createDatabaseIfNotExist=true`.
- Caching Layer: Redis lưu trữ dữ liệu metadata tĩnh, cấu hình trạm và token người dùng 
  để tránh truy vấn trực tiếp vào MySQL liên tục.

2.4. Lớp Hiển thị & Giám sát (Presentation & Monitoring Layer)
- Grafana Dashboard: Truy vấn trực tiếp từ InfluxDB để vẽ đồ thị thời gian thực.
- Web Portal (React/Angular): Giao tiếp qua Gateway (Cổng 8080) để quản lý thiết bị, 
  người dùng, cấu hình ngưỡng và gửi yêu cầu xuất báo cáo bất đồng bộ (On-demand).


3. CHI TIẾT CÁC MICROSERVICES (TÊN, NHIỆM VỤ, DATABASE)
--------------------------------------------------------------------------------

3.1. API Gateway & Discovery Server (Thành phần hạ tầng)
- Tên Service: `gateway-service` & `discovery-service`
- Nhiệm vụ: Định tuyến request, cân bằng tải, xác thực tập trung (Oauth2 Client & JWT Resource Server) 
  và giới hạn tần suất (Redis Rate Limiter).
- Database: Không sử dụng database riêng.

3.2. Ingestion & Normalization Service (Dịch vụ thu thập và chuẩn hóa)
- Tên Service: `ingestion-service`
- Nhiệm vụ: Kết nối đến Mosquitto Broker cổng 1883 để đón nhận dữ liệu thô. 
  Thực hiện phân tích, chuẩn hóa dữ liệu, ghi vào InfluxDB (`telemetry_raw`, cổng 8086) 
  và gửi sự kiện dữ liệu chuẩn hóa vào Kafka topic `telemetry-normalized`.
- Database: Không dùng DB quan hệ riêng. Kết nối trực tiếp đến `iot_telemetry_db` (InfluxDB).
- Cache hỗ trợ: Đọc danh sách cấu hình trạm/cảm biến hợp lệ từ Redis.

3.3. Authentication & User Service (Dịch vụ Xác thực và Người dùng)
- Tên Service: `auth-service` / `vti-keycloak`
- Nhiệm vụ: Sử dụng Keycloak 22.0.5 tích hợp sẵn làm IAM. Quản lý tài khoản, phân quyền 
  (ROLE_ADMIN, ROLE_OPERATOR, ROLE_VIEWER) và cấp phát token JWT.
- Database đi kèm: `keycloak_db` (Tự động khởi tạo trên mysql-vti).

3.4. Device & Sensor Management Service (Dịch vụ Quản lý Trạm và Thiết bị)
- Tên Service: `device-service` (Package: `com.example.deviceservice`)
- Nhiệm vụ: Quản lý danh mục các trạm (`stations`), gateway (`gateways`), loại cảm biến (`sensor_types`) 
  và các cảm biến vật lý cụ thể (`sensors`). Cung cấp các API CRUD đầy đủ và API lọc động 
  phân trang `/filter` thông qua JPA Specification. Hỗ trợ cơ chế xóa mềm & Restore.
- Database đi kèm: `iot_device_db` (Tự động khởi tạo trên mysql-vti).
- Cache hỗ trợ: Redis (Đồng bộ danh sách trạm/cảm biến hoạt động).

3.5. Alert & Notification Service (Dịch vụ Cảnh báo và Thông báo)
- Tên Service: `notification-service`
- Nhiệm vụ: Quản lý cấu hình ngưỡng báo động. Lắng nghe sự kiện từ Kafka topic `telemetry-normalized`, 
  so khớp ngưỡng (sử dụng cache Redis) để ghi nhận sự cố và trigger thông báo.
- Database đi kèm: `iot_alert_db` (Tự động khởi tạo trên mysql-vti).
- Cache hỗ trợ: Redis (Lưu cache các ngưỡng đo an toàn để so khớp tức thời).

3.6. Realtime Service (Dịch vụ Đẩy Dữ Liệu Thời Gian Thực)
- Tên Service: `realtime-service`
- Nhiệm vụ: Đảm nhiệm kết nối WebSocket / Server-Sent Events (SSE) với Web Portal (React). 
  Lắng nghe các sự kiện từ Kafka (AlertTriggeredEvent) và đẩy trực tiếp tín hiệu, số liệu 
  mới nhất lên màn hình của người dùng mà không cần tải lại trang.
- Database: Không sử dụng database riêng.

3.7. Data Integration Service (Dịch vụ Tích hợp và Xuất Báo cáo)
- Tên Service: `data-service`
- Nhiệm vụ: Truy vấn dữ liệu từ InfluxDB cổng 8086 để xuất báo cáo PDF/Excel chuỗi thời gian 
  bất đồng bộ (Async) khi có yêu cầu cụ thể từ người dùng (On-demand).
- Database đi kèm: `iot_report_db` (Tự động khởi tạo trên mysql-vti).


4. THIẾT KẾ CHI TIẾT CÁC CƠ SỞ DỮ LIỆU (DATABASE SCHEMA)
--------------------------------------------------------------------------------

4.1. DATABASE: keycloak_db / iot_auth_db (Hệ quản trị: MySQL 8.x)
- Ý nghĩa: Lưu trữ cấu hình phân quyền người dùng, client, session hoạt động và thông tin xác thực.
- Cấu trúc: Do Keycloak tự động quản lý và tạo bảng khi khởi chạy.

---

4.2. DATABASE: iot_device_db (Hệ quản trị: MySQL 8.x)
- Ý nghĩa: Lưu trữ siêu dữ liệu quản lý hạ tầng trạm, phân loại danh mục cảm biến, danh sách 
  các cảm biến vật lý được lắp đặt và cấu hình tham số kiểm soát trạng thái hoạt động. Tích hợp xóa mềm.

  * Bảng 1: `stations` (Thông tin trạm quan trắc)
  +--------------------+---------------+----------------------------------------------+
  | Tên trường         | Kiểu dữ liệu  | Giải thích                                   |
  +--------------------+---------------+----------------------------------------------+
  | id                 | VARCHAR(36)PK | Khóa chính UUID dạng String                  |
  | name               | VARCHAR(100)  | Tên trạm quan trắc (VD: Trạm Khí Thải A)     |
  | location           | TEXT          | Địa chỉ, vị trí lắp đặt                      |
  | latitude           | DOUBLE        | Tọa độ Vĩ độ                                 |
  | longitude          | DOUBLE        | Tọa độ Kinh độ                               |
  | status             | VARCHAR(20)   | Trạng thái trạm (ONLINE, OFFLINE)            |
  | heartbeat_interval | INT           | Khoảng thời gian tối đa cho phép mất kết nối |
  | is_deleted         | TINYINT(1)    | Cờ xác định trạng thái xóa mềm (1: Đã xóa)   |
  | created_at         | TIMESTAMP     | Thời gian khởi tạo trạm                      |
  | updated_at         | TIMESTAMP     | Thời gian cập nhật gần nhất                  |
  +--------------------+---------------+----------------------------------------------+

  * Bảng 2: `sensor_types` (Danh mục phân loại cảm biến)
  +--------------------+---------------+----------------------------------------------+
  | Tên trường         | Kiểu dữ liệu  | Giải thích                                   |
  +--------------------+---------------+----------------------------------------------+
  | id                 | VARCHAR(36)PK | Khóa chính UUID dạng String                  |
  | code               | VARCHAR(50)   | Mã loại cảm biến (VD: TEMP, PM25) - Duy nhất |
  | name               | VARCHAR(100)  | Tên hiển thị loại cảm biến                   |
  | default_unit       | VARCHAR(20)   | Đơn vị đo lường mặc định (VD: °C, mg/m3)     |
  | description        | TEXT          | Mô tả chi tiết loại cảm biến                 |
  | is_deleted         | TINYINT(1)    | Cờ xác định trạng thái xóa mềm (1: Đã xóa)   |
  | created_at         | TIMESTAMP     | Thời điểm tạo danh mục                       |
  +--------------------+---------------+----------------------------------------------+

  * Bảng 3: `gateways` (Bo mạch vi điều khiển trung tâm lắp tại trạm)
  +--------------------+---------------+----------------------------------------------+
  | Tên trường         | Kiểu dữ liệu  | Giải thích                                   |
  +--------------------+---------------+----------------------------------------------+
  | id                 | VARCHAR(36)PK | Khóa chính UUID dạng String                  |
  | station_id         | VARCHAR(36)FK | Khóa ngoại tham chiếu đến bảng stations(id)  |
  | gateway_code       | VARCHAR(100)  | Mã định danh Gateway duy nhất                |
  | serial_number      | VARCHAR(100)  | Số Serial của nhà sản xuất                   |
  | model              | VARCHAR(100)  | Model của Gateway                            |
  | firmware_version   | VARCHAR(100)  | Phiên bản Firmware                           |
  | ip_address         | VARCHAR(100)  | Địa chỉ IP (IPv4)                            |
  | mac_address        | VARCHAR(100)  | Địa chỉ MAC                                  |
  | status             | VARCHAR(50)   | Trạng thái (ONLINE, OFFLINE)                 |
  | last_seen          | TIMESTAMP     | Thời gian cuối cùng nhận tín hiệu            |
  | is_deleted         | TINYINT(1)    | Cờ xác định trạng thái xóa mềm (1: Đã xóa)   |
  | created_at         | TIMESTAMP     | Thời gian thêm vào hệ thống                  |
  +--------------------+---------------+----------------------------------------------+

  * Bảng 4: `sensors` (Danh sách cảm biến vật lý được cắm vào Gateway)
  +--------------------+---------------+----------------------------------------------+
  | Tên trường         | Kiểu dữ liệu  | Giải thích                                   |
  +--------------------+---------------+----------------------------------------------+
  | id                 | VARCHAR(36)PK | Khóa chính UUID dạng String                  |
  | gateway_id         | VARCHAR(36)FK | Khóa ngoại tham chiếu đến bảng gateways(id)  |
  | sensor_type_id     | VARCHAR(36)FK | Khóa ngoại tham chiếu đến sensor_types(id)   |
  | sensor_code        | VARCHAR(100)  | Mã cảm biến duy nhất                         |
  | name               | VARCHAR(255)  | Tên cụ thể cảm biến                          |
  | model              | VARCHAR(100)  | Model của cảm biến                           |
  | manufacturer       | VARCHAR(255)  | Nhà sản xuất                                 |
  | min_value          | DOUBLE        | Giá trị đo tối thiểu (cấu hình kỹ thuật)     |
  | max_value          | DOUBLE        | Giá trị đo tối đa (cấu hình kỹ thuật)        |
  | status             | VARCHAR(50)   | Trạng thái hoạt động (ONLINE, OFFLINE)       |
  | is_deleted         | TINYINT(1)    | Cờ xác định trạng thái xóa mềm (1: Đã xóa)   |
  | created_at         | TIMESTAMP     | Thời gian thêm thiết bị vào hệ thống         |
  +--------------------+---------------+----------------------------------------------+

---

4.3. DATABASE: iot_alert_db (Hệ quản trị: MySQL 8.x)
- Ý nghĩa: Lưu cấu hình ngưỡng an toàn áp dụng cho từng loại thiết bị, cấu hình các kênh 
  nhận tin nhắn đầu ra và lịch sử toàn bộ tiến trình xử lý sự cố.

  * Bảng 1: `alert_thresholds` (Cấu hình ngưỡng an toàn cho thiết bị)
  +--------------+---------------+----------------------------------------------+
  | Tên trường   | Kiểu dữ liệu  | Giải thích                                   |
  +--------------+---------------+----------------------------------------------+
  | id           | BIGINT (PK)   | Khóa chính định danh cấu hình ngưỡng         |
  | station_id   | VARCHAR(36)   | ID của trạm áp dụng cấu hình                 |
  | sensor_type  | VARCHAR(50)   | Loại cảm biến áp dụng ngưỡng (VD: TEMP)      |
  | min_value    | DOUBLE        | Ngưỡng dưới (Dưới mức này sẽ báo động)       |
  | max_value    | DOUBLE        | Ngưỡng trên (Vượt mức này sẽ báo động)       |
  | severity     | VARCHAR(20)   | Mức độ cảnh báo (WARNING, CRITICAL)          |
  | is_active    | TINYINT(1)    | Trạng thái kích hoạt của quy tắc             |
  | created_at   | TIMESTAMP     | Thời gian khởi tạo quy tắc                   |
  | updated_at   | TIMESTAMP     | Thời gian cập nhật quy tắc gần nhất          |
  +--------------+---------------+----------------------------------------------+

  * Bảng 2: `notification_channels` (Cấu hình đích nhận thông báo sự cố của từng trạm)
  +-------------------+---------------+----------------------------------------------+
  | Tên trường        | Kiểu dữ liệu  | Giải thích                                   |
  +-------------------+---------------+----------------------------------------------+
  | id                | BIGINT (PK)   | Khóa chính tự tăng                           |
  | station_id        | VARCHAR(36)   | ID của trạm áp dụng cấu hình kênh nhận       |
  | channel_type      | VARCHAR(20)   | Loại kênh (EMAIL, SMS, TELEGRAM)             |
  | recipient_address | VARCHAR(255)  | Địa chỉ nhận (Email, SĐT, hoặc Telegram ID)  |
  | is_active         | TINYINT(1)    | Kênh này có đang được bật hay không          |
  +-------------------+---------------+----------------------------------------------+

  * Bảng 3: `alert_logs` (Nhật ký lịch sử cảnh báo và quy trình xử lý)
  +-----------------+---------------+----------------------------------------------+
  | Tên trường      | Kiểu dữ liệu  | Giải thích                                   |
  +-----------------+---------------+----------------------------------------------+
  | id              | BIGINT (PK)   | Khóa chính định danh bản ghi log             |
  | station_id      | VARCHAR(36)   | ID của trạm xảy ra sự cố                     |
  | sensor_id       | VARCHAR(50)   | ID của cảm biến gây ra cảnh báo              |
  | metric_name     | VARCHAR(50)   | Tên thông số (VD: nhiệt độ)                  |
  | violated_value  | DOUBLE        | Giá trị thực tế đo được                      |
  | threshold_value | DOUBLE        | Giá trị ngưỡng bị vi phạm                    |
  | severity        | VARCHAR(20)   | Mức độ nghiêm trọng tại thời điểm đó         |
  | message         | TEXT          | Nội dung chi tiết cảnh báo                   |
  | triggered_at    | TIMESTAMP     | Thời điểm hệ thống kích hoạt cảnh báo        |
  | is_resolved     | TINYINT(1)    | Sự cố đã được xử lý xong chưa                |
  | resolved_at     | TIMESTAMP     | Thời điểm xử lý xong sự cố                   |
  | resolved_by     | BIGINT        | ID của người vận hành xử lý                  |
  | resolution_note | TEXT          | Ghi chú chi tiết phương án xử lý sự cố       |
  +-----------------+---------------+----------------------------------------------+

---

4.4. DATABASE: iot_report_db (Hệ quản trị: MySQL 8.x)
- Ý nghĩa: Lưu vết các yêu cầu xuất báo cáo dữ liệu lịch sử theo nhu cầu thực tế (On-demand).

  * Bảng 1: `report_export_tasks` (Nhiệm vụ xuất báo cáo dữ liệu)
  +-----------------+---------------+----------------------------------------------+
  | Tên trường      | Kiểu dữ liệu  | Giải thích                                   |
  +-----------------+---------------+----------------------------------------------+
  | id              | BIGINT (PK)   | Khóa chính tự tăng                           |
  | requester_id    | BIGINT        | ID người yêu cầu xuất                        |
  | station_id      | VARCHAR(36)   | ID trạm cần trích xuất dữ liệu               |
  | report_type     | VARCHAR(20)   | Loại báo cáo (DAY, WEEK, MONTH)              |
  | start_time      | TIMESTAMP     | Thời điểm bắt đầu của khoảng dữ liệu cần xuất|
  | end_time        | TIMESTAMP     | Thời điểm kết thúc của khoảng dữ liệu cần xuất|
  | status          | VARCHAR(20)   | Trạng thái (PENDING, RUNNING, COMPLETED, FAILED)|
  | file_url        | VARCHAR(512)  | Đường dẫn tải file báo cáo từ MinIO/S3       |
  | error_message   | TEXT          | Mô tả lỗi nếu xuất báo cáo thất bại          |
  | requested_at    | TIMESTAMP     | Thời điểm người dùng yêu cầu                 |
  | completed_at    | TIMESTAMP     | Thời điểm tiến trình xuất hoàn tất           |
  +-----------------+---------------+----------------------------------------------+


5. CHI TIẾT LUỒNG DỮ LIỆU TÍCH HỢP (DATA FLOW)
--------------------------------------------------------------------------------
[Luồng 1: Thu thập, Chuẩn hóa và Lưu trữ Realtime Telemetry]
1. Thiết bị cảm biến định kỳ gửi dữ liệu đo đạc (JSON) lên Mosquitto Broker (1883).
2. `ingestion-service` nhận bản tin -> Đọc nhanh Redis Cache để xác thực cảm biến và trạm.
3. Sau khi xác thực hợp lệ, `ingestion-service` chuẩn hóa dữ liệu, ghi vào Bucket `telemetry_raw` 
   của InfluxDB (8086), đồng thời publish một Event `TelemetryNormalizedEvent` vào Kafka topic 
   `telemetry-normalized` (Listener cổng 29092 nội bộ).
4. Grafana kết nối trực tiếp vào InfluxDB truy vấn dữ liệu theo thời gian thực để vẽ biểu đồ.

[Luồng 2: Xử lý Cảnh báo bất đồng bộ qua Kafka]
1. `alert-service` lắng nghe liên tục sự kiện đo đạc chuẩn hóa từ Kafka topic `telemetry-normalized`.
2. Khi nhận thông tin đo đạc, `alert-service` lấy dữ liệu ra và so khớp với danh sách ngưỡng 
   cấu hình an toàn tương ứng (đang được lưu đệm trong Redis để tránh query liên tục vào MySQL).
3. Nếu phát hiện giá trị đo đạc vượt ngưỡng an toàn:
   - Ghi nhận bản ghi sự cố mới vào bảng `alert_logs` (Database: `iot_alert_db`).
   - Publish một Event `AlertTriggeredEvent` vào Kafka topic `alert-notifications`.
4. Các Worker Notification lắng nghe topic `alert-notifications` để gửi thông báo 
   đến Telegram/Email của kỹ thuật viên dựa trên cấu hình trong bảng `notification_channels`.

[Luồng 3: Quản lý và Đồng bộ Metadata (EDA)]
1. Khi có sự thay đổi cấu hình trạm, loại cảm biến, hoặc thiết bị trên Web Portal, 
   `device-service` cập nhật vào `iot_device_db` và thực hiện xóa mềm nếu cần.
2. `device-service` phát đi một sự kiện `StationMetadataChangedEvent` vào Kafka topic `metadata-sync`.
3. `ingestion-service` và `alert-service` cùng lắng nghe topic này để tự động làm mới (invalidate) 
   bộ nhớ đệm Redis liên quan, đảm bảo việc xác thực và kiểm tra ngưỡng luôn chạy trên thông tin mới nhất.

[Luồng 4: Xuất báo cáo dữ liệu lịch sử theo nhu cầu (On-Demand Asynchronous Reporting)]
1. Người dùng truy cập Web Portal, cấu hình thời gian và chủ động nhấn nút yêu cầu xuất báo cáo.
2. Web Portal gửi yêu cầu đến `data-service`. Dịch vụ này ngay lập tức tạo một bản ghi nhiệm vụ 
   trong bảng `report_export_tasks` với trạng thái `PENDING` và trả về một mã `task_id` cho Frontend.
3. `data-service` đẩy thông điệp yêu cầu xuất báo cáo vào Kafka topic `report-export-requests`.
4. Worker thuộc `data-service` lắng nghe topic này, chuyển trạng thái task sang `RUNNING` 
   và thực hiện truy vấn InfluxDB Bucket `telemetry_aggregated` để trích xuất dữ liệu lịch sử.
5. Worker sinh file báo cáo (Excel/PDF), tải tệp tin lên hệ thống Object Storage (MinIO/S3), 
   cập nhật trạng thái task sang `COMPLETED` cùng link tải `file_url` trong database `iot_report_db`.
6. Hệ thống gửi thông báo WebSocket về cho trình duyệt của người dùng để hiển thị nút tải file.


6. CÔNG NGHỆ VÀ MÔI TRƯỜNG TRIỂN KHAI
--------------------------------------------------------------------------------
- Hạ tầng Backend: Java 17/21, Spring Boot 3.2.x, Spring Cloud (Gateway, Eureka).
- Message Broker & Hàng đợi xương sống (EDA): Apache Kafka (wurstmeister/kafka: Cổng 9092).
- Bộ nhớ đệm (Caching): Redis (Cổng 6379).
- MQTT Broker: Eclipse Mosquitto (Cho phép anonymous, Cổng 1883).
- Lưu trữ Tệp tin: MinIO hoặc AWS S3 (cho các tệp báo cáo On-demand, Cổng 9000).
- Bảo mật API: Spring Security + JWT, Keycloak 22.0.5 (Cổng 8082).
- Môi trường chạy lập trình: Các dịch vụ Spring Boot chạy trực tiếp trên máy vật lý 
  (thông qua IntelliJ), kết nối vào cơ sở hạ tầng được ảo hóa hoàn toàn bằng Docker Compose.

================================================================================
                             HẾT TÀI LIỆU
================================================================================