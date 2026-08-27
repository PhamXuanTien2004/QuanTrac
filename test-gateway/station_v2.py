import paho.mqtt.client as mqtt
import time
import json
import random

# ==========================================
# CẤU HÌNH KẾT NỐI MQTT
# ==========================================
BROKER_ADDRESS = "localhost"
BROKER_PORT = 1883
TOPIC = "iot/telemetry/stationB"
USERNAME = "admin"           # Thông tin xác thực theo MqttConfig.java
PASSWORD = "password123"

# ==========================================
# CẤU HÌNH THIẾT BỊ 
# ==========================================
GATEWAY_ID = "4898bad6-f22b-4347-9553-d2cdccf9fad5" # GW2 của Trạm 2

SENSORS = [
    {
        "id": "36153b35-8dfe-4013-9cd9-e4f5828496e7", 
        "name": "Độ Ẩm 2", 
        "min": 40.0, 
        "max": 60.0
    },
    {
        "id": "c25dec39-c325-4dae-b9fe-585ec719cfc1", 
        "name": "Nhiệt Độ 2", 
        "min": 20.0, 
        "max": 80.0
    },
    {
        "id": "253de576-4e2a-428d-98e8-f9423c6aa614", 
        "name": "Áp Suất 2", 
        "min": 950.0, 
        "max": 1020.0
    }
]

def on_connect(client, userdata, flags, rc):
    if rc == 0:
        print("[+] Kết nối MQTT Broker thành công!")
    else:
        print(f"[-] Kết nối thất bại, mã trả về: {rc}")

def main():
    # Khởi tạo MQTT Client
    client = mqtt.Client(client_id="mock_python_station_02")
    client.username_pw_set(USERNAME, PASSWORD)
    client.on_connect = on_connect

    try:
        print(f"[*] Đang kết nối tới MQTT Broker tại {BROKER_ADDRESS}:{BROKER_PORT}...")
        client.connect(BROKER_ADDRESS, BROKER_PORT, 60)
        client.loop_start()
    except Exception as e:
        print(f"[-] Lỗi kết nối: {e}")
        return

    print("[*] Bắt đầu gửi dữ liệu cảm biến ngẫu nhiên mỗi 5 giây (Bấm Ctrl+C để dừng)...")
    try:
        while True:
            # Tạo payload đúng chuẩn MqttPayload.java (của Ingestion Service)
            payload = {
                "gatewayId": GATEWAY_ID,
                "timestamp": int(time.time()), # Timestamp dạng Giây (Seconds) vì backend gọi Instant.ofEpochSecond
                "sensors": []
            }
            
            for sensor in SENSORS:
                # Random giá trị trong khoảng cho phép
                val = round(random.uniform(sensor["min"], sensor["max"]), 2)
                payload["sensors"].append({
                    "sensorId": sensor["id"],
                    "value": val
                })
                
            json_payload = json.dumps(payload)
            
            # Gửi lên broker với QoS 1
            client.publish(TOPIC, json_payload, qos=1)
            print(f"[{time.strftime('%H:%M:%S')}] Đã gửi -> {json_payload}")
            
            time.sleep(5)
            
    except KeyboardInterrupt:
        print("\n[*] Đã nhận lệnh dừng từ người dùng.")
    finally:
        client.loop_stop()
        client.disconnect()
        print("[*] Đã ngắt kết nối an toàn.")

if __name__ == "__main__":
    main()
