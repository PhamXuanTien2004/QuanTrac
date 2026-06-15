================================================================================
          DỰ ÁN: HỆ THỐNG QUAN TRẮC VÀ THEO DÕI DỮ LIỆU TỪ XA THEO THỜI GIAN THỰC
                                  (BẢN CẬP NHẬT ĐẦY ĐỦ - V4)
================================================================================

1. TỔNG QUAN DỰ ÁN
--------------------------------------------------------------------------------
Dự án xây dựng một hệ thống Web Service nhằm thu thập, xử lý, giám sát và hiển thị 
dữ liệu từ các trạm quan trắc từ xa theo thời gian thực. Hệ thống sử dụng kiến trúc 
Microservices (Java Spring Boot) kết hợp mô hình Kiến trúc hướng sự kiện (EDA) qua 
Apache Kafka để đảm bảo tính mở rộng, bảo mật và khả năng chịu tải cao trong môi 
trường doanh nghiệp.

2. KIẾN TRÚC HỆ THỐNG TỔNG THỂ
--------------------------------------------------------------------------------
Hệ thống được chia thành 4 lớp (Layers) chính:

2.1. Lớp Thu thập dữ liệu (Edge / Device Layer)
- Thiết bị: Các cảm biến tại trạm quan trắc (đo nhiệt độ, độ ẩm, chất lượng không khí, v.v.).
- Giao thức: Truyền tải dữ liệu qua MQTTS (MQTT bảo mật, Port 8883).
- Broker: EMQX hoặc HiveMQ hỗ trợ clustering tốt cho hàng triệu kết nối đồng thời. 
  Bảo mật qua mTLS (chứng chỉ Client/Server) hoặc API Webhook liên kết với auth-service.

2.2. Lớp Xử lý và Lưu trữ dữ liệu (Data Pipeline Layer)
- Ingestion Service: Một dịch vụ stateless viết bằng Java Spring Boot (hoặc Go) chuyên biệt. 
  Dịch vụ này kết nối trực tiếp đến MQTT Broker để tiêu thụ dữ liệu thô, xác thực 
  thiết bị qua Redis, chuẩn hóa định dạng (JSON) và đẩy sự kiện vào hệ thống Kafka.
- Message Broker: Apache Kafka đóng vai trò xương sống cho kiến trúc hướng sự kiện (EDA). 
  Dữ liệu được phân chia vào các Topic rõ ràng (VD: telemetry-raw, telemetry-normalized).
- Time-Series Database: InfluxDB lưu trữ dữ liệu telemetry thông qua 02 Bucket:
  + `telemetry_raw`: Lưu dữ liệu thô chi tiết trong vòng 30 ngày.
  + `telemetry_aggregated`: Lưu dữ liệu đã giảm mẫu (Downsampled) trung bình theo giờ/ngày 
    để phục vụ truy vấn lịch sử dài hạn (365 ngày).

2.3. Lớp Quản lý Nghiệp vụ (Backend Microservices - Java Spring Boot)
Sử dụng Spring Cloud (Eureka Server, Spring Cloud Gateway) để điều phối hệ thống.
- Database per Service: Mỗi service quản lý một cơ sở dữ liệu quan hệ riêng (PostgreSQL/MySQL).
- Caching Layer: Redis lưu trữ dữ liệu metadata tĩnh, cấu hình trạm và token người dùng 
  để tránh truy vấn trực tiếp vào PostgreSQL/MySQL liên tục.

2.4. Lớp Hiển thị (Presentation / Frontend Layer)
- Grafana Dashboard: Nhúng (embed) vào giao diện web hoặc xem trực tiếp qua iframe. 
  Truy vấn trực tiếp từ InfluxDB.
- Web Portal (React/Angular): Giao tiếp qua Gateway để quản lý người dùng, cấu hình trạm, 
  thiết lập ngưỡng cảnh báo và gửi yêu cầu xuất báo cáo khi cần.


3. CHI TIẾT CÁC MICROSERVICES (TÊN, NHIỆM VỤ, DATABASE)
--------------------------------------------------------------------------------

3.1. API Gateway & Discovery Server (Thành phần hạ tầng)
- Tên Service: `gateway-service` & `discovery-service`
- Nhiệm vụ: Định tuyến request, cân bằng tải, xác thực tập trung (JWT) và giới hạn tần suất.
- Database: Không sử dụng database riêng.

