import json
import random
import time
from datetime import datetime
import paho.mqtt.client as mqtt

# ==========================================
# CẤU HÌNH THÔNG SỐ KẾT NỐI (MQTT CONFIG)
# ==========================================
MQTT_BROKER = "localhost" # Địa chỉ Mosquitto Docker map ra máy thật
MQTT_PORT = 1883
MQTT_TOPIC = "iot/telemetry/station_1/sensor_temp" # Khớp với mẫu topic "iot/telemetry/#"

# Khai báo mã ID cảm biến (BẮT BUỘC phải tồn tại trong Database & Redis Cache của bạn)
SENSOR_ID = "fb100279-2ab0-439e-bc74-0dcd3fb18b33" 

# ==========================================
# CÁC HÀM XỬ LÝ SỰ KIỆN (CALLBACKS)
# ==========================================
def on_connect(client, userdata, flags, rc, properties=None):
    if rc == 0:
        print("[INFO] Kết nối tới Mosquitto Broker thành công!")
    else:
        print(f"[ERROR] Kết nối thất bại, mã phản hồi (Reason Code): {rc}")

def on_publish(client, userdata, mid, reason_code=None, properties=None):
    print(f"[INFO] Đã gửi bản tin thành công (Message ID: {mid})")

# ==========================================
# KHỞI TẠO VÀ CHẠY THIẾT BỊ GIẢ LẬP
# ==========================================
def run_simulator():
    # Sử dụng CallbackAPIVersion.VERSION2 cho thư viện paho-mqtt phiên bản 2.x trở lên
    client = mqtt.Client(callback_api_version=mqtt.CallbackAPIVersion.VERSION2)
    
    # Đăng ký các sự kiện kết nối và gửi tin
    client.on_connect = on_connect
    client.on_publish = on_publish

    print(f"[START] Đang kết nối tới Broker {MQTT_BROKER}:{MQTT_PORT}...")
    try:
        client.connect(MQTT_BROKER, MQTT_PORT, keepalive=60)
    except Exception as e:
        print(f"[CRITICAL] Không thể kết nối tới Mosquitto! Hãy chắc chắn container Docker đang chạy. Lỗi: {e}")
        return

    # Chạy vòng lặp mạng ngầm để giữ kết nối
    client.loop_start()

    print("[START] Bắt đầu luồng giả lập gửi dữ liệu thời gian thực (Nhấn Ctrl+C để dừng)...")
    try:
        while True:
            # 1. Giả lập giá trị nhiệt độ biến thiên quanh mức 25°C đến 35°C
            simulated_temp = round(random.uniform(25.0, 35.0), 2)
            
            # 2. Lấy mốc thời gian hiện tại dưới dạng Unix Epoch Timestamp (giây)
            epoch_timestamp = int(time.time())

            # 3. Đóng gói dữ liệu đúng định dạng DTO MqttPayload của ingestion-service
            payload = {
                "sensorId": SENSOR_ID,
                "value": simulated_temp,
                "timestamp": epoch_timestamp
            }

            # 4. Chuyển đổi sang chuỗi JSON và gửi đi
            json_payload = json.dumps(payload)
            print(f"\n[SEND] Gửi dữ liệu: {json_payload} tới topic '{MQTT_BROKER}'")
            
            client.publish(MQTT_TOPIC, payload=json_payload, qos=1)

            # 5. Nghỉ 5 giây trước khi gửi bản tin tiếp theo
            time.sleep(5)

    except KeyboardInterrupt:
        print("\n[STOP] Đang tắt thiết bị giả lập...")
    finally:
        client.loop_stop()
        client.disconnect()
        print("[STOP] Đã ngắt kết nối an toàn.")

if __name__ == "__main__":
    run_simulator()