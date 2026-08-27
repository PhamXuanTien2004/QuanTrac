import paho.mqtt.client as mqtt
import time
import json
import random

# ==========================================
# CẤU HÌNH KẾT NỐI MQTT
# ==========================================
BROKER_ADDRESS = "localhost"
BROKER_PORT = 1883
TOPIC = "iot/telemetry/station04"
USERNAME = "admin"           
PASSWORD = "password123"

# ==========================================
# CẤU HÌNH THIẾT BỊ 
# ==========================================
GATEWAY_ID = "3c3fb99b-f174-4bd1-8710-b5ec760e0754"

SENSORS = [
    {
        "id": "07f6b397-2928-4f34-9115-818b4763a38c",
        "name": "NO2",
        "min": 20,
        "max": 200
    },
    {
        "id": "1291c2b5-4e90-4bad-b147-7e1e7d526052",
        "name": "TEMP",
        "min": 25,
        "max": 35
    },
    {
        "id": "1c207efc-1e85-4085-859a-6e3dac5b5db6",
        "name": "CO",
        "min": 5000,
        "max": 25000
    },
    {
        "id": "2f6cca64-7bd5-4303-b032-b617019be59f",
        "name": "O3",
        "min": 20,
        "max": 180
    },
    {
        "id": "34b342c0-a80c-48b3-8802-125a5ac6fa01",
        "name": "SO2",
        "min": 20,
        "max": 250
    },
    {
        "id": "8913347d-57ec-4ef3-bb9d-691a17dc9c53",
        "name": "PM2.5",
        "min": 50,
        "max": 250
    },
    {
        "id": "fa42860b-a93f-4912-8c6d-6de779156783",
        "name": "PM10",
        "min": 80,
        "max": 300
    }
]

def on_connect(client, userdata, flags, rc):
    if rc == 0:
        print("[+] Kết nối MQTT Broker thành công!")
    else:
        print(f"[-] Kết nối thất bại, mã trả về: {rc}")

def main():
    client = mqtt.Client(client_id="mock_python_station_04")
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
            payload = {
                "gatewayId": GATEWAY_ID,
                "timestamp": int(time.time()),
                "sensors": []
            }
            
            for sensor in SENSORS:
                val = round(random.uniform(sensor["min"], sensor["max"]), 2)
                payload["sensors"].append({
                    "sensorId": sensor["id"],
                    "value": val
                })
                
            json_payload = json.dumps(payload)
            
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