3.2. Ingestion & Normalization Service (Dịch vụ thu thập và chuẩn hóa)
- Tên Service: `ingestion-service`
- Nhiệm vụ: Kết nối đến MQTT Broker để đón nhận dữ liệu thô từ các trạm quan trắc. 
  Thực hiện phân tích cú pháp (parse), chuẩn hóa dữ liệu, ghi vào InfluxDB (`telemetry_raw`) 
  và gửi sự kiện dữ liệu chuẩn hóa vào Apache Kafka topic `telemetry-normalized`.
- Database: Không dùng DB quan hệ riêng. Kết nối trực tiếp đến `iot_telemetry_db` (InfluxDB).
- Cache hỗ trợ: Đọc danh sách cấu hình trạm/thiết bị hợp lệ từ Redis để xác thực nhanh bản tin.

3.3. Authentication & User Service (Dịch vụ Xác thực và Người dùng)
- Tên Service: `auth-service`
- Nhiệm vụ: Quản lý tài khoản, phân quyền, cấp phát token JWT và quản lý API Key/Client Credentials 
  cho các dịch vụ/bên thứ ba tích hợp.
- Database đi kèm: `iot_auth_db`
- Cache hỗ trợ: Redis (Lưu token blacklist, thông tin xác thực để tăng tốc kiểm tra quyền).

3.4. Station & Device Management Service (Dịch vụ Quản lý Trạm và Thiết bị)
- Tên Service: `station-service`
- Nhiệm vụ: Quản lý danh mục các trạm, các loại cảm biến và các thiết bị cụ thể. 
  Cung cấp API cho người dùng quản lý và phát đi sự kiện đồng bộ metadata qua Kafka 
  mỗi khi có trạm/thiết bị được thêm, sửa, xóa hoặc thay đổi trạng thái hoạt động.
- Database đi kèm: `iot_station_db` (Lưu thông tin trạm, loại cảm biến, thiết bị và lịch sử kết nối).
- Cache hỗ trợ: Redis (Đồng bộ danh sách trạm/thiết bị hoạt động để chia sẻ cho `ingestion-service`).

3.5. Alert & Notification Service (Dịch vụ Cảnh báo và Thông báo)
- Tên Service: `alert-service`
- Nhiệm vụ: Quản lý cấu hình ngưỡng báo động và kênh nhận tin. Lắng nghe sự kiện từ Kafka 
  topic `telemetry-normalized`, so khớp ngưỡng (sử dụng cache Redis) để ghi nhận sự cố 
  và trigger thông báo qua Email/SMS/Telegram.
- Database đi kèm: `iot_alert_db` (Lưu cấu hình ngưỡng, kênh liên lạc và log sự cố).
- Cache hỗ trợ: Redis (Lưu cache các ngưỡng đo an toàn để so khớp tức thời với dữ liệu từ Kafka).

3.6. Data Integration Service (Dịch vụ Tích hợp và Xuất Báo cáo)
- Tên Service: `data-service`
- Nhiệm vụ: Chỉ thực hiện truy vấn dữ liệu từ InfluxDB để xuất báo cáo PDF/Excel (ngày, tuần, tháng) 
  bất đồng bộ (Async) khi có yêu cầu cụ thể từ người dùng (On-demand). Tệp tin báo cáo được 
  tải lên Object Storage (MinIO/S3) để cung cấp link tải an toàn.
- Database đi kèm: Lưu trạng thái xử lý của các yêu cầu xuất báo cáo trong `iot_report_db`.


4. THIẾT KẾ CHI TIẾT CÁC CƠ SỞ DỮ LIỆU (DATABASE SCHEMA)
--------------------------------------------------------------------------------

