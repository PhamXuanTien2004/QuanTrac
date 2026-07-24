import paho.mqtt.client as mqtt
import time
import json
import random

# ==========================================
# CẤU HÌNH KẾT NỐI MQTT
# ==========================================
BROKER_ADDRESS = "localhost"
BROKER_PORT = 1883
TOPIC = "iot/telemetry/station1"
USERNAME = "admin"           # Thông tin xác thực theo MqttConfig.java
PASSWORD = "password123"

# ==========================================
# CẤU HÌNH THIẾT BỊ 
# ==========================================
# Gateway 91d351fa-3160-4b83-afd9-1ab3d5106944 chứa 3 cảm biến Độ ẩm, Áp suất, Nhiệt độ
GATEWAY_ID = "91d351fa-3160-4b83-afd9-1ab3d5106944"

SENSORS = [
    {"id": "132b572a-73cd-47bc-817f-dab73c5590e8", "name": "Độ Ẩm", "min": 20.0, "max": 80.0},
    {"id": "45a8c36a-26b6-413c-b556-cffb0d22690e", "name": "Áp suất", "min": 30.0, "max": 50.0},
    {"id": "f560cb80-74a1-48ea-9f74-8e5df406d882", "name": "Cảm biến Nhiệt độ", "min": 20.0, "max": 80.0}
]

def on_connect(client, userdata, flags, rc):
    if rc == 0:
        print("[+] Kết nối MQTT Broker thành công!")
    else:
        print(f"[-] Kết nối thất bại, mã trả về: {rc}")

def main():
    # Khởi tạo MQTT Client
    client = mqtt.Client(client_id="mock_python_station_01")
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

