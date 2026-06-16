-- Khởi tạo các Database độc lập cho từng dịch vụ và Keycloak
CREATE DATABASE IF NOT EXISTS `iot_auth_db` CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS `iot_station_db` CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS `iot_alert_db` CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS `iot_report_db` CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS `keycloak_db` CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;