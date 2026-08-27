import requests
import random
import time
import sys
import io

sys.stdout = io.TextIOWrapper(sys.stdout.buffer, encoding='utf-8')

API_URL = "http://localhost:8180/api/v1"

def get_stations():
    # Hardcoded stations based on user input
    return [
        {
            "id": "5622a918-190d-4260-b27f-dbfdd92368f5",
            "name": "01 Chi cục Bảo vệ môi trường Hà Nội"
        },
        {
            "id": "e877c4bd-51e6-4048-9fff-42149822476e",
            "name": "02 UBND phường Minh Khai, Bắc Từ Liêm"
        },
        {
            "id": "1067ca6a-2c22-45d3-8551-fda8e41ae7bf",
            "name": "03 Công viên Nhân Chính - Khuất Duy Tiến"
        },
        {
            "id": "96e7a98b-16f9-4c4f-a7f4-9a54f987a9c7",
            "name": "04 Số 1 đường Giải Phóng - phường Bạch Mai - ĐHBK"
        }
    ]

def register_user(username, password, email, role, station_id, station_name, first_name, last_name, phone):
    payload = {
        "username": username,
        "password": password,
        "email": email,
        "firstName": first_name,
        "lastName": last_name,
        "phone": phone,
        "roles": [role],
        "stationId": station_id,
        "stationName": station_name,
        "notificationMethod": "ALL"
    }
    res = requests.post(f"{API_URL}/auth/register", json=payload)
    if res.status_code == 200 or res.status_code == 201:
        print(f"Created {role} {username} for station '{station_name}'")
    else:
        print(f"Failed to create {username} | Status: {res.status_code} | Text: {res.text}")
        sys.exit(1)

def main():
    stations = get_stations()

    for idx, station in enumerate(stations):
        station_num = str(idx + 1).zfill(2)
        station_id = station.get("id")
        station_name = station.get("name")
        
        print(f"\n=== Creating users for {station_name} (ID: {station_id}) ===")
        
        # 5 Managers
        for i in range(1, 6):
            username = f"{station_num}_Manager{i}"
            email = f"{station_num}_manager{i}@gmail.com".lower()
            register_user(
                username=username,
                password="Pxtien@2004",
                email=email,
                role="Manager",
                station_id=station_id,
                station_name=station_name,
                first_name=f"Manager {i}",
                last_name=f"Trạm {station_num}",
                phone=f"09{random.randint(10000000, 99999999)}"
            )
            
        # 100 Staff
        for i in range(1, 101):
            username = f"{station_num}_Staff{i}"
            email = f"{station_num}_staff{i}@gmail.com".lower()
            register_user(
                username=username,
                password="Pxtien@2004",
                email=email,
                role="Staff",
                station_id=station_id,
                station_name=station_name,
                first_name=f"Staff {i}",
                last_name=f"Trạm {station_num}",
                phone=f"09{random.randint(10000000, 99999999)}"
            )
            time.sleep(0.01) # Tránh bị dội Rate Limit quá nhanh

if __name__ == "__main__":
    main()
