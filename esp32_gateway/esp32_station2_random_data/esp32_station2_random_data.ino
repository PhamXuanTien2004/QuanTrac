#include <WiFi.h>
#include <PubSubClient.h>
#include <ArduinoJson.h>
#include <time.h>
#include <ModbusMaster.h>
#include "PMS.h"
// ==========================================
// CẤU HÌNH WIFI
// ==========================================
const char* ssid = "Ptx";
const char* password = "01042004";

// ==========================================
// CẤU HÌNH KẾT NỐI MQTT (Station 02)
// ==========================================
const char* mqtt_server = "172.20.10.4"; // Thay bằng IP của Broker (không dùng localhost trên ESP32)
const int mqtt_port = 1883;
const char* mqtt_user = "admin";
const char* mqtt_password = "password123";
const char* mqtt_topic = "iot/telemetry/station02";

// ==========================================
// CẤU HÌNH THIẾT BỊ & CẢM BIẾN
// ==========================================
const char* GATEWAY_ID = "b1bb11d8-9716-476c-9ea4-31fff86faabd";

struct SensorConfig {
  const char* id;
  const char* name;
  float minVal;
  float maxVal;
};

const SensorConfig SENSORS[] = {
  {"2739cb0b-adf1-440b-81b1-4958b466887c", "CO",    0.0,     15000.0},
  {"5cdf30d8-0a52-47ab-b36b-b342f07955a3", "PM10",  50.0,    200.0},
  {"70905001-8b60-4b08-923c-b1cf79a091d4", "SO2",   10.0,    200.0},
  {"a4aa6b3e-1a2c-4a8c-ba7a-c63a10156481", "O3",    10.0,    150.0},
  {"b0a487cc-62d5-478b-adcc-3d3af05caaad", "TEMP",  25.0,    35.0},
  {"e0053b3f-eb75-4206-8409-f05f4143b91b", "NO2",   10.0,    150.0},
  {"e560030d-401b-4fa7-93b5-07965c9f59ac", "PM2.5", 30.0,    150.0}
};

const int NUM_SENSORS = sizeof(SENSORS) / sizeof(SENSORS[0]);

// ==========================================
// MẠNG & THỜI GIAN
// ==========================================
WiFiClient espClient;
PubSubClient client(espClient);
unsigned long lastSendTime = 0;
const unsigned long SEND_INTERVAL = 5000; // Gửi mỗi 5 giây

const char* ntpServer = "pool.ntp.org";
const long gmtOffset_sec = 7 * 3600;
const int daylightOffset_sec = 0;

void setup_wifi() {
  delay(10);
  Serial.println();
  Serial.print("Dang ket noi toi WiFi: ");
  Serial.println(ssid);
  WiFi.begin(ssid, password);
  while (WiFi.status() != WL_CONNECTED) {
    delay(500);
    Serial.print(".");
  }
  Serial.println("\nDa ket noi WiFi thanh cong!");
  Serial.print("Dia chi IP ESP32: ");
  Serial.println(WiFi.localIP());
}

void reconnect() {
  while (!client.connected()) {
    Serial.print("Dang thu ket noi MQTT Broker...");
    String clientId = "mock_esp32_station_02";

    if (client.connect(clientId.c_str(), mqtt_user, mqtt_password)) {
      Serial.println(" Thanh cong!");
    } else {
      Serial.print(" That bai, ma loi = ");
      Serial.print(client.state());
      Serial.println(" -> Thu lai trong 5 giay");
      delay(5000);
    }
  }
}

long getTime() {
  time_t now;
  struct tm timeinfo;
  if (!getLocalTime(&timeinfo)) {
    return 0;
  }
  time(&now);
  return now;
}

// Hàm sinh số ngẫu nhiên dạng float làm tròn 2 chữ số thập phân
float getRandomFloat(float minVal, float maxVal) {
  float scale = rand() / (float)RAND_MAX;
  float val = minVal + scale * (maxVal - minVal);
  return roundf(val * 100.0f) / 100.0f;
}

void setup() {
  Serial.begin(115200);

  // WiFi, NTP & MQTT
  setup_wifi();
  configTime(gmtOffset_sec, daylightOffset_sec, ntpServer);
  client.setServer(mqtt_server, mqtt_port);
  client.setBufferSize(1024);

  Serial.println("Khoi tao thanh cong. Bat dau gui du lieu mo phong moi 5s...");
}

void loop() {
  // 1. Duy trì kết nối MQTT
  if (!client.connected()) {
    reconnect();
  }
  client.loop();

  // 2. Gửi dữ liệu định kỳ mỗi 5 giây
  unsigned long now = millis();
  if (now - lastSendTime >= SEND_INTERVAL) {
    lastSendTime = now;

    // Đóng gói JSON
    StaticJsonDocument<1024> doc;
    doc["gatewayId"] = GATEWAY_ID;

    long current_timestamp = getTime();
    if (current_timestamp > 1000000000) {
      doc["timestamp"] = current_timestamp;
    } else {
      doc["timestamp"] = 0; // Fallback khi chưa sync xong NTP
    }

    JsonArray sensorArray = doc.createNestedArray("sensors");

    for (int i = 0; i < NUM_SENSORS; i++) {
      JsonObject sensorObj = sensorArray.createNestedObject();
      sensorObj["sensorId"] = SENSORS[i].id;
      sensorObj["value"] = getRandomFloat(SENSORS[i].minVal, SENSORS[i].maxVal);
    }

    // Xuất chuỗi và gửi đi
    String payload;
    serializeJson(doc, payload);

    Serial.println("=================================");
    Serial.print("Gui MQTT: ");
    serializeJsonPretty(doc, Serial);
    Serial.println();

    client.publish(mqtt_topic, payload.c_str());
  }
}