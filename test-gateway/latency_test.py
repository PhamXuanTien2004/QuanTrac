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
    {"id": "2739cb0b-adf1-440b-81b1-4958b466887c", "name": "CO", "min": 0, "max": 1500},
    {"id": "5cdf30d8-0a52-47ab-b36b-b342f07955a3", "name": "PM10", "min": 50, "max": 200},
    {"id": "b0a487cc-62d5-478b-adcc-3d3af05caaad", "name": "TEMP", "min": 25, "max": 35},
    {"id": "e560030d-401b-4fa7-93b5-07965c9f59ac", "name": "PM2.5", "min": 30, "max": 150}
]

def on_connect(client, userdata, flags, rc):
    if rc == 0:
        print("[+] Kết nối MQTT Broker thành công!")
    else:
        print(f"[-] Kết nối thất bại, mã trả về: {rc}")

def main():
    client = mqtt.Client(client_id="mock_latency_test_station")
    client.username_pw_set(USERNAME, PASSWORD)
    client.on_connect = on_connect

    try:
        print(f"[*] Đang kết nối tới MQTT Broker tại {BROKER_ADDRESS}:{BROKER_PORT}...")
        client.connect(BROKER_ADDRESS, BROKER_PORT, 60)
        client.loop_start()
    except Exception as e:
        print(f"[-] Lỗi kết nối: {e}")
        return

    print("[*] BẮT ĐẦU BÀI TEST ĐỘ TRỄ (LATENCY TEST)")
    print("[*] Sẽ gửi dữ liệu kèm Timestamp (Mili-giây) mỗi 2 giây...")
    try:
        count = 1
        while True:
            # Lấy thời gian hiện tại theo MILI-GIÂY (ms)
            current_time_ms = int(time.time() * 1000)
            
            payload = {
                "gatewayId": GATEWAY_ID,
                "timestamp": current_time_ms,
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
            print(f"[{time.strftime('%H:%M:%S')}] Bản tin #{count} | Timestamp: {current_time_ms} -> Gửi thành công")
            
            count += 1
            time.sleep(2) # Gửi mỗi 2 giây để bạn dễ quan sát trên Web
            
    except KeyboardInterrupt:
        print("\n[*] Đã nhận lệnh dừng từ người dùng.")
    finally:
        client.loop_stop()
        client.disconnect()
        print("[*] Đã ngắt kết nối an toàn.")

if __name__ == "__main__":
    main()
