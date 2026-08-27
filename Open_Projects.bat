@echo off
:: Đường dẫn tới file chạy của IntelliJ IDEA
set IDEA="C:\Program Files\JetBrains\IntelliJ IDEA 2025.2.6.1\bin\idea64.exe"

start "" %IDEA% "D:\DaiHoc\QuanTrac\discovery-service"
start "" %IDEA% "D:\DaiHoc\QuanTrac\gateway-service"
start "" %IDEA% "D:\DaiHoc\QuanTrac\auth-service"
start "" %IDEA% "D:\DaiHoc\QuanTrac\user-service"
start "" %IDEA% "D:\DaiHoc\QuanTrac\device-service"
start "" %IDEA% "D:\DaiHoc\QuanTrac\ingestion-service"
start "" %IDEA% "D:\DaiHoc\QuanTrac\realtime-service"
start "" %IDEA% "D:\DaiHoc\QuanTrac\notification-service"
