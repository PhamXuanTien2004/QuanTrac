import json
import random
import time
import threading
import paho.mqtt.client as mqtt

# ==========================================
# CẤU HÌNH HẠ TẦNG (INFRASTRUCTURE CONFIG)
# ==========================================
MQTT_BROKER = "localhost"
MQTT_PORT = 1883

# Ngưỡng dải đo an toàn thực tế từ CSDL MySQL của bạn
TEMP_MIN = 10.5
TEMP_MAX = 45.5

# ==========================================
# THIẾT KẾ CÁC KỊCH BẢN KIỂM THỬ (TEST CASES)
# ==========================================
GATEWAYS_DATA_SOURCE = [
    # -------------------------------------------------------------
    # CASE 1: HỢP LỆ HOÀN TOÀN - Sử dụng Gateway và 3 cảm biến thực tế tương ứng
    # -------------------------------------------------------------
    {
        "case_name": "TH_1_HOP_LE_HOAN_TOAN",
        "station_code": "HN_DONG_DA-01",
        "gateway_code": "GW_ESP32_DONGDA_01",
        "gateway_id": "9f6bf558-7f5c-46d3-9f1f-bb94e4040f80", # Đúng Gateway ID thực tế của bạn
        "sensor_ids": [
            "5b016e4e-529d-4344-acc3-ec416623f43f", # Đúng Sensor ID (TEMP1)
            "53aa3a44-da47-47e1-bcfd-9d2ed65ece57", # Đúng Sensor ID (HUMI1)
            "24f71b08-79a6-4c60-845c-ebe644518bcd"  # Đúng Sensor ID (CO21)
        ],
        "value_mode": "NORMAL" # Giá trị ngẫu nhiên trong dải đo an toàn 10.5 -> 45.5
    },

    # -------------------------------------------------------------
    # CASE 2: SAI GATEWAY ID (Hệ thống từ chối & Hủy toàn bộ gói tin ngay lập tức)
    # -------------------------------------------------------------
    {
        "case_name": "TH_2_SAI_GATEWAY_ID",
        "station_code": "HN_DONG_DA-01",
        "gateway_code": "GW_ESP32_SAI_GATE_ID",
        "gateway_id": "99999999-aaaa-bbbb-cccc-999999999999", # Gateway ID giả mạo
        "sensor_ids": [
            "5b016e4e-529d-4344-acc3-ec416623f43f" # Gửi kèm Sensor ID đúng
        ],
        "value_mode": "NORMAL"
    },

    # -------------------------------------------------------------
    # CASE 3: SAI SENSOR ID (Gateway đúng nhưng chứa 1 Sensor ID giả mạo)
    # -------------------------------------------------------------
    {
        "case_name": "TH_3_SAI_SENSOR_ID",
        "station_code": "HN_DONG_DA-01",
        "gateway_code": "GW_ESP32_SAI_SENSOR",
        "gateway_id": "9f6bf558-7f5c-46d3-9f1f-bb94e4040f80", # Đúng Gateway ID
        "sensor_ids": [
            "5b016e4e-529d-4344-acc3-ec416623f43f", # Đúng Sensor ID
            "sensor-id-gia-mao-999999999999999999"  # Sensor ID giả mạo
        ],
        "value_mode": "NORMAL"
    },

    # -------------------------------------------------------------
    # CASE 4: GIÁ TRỊ VƯỢT NGƯỠNG AN TOÀN (Gateway và Sensor đúng, từ chối lưu vì quá nhiệt)
    # -------------------------------------------------------------
    {
        "case_name": "TH_4_VUOT_NGUONG_AN_TOAN",
        "station_code": "HN_DONG_DA-01",
        "gateway_code": "GW_ESP32_VUOT_NGUONG",
        "gateway_id": "9f6bf558-7f5c-46d3-9f1f-bb94e4040f80", # Đúng Gateway ID
        "sensor_ids": [
            "5b016e4e-529d-4344-acc3-ec416623f43f", # Đúng Sensor ID (TEMP1)
            "53aa3a44-da47-47e1-bcfd-9d2ed65ece57"  # Đúng Sensor ID (HUMI1)
        ],
        "value_mode": "OUT_OF_RANGE" # Ép tạo giá trị đột biến < 10.5 hoặc > 45.5
    }
]