4.1. DATABASE: iot_auth_db (Hệ quản trị: PostgreSQL/MySQL)
- Ý nghĩa: Lưu trữ dữ liệu xác thực người dùng và khóa tích hợp của các ứng dụng ngoại vi.

  * Bảng 1: `users` (Thông tin người dùng)
  +--------------+---------------+----------------------------------------------+
  | Tên trường   | Kiểu dữ liệu  | Giải thích                                   |
  +--------------+---------------+----------------------------------------------+
  | id           | BIGINT (PK)   | Khóa chính định danh người dùng (Tự tăng)    |
  | username     | VARCHAR(50)   | Tên đăng nhập (Duy nhất, không trống)        |
  | password     | VARCHAR(255)  | Mật khẩu đã được mã hóa BCrypt               |
  | email        | VARCHAR(100)  | Địa chỉ email nhận thông báo và khôi phục mã |
  | full_name    | VARCHAR(100)  | Họ và tên đầy đủ                             |
  | status       | INT           | Trạng thái tài khoản (1: Hoạt động, 0: Khóa) |
  | created_at   | TIMESTAMP     | Thời gian tạo tài khoản                      |
  | updated_at   | TIMESTAMP     | Thời gian cập nhật gần nhất                  |
  +--------------+---------------+----------------------------------------------+

  * Bảng 2: `roles` (Danh mục vai trò/quuyền)
  +--------------+---------------+----------------------------------------------+
  | Tên trường   | Kiểu dữ liệu  | Giải thích                                   |
  +--------------+---------------+----------------------------------------------+
  | id           | BIGINT (PK)   | Khóa chính định danh quyền (Tự tăng)         |
  | name         | VARCHAR(50)   | Tên quyền (VD: ROLE_ADMIN, ROLE_OPERATOR)    |
  | description  | VARCHAR(255)  | Mô tả chi tiết chức năng của quyền           |
  +--------------+---------------+----------------------------------------------+

  * Bảng 3: `user_roles` (Bảng trung gian liên kết Người dùng - Quyền)
  +--------------+---------------+----------------------------------------------+
  | Tên trường   | Kiểu dữ liệu  | Giải thích                                   |
  +--------------+---------------+----------------------------------------------+
  | user_id      | BIGINT (FK)   | Khóa ngoại tham chiếu đến bảng users(id)     |
  | role_id      | BIGINT (FK)   | Khóa ngoại tham chiếu đến bảng roles(id)     |
  +--------------+---------------+----------------------------------------------+

  * Bảng 4: `client_credentials` (Quản lý API Key cho các dịch vụ ngoại vi)
  +--------------+---------------+----------------------------------------------+
  | Tên trường   | Kiểu dữ liệu  | Giải thích                                   |
  +--------------+---------------+----------------------------------------------+
  | id           | BIGINT (PK)   | Khóa chính tự tăng                           |
  | client_id    | VARCHAR(100)  | Mã định danh ứng dụng gọi API                |
  | client_secret| VARCHAR(255)  | Khóa bí mật ứng dụng (Mã hóa một chiều)      |
  | app_name     | VARCHAR(100)  | Tên ứng dụng được phép tích hợp              |
  | scopes       | VARCHAR(255)  | Phạm vi quyền hạn (VD: read:telemetry)       |
  | is_active    | BOOLEAN       | Khóa có đang hoạt động hay không             |
  | created_at   | TIMESTAMP     | Ngày khởi tạo khóa                           |
  | expires_at   | TIMESTAMP     | Ngày hết hạn của khóa                        |
  +--------------+---------------+----------------------------------------------+

---

