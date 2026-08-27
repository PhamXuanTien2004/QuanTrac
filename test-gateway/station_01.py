import paho.mqtt.client as mqtt
import time
import json
import random

# ==========================================
# CẤU HÌNH KẾT NỐI MQTT
# ==========================================
BROKER_ADDRESS = "localhost"
BROKER_PORT = 1883
TOPIC = "iot/telemetry/station01"
USERNAME = "admin"           
PASSWORD = "password123"

# ==========================================
# CẤU HÌNH THIẾT BỊ 
# ==========================================
GATEWAY_ID = "7ac2af92-3669-4865-b18e-0a4de6b5c717"

SENSORS = [
    {
        "id": "0b311ef2-d908-482f-8f24-03475a824259",
        "name": "SO2",
        "min": 0,
        "max": 150
    },
    {
        "id": "3e452cc5-2492-4836-ae23-1ff89af48c4b",
        "name": "PM2.5",
        "min": 0,
        "max": 100
    },
    {
        "id": "5fb037ed-99aa-4f3f-bfba-30c8bfe5a670",
        "name": "NO2",
        "min": 0,
        "max": 100
    },
    {
        "id": "815f4b1b-e610-4fb4-939e-e344875671c4",
        "name": "O3",
        "min": 0,
        "max": 120
    },
    {
        "id": "b8f28569-7f02-4a60-bd59-92c3db99c139",
        "name": "CO",
        "min": 0,
        "max": 10000
    },
    {
        "id": "c5455784-ea0e-44d6-865c-65910376807d",
        "name": "PM10",
        "min": 0,
        "max": 150
    },
    {
        "id": "ceb0fa40-db19-48cf-b302-7dc6d2f68625",
        "name": "TEMP",
        "min": 25,
        "max": 35
    }
]

def on_connect(client, userdata, flags, rc):
    if rc == 0:
        print("[+] Kết nối MQTT Broker thành công!")
    else:
        print(f"[-] Kết nối thất bại, mã trả về: {rc}")

def main():
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