# ==========================================
# HÀM MÔ PHỎNG HOẠT ĐỘNG CỦA 1 THIẾT BỊ
# ==========================================
def gateway_simulator_thread(gw_info):
    case_name = gw_info["case_name"]
    station_code = gw_info["station_code"]
    gateway_code = gw_info["gateway_code"]
    gateway_id = gw_info["gateway_id"]
    sensor_ids = gw_info["sensor_ids"]
    value_mode = gw_info["value_mode"]

    client_id = f"client_sim_{gateway_code}"
    client = mqtt.Client(callback_api_version=mqtt.CallbackAPIVersion.VERSION2, client_id=client_id)
    
    def on_connect(client, userdata, flags, rc, properties=None):
        if rc == 0:
            print(f"[CONNECTED] {gateway_code} ({case_name}) đã kết nối thành công!")
        else:
            print(f"[ERROR] {gateway_code} kết nối thất bại, mã lỗi: {rc}")

    client.on_connect = on_connect

    try:
        client.connect(MQTT_BROKER, MQTT_PORT, keepalive=60)
    except Exception as e:
        print(f"[FATAL] {gateway_code} không thể kết nối Broker: {e}")
        return

    client.loop_start()
    time.sleep(random.uniform(0.5, 1.5)) 

    topic = f"iot/telemetry/{station_code}/{gateway_code}"

    try:
        while True:
            # 1. Khởi tạo cấu trúc gói tin thô
            payload = {
                "gatewayId": gateway_id,
                "timestamp": int(time.time()), 
                "sensors": []                  
            }

            # 2. Xử lý giá trị đo đạc theo từng kịch bản kiểm thử
            for s_id in sensor_ids:
                if value_mode == "OUT_OF_RANGE":
                    # Ép tạo giá trị lỗi: thấp hơn 10.5 hoặc cao hơn 45.5
                    simulated_value = random.choice([
                        round(random.uniform(1.0, 9.9), 2),
                        round(random.uniform(46.0, 60.0), 2)
                    ])
                else:
                    # Giá trị an toàn nằm trong dải đo
                    simulated_value = round(random.uniform(TEMP_MIN, TEMP_MAX), 2)
                
                sensor_reading = {
                    "sensorId": s_id,
                    "value": simulated_value
                }
                payload["sensors"].append(sensor_reading)

            # 3. Đóng gói và gửi
            json_payload = json.dumps(payload)
            client.publish(topic, payload=json_payload, qos=1)
            
            print(f"\n=============================================================")
            print(f"✈️  [{case_name}] GỬI TIN LÊN TOPIC: {topic}")
            print(f"=============================================================")
            print(json.dumps(payload, indent=2))
            
            # Gửi tin giãn cách ngẫu nhiên từ 5 đến 8 giây để dễ quan sát log
            time.sleep(random.uniform(5.0, 8.0))

    except KeyboardInterrupt:
        pass
    finally:
        client.loop_stop()
        client.disconnect()
        print(f"[DISCONNECTED] {gateway_code} đã dừng.")

# ==========================================
# KHỞI CHẠY HỆ THỐNG GIẢ LẬP
# ==========================================
if __name__ == "__main__":
    print("=============================================================")
    print("      HỆ THỐNG GIẢ LẬP ĐA KỊCH BẢN KIỂM THỬ PIPELINE (V5)    ")
    print("=============================================================")
    
    threads = []
    for gw_info in GATEWAYS_DATA_SOURCE:
        thread = threading.Thread(target=gateway_simulator_thread, args=(gw_info,))
        threads.append(thread)
        thread.start()

    try:
        for thread in threads:
            thread.join()
    except KeyboardInterrupt:
        print("\n[INFO] Đã tắt hệ thống kiểm thử thành công.")