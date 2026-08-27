import paho.mqtt.client as mqtt
import time
import json
import random

# ==========================================
# CẤU HÌNH KẾT NỐI MQTT
# ==========================================
BROKER_ADDRESS = "localhost"
BROKER_PORT = 1883
TOPIC = "iot/telemetry/station03"
USERNAME = "admin"           
PASSWORD = "password123"

# ==========================================
# CẤU HÌNH THIẾT BỊ 
# ==========================================
GATEWAY_ID = "c5ef05b7-d0fc-45f8-8412-7875e916a346"

SENSORS = [
    {
        "id": "1f8f5147-56ba-4b5d-9cf7-b78ea8fa7fb0",
        "name": "SO2",
        "min": 0,
        "max": 50
    },
    {
        "id": "6a2012a7-dd62-4a3d-82ac-36cb1101f008",
        "name": "O3",
        "min": 0,
        "max": 80
    },
    {
        "id": "783cae1a-ab4f-4f1d-8b49-e5b2029b2ec3",
        "name": "PM10",
        "min": 10,
        "max": 80
    },
    {
        "id": "8f57ae4b-28f4-450f-a883-24610529721a",
        "name": "PM2.5",
        "min": 5,
        "max": 50
    },
    {
        "id": "b0a236c3-a6d5-4bc3-a57f-e6354e6629ff",
        "name": "CO",
        "min": 0,
        "max": 5000
    },
    {
        "id": "be6941a8-2972-41ab-9e52-c0897096eab2",
        "name": "NO2",
        "min": 0,
        "max": 50
    },
    {
        "id": "eb53ea44-95aa-4fbe-9ce8-68c9e0526e4d",
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
    client = mqtt.Client(client_id="mock_python_station_03")
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
