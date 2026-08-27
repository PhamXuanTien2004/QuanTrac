#include <WiFi.h>
#include <PubSubClient.h>
#include <ArduinoJson.h>
#include <time.h>
#include <ModbusMaster.h>
#include "PMS.h"

// ==========================================
// CẤU HÌNH WIFI
// ==========================================
// const char* ssid = "TP-Link_634C";
// const char* password = "48292719";
const char* ssid = "Ptx";
const char* password = "01042004";

// ==========================================
// CẤU HÌNH KẾT NỐI MQTT
// ==========================================
const char* mqtt_server = "172.20.10.4";
// const char* mqtt_server = "172.20.10.4";
const int mqtt_port = 1883;
const char* mqtt_user = "admin";
const char* mqtt_password = "password123";
const char* mqtt_topic = "iot/telemetry/station01";
const char* mqtt_control_topic = "iot/control/station01";

// ==========================================
// CẤU HÌNH THIẾT BỊ & ID CẢM BIẾN
// ==========================================
const char* GATEWAY_ID = "7ac2af92-3669-4865-b18e-0a4de6b5c717"; // ID của Gateway 01

const char* SENSOR_ID_TEMP = "ceb0fa40-db19-48cf-b302-7dc6d2f68625"; 
const char* SENSOR_ID_HUMI = "99ccdff1-9926-446d-bfa6-54d3538045b9"; 
const char* SENSOR_ID_PM25 = "3e452cc5-2492-4836-ae23-1ff89af48c4b"; 
const char* SENSOR_ID_PM10 = "c5455784-ea0e-44d6-865c-65910376807d"; 
const char* SENSOR_ID_CO   = "b8f28569-7f02-4a60-bd59-92c3db99c139"; 

// ==========================================
// CẤU HÌNH PHẦN CỨNG (UART & CHÂN PIN)
// ==========================================
HardwareSerial rs485Serial(1);
#define RX_MODBUS 22
#define TX_MODBUS 23
ModbusMaster node;

HardwareSerial pmsSerial(2);
PMS pms(pmsSerial);
PMS::DATA data;

int current_pm25 = 0;
int current_pm10 = 0;

#define MICS_PIN 35  

// ==========================================
// CẤU HÌNH MẠNG & THỜI GIAN
// ==========================================
WiFiClient espClient;
PubSubClient client(espClient);
unsigned long lastMsg = 0;

