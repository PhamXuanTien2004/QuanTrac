import paho.mqtt.client as mqtt
import time
import json
import random

# ==========================================
# CẤU HÌNH KẾT NỐI MQTT
# ==========================================
BROKER_ADDRESS = "localhost"
BROKER_PORT = 1883
TOPIC = "iot/telemetry/station02"
USERNAME = "admin"           
PASSWORD = "password123"

# ==========================================
# CẤU HÌNH THIẾT BỊ 
# ==========================================
GATEWAY_ID = "b1bb11d8-9716-476c-9ea4-31fff86faabd"

SENSORS = [
    {
        "id": "2739cb0b-adf1-440b-81b1-4958b466887c",
        "name": "CO",
        "min": 0,
        "max": 15000
    },
    {
        "id": "5cdf30d8-0a52-47ab-b36b-b342f07955a3",
        "name": "PM10",
        "min": 50,
        "max": 200
    },
    {
        "id": "70905001-8b60-4b08-923c-b1cf79a091d4",
        "name": "SO2",
        "min": 10,
        "max": 200
    },
    {
        "id": "a4aa6b3e-1a2c-4a8c-ba7a-c63a10156481",
        "name": "O3",
        "min": 10,
        "max": 150
    },
    {
        "id": "b0a487cc-62d5-478b-adcc-3d3af05caaad",
        "name": "TEMP",
        "min": 25,
        "max": 35
    },
    {
        "id": "e0053b3f-eb75-4206-8409-f05f4143b91b",
        "name": "NO2",
        "min": 10,
        "max": 150
    },
    {
        "id": "e560030d-401b-4fa7-93b5-07965c9f59ac",
        "name": "PM2.5",
        "min": 30,
        "max": 150
    }
]

def on_connect(client, userdata, flags, rc):
    if rc == 0:
        print("[+] Kết nối MQTT Broker thành công!")
    else:
        print(f"[-] Kết nối thất bại, mã trả về: {rc}")

def main():
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
