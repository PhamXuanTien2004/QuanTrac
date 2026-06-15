import paho.mqtt.client as mqtt
import ssl

def on_connect(client, userdata, flags, rc):
    if rc == 0:
        print("Đã kết nối an toàn với Mosquitto Mức 3!")
    else:
        print(f"Kết nối thất bại, mã lỗi: {rc}")

client = mqtt.Client()
client.on_connect = on_connect

# Cấu hình chứng chỉ 2 chiều
client.tls_set(ca_certs="ca.crt", 
               certfile="client.crt", 
               keyfile="client.key", 
               tls_version=ssl.PROTOCOL_TLSv1_2)

# Vô hiệu hóa kiểm tra hostname nếu bạn test trong mạng LAN bằng IP
client.tls_insecure_set(True) 

# Thay IP này bằng đúng IP/Domain đã điền ở Common Name của Server
client.connect("192.168.1.150", 8883, 60)

client.publish("quantrac/data", "Dữ liệu tuyệt mật")
client.loop_forever()