const char* ntpServer = "pool.ntp.org";
const long  gmtOffset_sec = 7 * 3600; 
const int   daylightOffset_sec = 0;

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
    // Dùng ID cố định để ổn định phiên làm việc
    String clientId = "ESP32_Station_01";
    
    if (client.connect(clientId.c_str(), mqtt_user, mqtt_password)) {
      Serial.println(" Thanh cong!");
      // Đăng ký nhận lệnh điều khiển
      client.subscribe(mqtt_control_topic);
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

void setup() {
  Serial.begin(115200);

  // Khởi tạo UART cho PMS5003 (Serial2)
  pmsSerial.begin(9600, SERIAL_8N1, 16, 17);

  // Khởi tạo UART cho Modbus RS485 (Serial1)
  rs485Serial.begin(9600, SERIAL_8N1, RX_MODBUS, TX_MODBUS);
  node.begin(1, rs485Serial);
  
  // Khởi tạo MiCS-5524
  pinMode(MICS_PIN, INPUT);

  // WiFi & MQTT & NTP
  setup_wifi();
  configTime(gmtOffset_sec, daylightOffset_sec, ntpServer);
  client.setServer(mqtt_server, mqtt_port);
  client.setBufferSize(1024);
  
  Serial.println("He thong hoat dong. Dang lay mau du lieu...");
}

void loop() {
  // 1. Duy trì kết nối MQTT
  if (!client.connected()) {
    reconnect();
  }
  client.loop();

  // 2. Đọc liên tục dữ liệu từ PMS5003 (Không được block hàm này)
  if (pms.read(data)) {
    current_pm25 = data.PM_AE_UG_2_5;
    current_pm10 = data.PM_AE_UG_10_0;
  }

  // 3. Chu kỳ đồng bộ theo giờ chuẩn (Mỗi 1 phút: đúng giây 00 của mỗi phút)
  struct tm timeinfo;
  static int lastMinuteSent = -1;
  static unsigned long lastFallbackMsg = 0;
  bool shouldSend = false;

  if (getLocalTime(&timeinfo)) {
    // Đã có giờ thực từ Internet (NTP)
    int currentMinute = timeinfo.tm_min;
    int currentSecond = timeinfo.tm_sec;
    // Kiểm tra: Bất kỳ phút nào (mỗi phút), đúng giây 00 và chưa từng gửi ở phút này
    if (currentSecond == 0 && currentMinute != lastMinuteSent) {
      lastMinuteSent = currentMinute;
      shouldSend = true;
    }
  } else {
    // Fallback: Không lấy được giờ thực, dùng đếm ngược millis() mỗi 1 phút (60,000 ms)
    unsigned long now = millis();
    if (now - lastFallbackMsg > 60000) {
      lastFallbackMsg = now;
      shouldSend = true;
    }
  }

  if (shouldSend) {

    // --- ĐỌC NHIỆT ẨM (MODBUS) ---
    float nhietDo = 0.0;
    float doAm = 0.0;
    
    // Đọc bắt đầu từ địa chỉ 1, lấy 2 thanh ghi (dựa trên phân tích Modbus Poll)
    uint8_t result = node.readHoldingRegisters(1, 2);
    if (result == node.ku8MBSuccess) {
      nhietDo = node.getResponseBuffer(0) / 10.0f;
      doAm = node.getResponseBuffer(1) / 10.0f;
    } else {
      Serial.println("[!] Loi doc Modbus Lefoo.");
    }

    // --- ĐỌC KHÍ CO ---
    int micsAnalog = analogRead(MICS_PIN);
    float coValue = micsAnalog * (3.3 / 4095.0) * 100; // Tạm tính

    // ==========================================
    // TẠO JSON ĐÓNG GÓI DỮ LIỆU
    // ==========================================
    StaticJsonDocument<1024> doc;
    doc["gatewayId"] = GATEWAY_ID;
    
    long current_timestamp = getTime();
    if(current_timestamp > 1000000000) { 
       doc["timestamp"] = current_timestamp;
    }

    JsonArray sensorArray = doc.createNestedArray("sensors");
    
    // --- GẮN DỮ LIỆU THỰC TẾ ---
    JsonObject tempObj = sensorArray.createNestedObject();
    tempObj["sensorId"] = SENSOR_ID_TEMP;
    tempObj["value"] = nhietDo;

    JsonObject humiObj = sensorArray.createNestedObject();
    humiObj["sensorId"] = SENSOR_ID_HUMI;
    humiObj["value"] = doAm;

    JsonObject pm25Obj = sensorArray.createNestedObject();
    pm25Obj["sensorId"] = SENSOR_ID_PM25;
    pm25Obj["value"] = current_pm25;

    JsonObject pm10Obj = sensorArray.createNestedObject();
    pm10Obj["sensorId"] = SENSOR_ID_PM10;
    pm10Obj["value"] = current_pm10;

    JsonObject coObj = sensorArray.createNestedObject();
    coObj["sensorId"] = SENSOR_ID_CO;
    coObj["value"] = coValue;

    // ==========================================
    // GỬI MQTT
    // ==========================================
    String payload;
    serializeJson(doc, payload);

    Serial.println("=================================");
    Serial.print("Gui MQTT: ");
    serializeJsonPretty(doc, Serial); 
    Serial.println(); 
    
    client.publish(mqtt_topic, payload.c_str());
  }
}