4.2. DATABASE: iot_station_db (Hệ quản trị: PostgreSQL có hỗ trợ PostGIS)
- Ý nghĩa: Lưu trữ siêu dữ liệu quản lý hạ tầng trạm, phân loại danh mục cảm biến, danh sách 
  các cảm biến vật lý được lắp đặt và cấu hình tham số kiểm soát trạng thái hoạt động.

  * Bảng 1: `stations` (Thông tin trạm quan trắc)
  +--------------------+---------------+----------------------------------------------+
  | Tên trường         | Kiểu dữ liệu  | Giải thích                                   |
  +--------------------+---------------+----------------------------------------------+
  | id                 | BIGINT (PK)   | Khóa chính định danh trạm (Tự tăng)          |
  | name               | VARCHAR(100)  | Tên trạm quan trắc (VD: Trạm Khí Thải A)     |
  | location           | TEXT          | Địa chỉ, vị trí lắp đặt                      |
  | latitude           | DOUBLE        | Tọa độ Vĩ độ (Phục vụ hiển thị trên Bản đồ)  |
  | longitude          | DOUBLE        | Tọa độ Kinh độ (Phục vụ hiển thị trên Bản đồ)|
  | status             | VARCHAR(20)   | Trạng thái trạm (ONLINE, OFFLINE, MAINTENANCE)|
  | heartbeat_interval | INT           | Khoảng thời gian tối đa cho phép giữa 2 lần  |
  |                    |               | gửi dữ liệu trước khi coi là OFFLINE (giây)  |
  | created_at         | TIMESTAMP     | Thời gian khởi tạo trạm trên hệ thống        |
  | updated_at         | TIMESTAMP     | Thời gian cập nhật thông tin trạm            |
  +--------------------+---------------+----------------------------------------------+

  * Bảng 2: `device_types` (Danh mục phân loại cảm biến - MỚI BỔ SUNG)
  +--------------------+---------------+----------------------------------------------+
  | Tên trường         | Kiểu dữ liệu  | Giải thích                                   |
  +--------------------+---------------+----------------------------------------------+
  | id                 | BIGINT (PK)   | Khóa chính tự tăng                           |
  | code               | VARCHAR(50)   | Mã loại cảm biến (VD: TEMP, PM25, CO2) - Duy nhất|
  | name               | VARCHAR(100)  | Tên hiển thị loại cảm biến (VD: Đo nhiệt độ) |
  | default_unit       | VARCHAR(20)   | Đơn vị đo lường mặc định (VD: °C, mg/m3)     |
  | description        | TEXT          | Mô tả chức năng và dải đo an toàn lý thuyết  |
  | created_at         | TIMESTAMP     | Thời điểm tạo danh mục                       |
  +--------------------+---------------+----------------------------------------------+

  * Bảng 3: `devices` (Danh sách cảm biến/thiết bị vật lý lắp đặt tại trạm)
  +--------------------+---------------+----------------------------------------------+
  | Tên trường         | Kiểu dữ liệu  | Giải thích                                   |
  +--------------------+---------------+----------------------------------------------+
  | id                 | VARCHAR(50)PK | Khóa chính, thường là chuỗi MAC hoặc UUID    |
  | station_id         | BIGINT (FK)   | Khóa ngoại tham chiếu đến bảng stations(id)  |
  | device_type_id     | BIGINT (FK)   | Khóa ngoại tham chiếu đến device_types(id)   |
  | name               | VARCHAR(100)  | Tên cụ thể cảm biến (VD: Cảm biến Đo PM2.5 #1)|
  | unit_override      | VARCHAR(20)   | Đơn vị đo tùy chỉnh (Nếu khác default_unit)  |
  | is_active          | BOOLEAN       | Thiết bị đang bật hay tắt (True/False)       |
  | last_seen          | TIMESTAMP     | Thời gian cuối cùng thiết bị gửi tín hiệu    |
  | created_at         | TIMESTAMP     | Thời gian thêm thiết bị vào hệ thống         |
  +--------------------+---------------+----------------------------------------------+

  * Bảng 4: `station_connection_logs` (Nhật ký trạng thái kết nối trạm)
  +--------------+---------------+----------------------------------------------+
  | Tên trường   | Kiểu dữ liệu  | Giải thích                                   |
  +--------------+---------------+----------------------------------------------+
  | id           | BIGINT (PK)   | Khóa chính tự tăng                           |
  | station_id   | BIGINT        | ID của trạm thay đổi trạng thái              |
  | old_status   | VARCHAR(20)   | Trạng thái cũ trước khi thay đổi             |
  | new_status   | VARCHAR(20)   | Trạng thái mới được cập nhật                 |
  | changed_at   | TIMESTAMP     | Thời điểm ghi nhận sự thay đổi trạng thái    |
  | reason       | VARCHAR(255)  | Lý do thay đổi trạng thái                    |
  +--------------+---------------+----------------------------------------------+

---

4.3. DATABASE: iot_alert_db (Hệ quản trị: PostgreSQL/MySQL)
- Ý nghĩa: Lưu cấu hình ngưỡng an toàn áp dụng cho từng loại thiết bị, thông tin các kênh 
  nhận tin nhắn đầu ra và lịch sử toàn bộ tiến trình xử lý sự cố.

  * Bảng 1: `alert_thresholds` (Cấu hình ngưỡng an toàn cho thiết bị)
  +--------------+---------------+----------------------------------------------+
  | Tên trường   | Kiểu dữ liệu  | Giải thích                                   |
  +--------------+---------------+----------------------------------------------+
  | id           | BIGINT (PK)   | Khóa chính định danh cấu hình ngưỡng         |
  | station_id   | BIGINT        | ID của trạm áp dụng cấu hình (Logical FK)    |
  | device_type  | VARCHAR(50)   | Loại cảm biến áp dụng ngưỡng (VD: TEMP)      |
  | min_value    | DOUBLE        | Ngưỡng dưới (Dưới mức này sẽ báo động)       |
  | max_value    | DOUBLE        | Ngưỡng trên (Vượt mức này sẽ báo động)       |
  | severity     | VARCHAR(20)   | Mức độ cảnh báo (WARNING, CRITICAL)          |
  | is_active    | BOOLEAN       | Trạng thái kích hoạt của quy tắc cấu hình    |
  | created_at   | TIMESTAMP     | Thời gian khởi tạo quy tắc                   |
  | updated_at   | TIMESTAMP     | Thời gian cập nhật quy tắc gần nhất          |
  +--------------+---------------+----------------------------------------------+

  * Bảng 2: `notification_channels` (Cấu hình đích nhận thông báo sự cố của từng trạm)
  +-------------------+---------------+----------------------------------------------+
  | Tên trường        | Kiểu dữ liệu  | Giải thích                                   |
  +-------------------+---------------+----------------------------------------------+
  | id                | BIGINT (PK)   | Khóa chính tự tăng                           |
  | station_id        | BIGINT        | ID của trạm áp dụng cấu hình kênh nhận       |
  | channel_type      | VARCHAR(20)   | Loại kênh (EMAIL, SMS, TELEGRAM)             |
  | recipient_address | VARCHAR(255)  | Địa chỉ nhận (Email, SĐT, hoặc Telegram Chat |
  |                   |               | ID)                                          |
  | is_active         | BOOLEAN       | Kênh này có đang được bật hay không          |
  +-------------------+---------------+----------------------------------------------+

  * Bảng 3: `alert_logs` (Nhật ký lịch sử cảnh báo và quy trình xử lý)
  +-----------------+---------------+----------------------------------------------+
  | Tên trường      | Kiểu dữ liệu  | Giải thích                                   |
  +-----------------+---------------+----------------------------------------------+
  | id              | BIGINT (PK)   | Khóa chính định danh bản ghi log             |
  | station_id      | BIGINT        | ID của trạm xảy ra sự cố                     |
  | device_id       | VARCHAR(50)   | ID của thiết bị gây ra cảnh báo              |
  | metric_name     | VARCHAR(50)   | Tên thông số (VD: nhiệt độ)                  |
  | violated_value  | DOUBLE        | Giá trị thực tế đo được lúc xảy ra sự cố     |
  | threshold_value | DOUBLE        | Giá trị ngưỡng bị vi phạm                    |
  | severity        | VARCHAR(20)   | Mức độ nghiêm trọng tại thời điểm đó         |
  | message         | TEXT          | Nội dung chi tiết cảnh báo                   |
  | triggered_at    | TIMESTAMP     | Thời điểm hệ thống kích hoạt cảnh báo        |
  | is_resolved     | BOOLEAN       | Sự cố đã được xử lý xong chưa (True/False)   |
  | resolved_at     | TIMESTAMP     | Thời điểm xử lý xong sự cố                   |
  | resolved_by     | BIGINT        | ID của người vận hành xử lý (từ iot_auth_db) |
  | resolution_note | TEXT          | Ghi chú chi tiết phương án xử lý sự cố       |
  +-----------------+---------------+----------------------------------------------+

---

4.4. DATABASE: iot_report_db (Hệ quản trị: PostgreSQL/MySQL - MỚI BỔ SUNG)
- Ý nghĩa: Lưu vết các yêu cầu xuất báo cáo dữ liệu lịch sử theo nhu cầu thực tế 
  (On-demand) của người vận hành để quản lý trạng thái tải tệp tin và lưu trữ tạm thời.

  * Bảng 1: `report_export_tasks` (Nhiệm vụ xuất báo cáo dữ liệu)
  +-----------------+---------------+----------------------------------------------+
  | Tên trường      | Kiểu dữ liệu  | Giải thích                                   |
  +-----------------+---------------+----------------------------------------------+
  | id              | BIGINT (PK)   | Khóa chính tự tăng                           |
  | requester_id    | BIGINT        | ID người yêu cầu xuất (từ iot_auth_db)       |
  | station_id      | BIGINT        | ID trạm cần trích xuất dữ liệu               |
  | report_type     | VARCHAR(20)   | Loại báo cáo (DAY, WEEK, MONTH)              |
  | start_time      | TIMESTAMP     | Thời điểm bắt đầu của khoảng dữ liệu cần xuất|
  | end_time        | TIMESTAMP     | Thời điểm kết thúc của khoảng dữ liệu cần xuất|
  | status          | VARCHAR(20)   | Trạng thái (PENDING, RUNNING, COMPLETED, FAILED)|
  | file_url        | VARCHAR(512)  | Đường dẫn tải file báo cáo từ MinIO/S3       |
  | error_message   | TEXT          | Mô tả lỗi nếu xuất báo cáo thất bại          |
  | requested_at    | TIMESTAMP     | Thời điểm người dùng yêu cầu                 |
  | completed_at    | TIMESTAMP     | Thời điểm tiến trình xuất hoàn tất           |
  +-----------------+---------------+----------------------------------------------+

---

4.5. DATABASE: iot_telemetry_db (Hệ quản trị: InfluxDB - Time-Series DB)
- Ý nghĩa: Cơ sở dữ liệu chuỗi thời gian phân cấp để vừa đảm bảo tốc độ ghi dữ liệu thô, 
  vừa tối ưu hóa hiệu năng truy vấn biểu đồ lịch sử lâu dài.

  * Thiết kế Bucket & Retention Policy:
    1. Bucket `telemetry_raw`: Lưu dữ liệu thô chi tiết từ trạm gửi về. Retention: 30 ngày.
    2. Bucket `telemetry_aggregated`: Lưu dữ liệu tổng hợp trung bình theo giờ. Retention: 365 ngày.

  * Cấu trúc Measurement: `telemetry_reading`
  +-----------------+---------------+---------------+----------------------------------+
  | Tên thành phần  | Tên trường    | Kiểu dữ liệu  | Giải thích                       |
  +-----------------+---------------+---------------+----------------------------------+
  | TAG (Indexed)   | station_id    | String        | Mã trạm để lọc nhanh dữ liệu     |
  | TAG (Indexed)   | device_id     | String        | Mã thiết bị cảm biến cụ thể      |
  | TAG (Indexed)   | device_type   | String        | Loại thông số (TEMP, PM25...)    |
  | FIELD (Value)   | value         | Float/Double  | Giá trị đo đạc thực tế           |
  | FIELD (Value)   | status_code   | Integer       | Mã trạng thái dữ liệu (VD: 200)  |
  | TIMESTAMP       | time          | Int64 (Nano)  | Thời gian hệ thống ghi nhận bản  |
  +-----------------+---------------+---------------+----------------------------------+


5. CHI TIẾT LUỒNG DỮ LIỆU TÍCH HỢP (DATA FLOW)
--------------------------------------------------------------------------------
[Luồng 1: Thu thập, Chuẩn hóa và Lưu trữ Realtime Telemetry]
1. Thiết bị cảm biến định kỳ gửi dữ liệu đo đạc (JSON) lên EMQX Broker qua cổng MQTTS (8883).
2. `ingestion-service` subscribe EMQX Broker -> Nhận bản tin -> Đọc nhanh Redis Cache 
   để xác thực thiết bị và trạm (nếu không có trong cache mới gọi truy vấn `station-service`).
3. Sau khi xác thực hợp lệ, `ingestion-service` thực hiện chuẩn hóa dữ liệu, ghi bản tin 
   vào Bucket `telemetry_raw` của InfluxDB, đồng thời publish một Event `TelemetryNormalizedEvent` 
   vào Kafka topic `telemetry-normalized`.
4. Grafana kết nối trực tiếp vào InfluxDB truy vấn dữ liệu theo thời gian thực để cập nhật 
   biểu đồ trên Dashboard.

[Luồng 2: Xử lý Cảnh báo bất đồng bộ qua Kafka]
1. `alert-service` lắng nghe liên tục sự kiện đo đạc chuẩn hóa từ Kafka topic `telemetry-normalized`.
2. Khi nhận thông tin đo đạc, `alert-service` lấy dữ liệu ra và so khớp với danh sách ngưỡng 
   cấu hình an toàn tương ứng (đang được lưu đệm trong Redis để tránh query liên tục vào Postgres).
3. Nếu phát hiện giá trị đo đạc vượt ngưỡng an toàn:
   - Ghi nhận bản ghi sự cố mới vào bảng `alert_logs` (Database: `iot_alert_db`).
   - Publish một Event `AlertTriggeredEvent` vào Kafka topic `alert-notifications`.
4. Các Worker Notification lắng nghe topic `alert-notifications` để gửi thông báo 
   đến Telegram/Email của kỹ thuật viên dựa trên cấu hình trong bảng `notification_channels`.

[Luồng 3: Quản lý và Đồng bộ Metadata (EDA)]
1. Khi có sự thay đổi cấu hình trạm, loại cảm biến, hoặc thiết bị trên Web Portal, 
   `station-service` cập nhật vào `iot_station_db`.
2. `station-service` phát đi một sự kiện `StationMetadataChangedEvent` vào Kafka topic `metadata-sync`.
3. `ingestion-service` và `alert-service` cùng lắng nghe topic này để tự động làm mới (invalidate) 
   bộ nhớ đệm Redis liên quan, đảm bảo việc xác thực và kiểm tra ngưỡng luôn chạy trên thông tin mới nhất.

[Luồng 4: Xuất báo cáo dữ liệu lịch sử theo nhu cầu (On-Demand Asynchronous Reporting)]
1. Người dùng truy cập Web Portal, cấu hình thời gian và nhấn nút yêu cầu xuất báo cáo.
2. Web Portal gửi yêu cầu đến `data-service`. Dịch vụ này ngay lập tức tạo một bản ghi nhiệm vụ 
   trong bảng `report_export_tasks` với trạng thái `PENDING` và trả về một mã `task_id` cho Frontend.
3. `data-service` đẩy thông điệp yêu cầu xuất báo cáo vào Kafka topic `report-export-requests`.
4. Worker thuộc `data-service` lắng nghe topic này, chuyển trạng thái task sang `RUNNING` 
   và thực hiện truy vấn InfluxDB Bucket `telemetry_aggregated` để trích xuất dữ liệu lịch sử.
5. Worker sinh file báo cáo (Excel/PDF), tải tệp tin lên hệ thống Object Storage (MinIO/S3), 
   cập nhật trạng thái task sang `COMPLETED` cùng link tải `file_url` trong database `iot_report_db`.
6. Hệ thống gửi thông báo WebSocket về cho trình duyệt của người dùng. Giao diện Web Portal hiển thị 
   nút "Tải file" tương ứng với `task_id` để người dùng chủ động tải báo cáo về khi rảnh.


6. CÔNG NGHỆ VÀ MÔI TRƯỜNG TRIỂN KHAI
--------------------------------------------------------------------------------
- Hạ tầng Backend: Java 17/21, Spring Boot 3.x, Spring Cloud (Gateway, Eureka).
- Message Broker & Hàng đợi xương sống (EDA): Apache Kafka.
- Bộ nhớ đệm (Caching): Redis.
- MQTT Broker: EMQX hoặc HiveMQ bảo mật mTLS (Cổng 8883).
- Lưu trữ Tệp tin: MinIO hoặc AWS S3 (cho các tệp báo cáo On-demand).
- Bảo mật API: Spring Security + JWT, hỗ trợ Client Credentials cho các dịch vụ nền.
- Triển khai & Vận hành: Đóng gói Docker Container cho từng Microservice. 
  Sử dụng Docker Compose hoặc Kubernetes (K8s) để quản lý điều phối trên máy chủ Linux, 
  giúp tối ưu hóa tài nguyên phần cứng, tự động phục hồi và dễ dàng mở rộng độc lập.

================================================================================
                             HẾT TÀI LIỆU
================================================================================