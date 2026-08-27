import os

stations = {
    "station_01": {
        "gateway_id": "7ac2af92-3669-4865-b18e-0a4de6b5c717",
        "topic": "iot/telemetry/station01",
        "client_id": "mock_python_station_01",
        "sensors": [
            {"id": "0b311ef2-d908-482f-8f24-03475a824259", "name": "SO2", "min": 0, "max": 150},
            {"id": "3e452cc5-2492-4836-ae23-1ff89af48c4b", "name": "PM2.5", "min": 0, "max": 100},
            {"id": "5fb037ed-99aa-4f3f-bfba-30c8bfe5a670", "name": "NO2", "min": 0, "max": 100},
            {"id": "815f4b1b-e610-4fb4-939e-e344875671c4", "name": "O3", "min": 0, "max": 120},
            {"id": "b8f28569-7f02-4a60-bd59-92c3db99c139", "name": "CO", "min": 0, "max": 10000},
            {"id": "c5455784-ea0e-44d6-865c-65910376807d", "name": "PM10", "min": 0, "max": 150},
            {"id": "ceb0fa40-db19-48cf-b302-7dc6d2f68625", "name": "TEMP", "min": 25, "max": 35}
        ]
    },
    "station_02": {
        "gateway_id": "b1bb11d8-9716-476c-9ea4-31fff86faabd",
        "topic": "iot/telemetry/station02",
        "client_id": "mock_python_station_02",
        "sensors": [
            {"id": "2739cb0b-adf1-440b-81b1-4958b466887c", "name": "CO", "min": 0, "max": 15000},
            {"id": "5cdf30d8-0a52-47ab-b36b-b342f07955a3", "name": "PM10", "min": 50, "max": 200},
            {"id": "70905001-8b60-4b08-923c-b1cf79a091d4", "name": "SO2", "min": 10, "max": 200},
            {"id": "a4aa6b3e-1a2c-4a8c-ba7a-c63a10156481", "name": "O3", "min": 10, "max": 150},
            {"id": "b0a487cc-62d5-478b-adcc-3d3af05caaad", "name": "TEMP", "min": 25, "max": 35},
            {"id": "e0053b3f-eb75-4206-8409-f05f4143b91b", "name": "NO2", "min": 10, "max": 150},
            {"id": "e560030d-401b-4fa7-93b5-07965c9f59ac", "name": "PM2.5", "min": 30, "max": 150}
        ]
    },
    "station_03": {
        "gateway_id": "c5ef05b7-d0fc-45f8-8412-7875e916a346",
        "topic": "iot/telemetry/station03",
        "client_id": "mock_python_station_03",
        "sensors": [
            {"id": "1f8f5147-56ba-4b5d-9cf7-b78ea8fa7fb0", "name": "SO2", "min": 0, "max": 50},
            {"id": "6a2012a7-dd62-4a3d-82ac-36cb1101f008", "name": "O3", "min": 0, "max": 80},
            {"id": "783cae1a-ab4f-4f1d-8b49-e5b2029b2ec3", "name": "PM10", "min": 10, "max": 80},
            {"id": "8f57ae4b-28f4-450f-a883-24610529721a", "name": "PM2.5", "min": 5, "max": 50},
            {"id": "b0a236c3-a6d5-4bc3-a57f-e6354e6629ff", "name": "CO", "min": 0, "max": 5000},
            {"id": "be6941a8-2972-41ab-9e52-c0897096eab2", "name": "NO2", "min": 0, "max": 50},
            {"id": "eb53ea44-95aa-4fbe-9ce8-68c9e0526e4d", "name": "TEMP", "min": 25, "max": 35}
        ]
    },
    "station_04": {
        "gateway_id": "3c3fb99b-f174-4bd1-8710-b5ec760e0754",
        "topic": "iot/telemetry/station04",
        "client_id": "mock_python_station_04",
        "sensors": [
            {"id": "07f6b397-2928-4f34-9115-818b4763a38c", "name": "NO2", "min": 20, "max": 200},
            {"id": "1291c2b5-4e90-4bad-b147-7e1e7d526052", "name": "TEMP", "min": 25, "max": 35},
            {"id": "1c207efc-1e85-4085-859a-6e3dac5b5db6", "name": "CO", "min": 5000, "max": 25000},
            {"id": "2f6cca64-7bd5-4303-b032-b617019be59f", "name": "O3", "min": 20, "max": 180},
            {"id": "34b342c0-a80c-48b3-8802-125a5ac6fa01", "name": "SO2", "min": 20, "max": 250},
            {"id": "8913347d-57ec-4ef3-bb9d-691a17dc9c53", "name": "PM2.5", "min": 50, "max": 250},
            {"id": "fa42860b-a93f-4912-8c6d-6de779156783", "name": "PM10", "min": 80, "max": 300}
        ]
    }
}

template = """import paho.mqtt.client as mqtt
import time
import json
import random

# ==========================================
# CẤU HÌNH KẾT NỐI MQTT
# ==========================================
BROKER_ADDRESS = "localhost"
BROKER_PORT = 1883
TOPIC = "{topic}"
USERNAME = "admin"           
PASSWORD = "password123"

# ==========================================
# CẤU HÌNH THIẾT BỊ 
# ==========================================
GATEWAY_ID = "{gateway_id}"

SENSORS = {sensors_json}

def on_connect(client, userdata, flags, rc):
    if rc == 0:
        print("[+] Kết nối MQTT Broker thành công!")
    else:
        print(f"[-] Kết nối thất bại, mã trả về: {{rc}}")

def main():
    client = mqtt.Client(client_id="{client_id}")
    client.username_pw_set(USERNAME, PASSWORD)
    client.on_connect = on_connect

    try:
        print(f"[*] Đang kết nối tới MQTT Broker tại {{BROKER_ADDRESS}}:{{BROKER_PORT}}...")
        client.connect(BROKER_ADDRESS, BROKER_PORT, 60)
        client.loop_start()
    except Exception as e:
        print(f"[-] Lỗi kết nối: {{e}}")
        return

    print("[*] Bắt đầu gửi dữ liệu cảm biến ngẫu nhiên mỗi 5 giây (Bấm Ctrl+C để dừng)...")
    try:
        while True:
            payload = {{
                "gatewayId": GATEWAY_ID,
                "timestamp": int(time.time()),
                "sensors": []
            }}
            
            for sensor in SENSORS:
                val = round(random.uniform(sensor["min"], sensor["max"]), 2)
                payload["sensors"].append({{
                    "sensorId": sensor["id"],
                    "value": val
                }})
                
            json_payload = json.dumps(payload)
            
            client.publish(TOPIC, json_payload, qos=1)
            print(f"[{{time.strftime('%H:%M:%S')}}] Đã gửi -> {{json_payload}}")
            
            time.sleep(5)
            
    except KeyboardInterrupt:
        print("\\n[*] Đã nhận lệnh dừng từ người dùng.")
    finally:
        client.loop_stop()
        client.disconnect()
        print("[*] Đã ngắt kết nối an toàn.")

if __name__ == "__main__":
    main()
"""

for name, config in stations.items():
    import json
    sensors_json = json.dumps(config["sensors"], indent=4, ensure_ascii=False)
    content = template.format(
        topic=config["topic"],
        gateway_id=config["gateway_id"],
        client_id=config["client_id"],
        sensors_json=sensors_json
    )
    with open(f"{name}.py", "w", encoding="utf-8") as f:
        f.write(content)
        
print("Generated python scripts successfully!")
