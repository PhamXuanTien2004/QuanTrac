# 🌍 QuanTrac Pro - Frontend Web Portal

Đây là thư mục chứa mã nguồn Frontend cho **Hệ thống Quan trắc và Theo dõi dữ liệu từ xa**. Giao diện được thiết kế hiện đại, sử dụng kiến trúc Single Page Application (SPA), hướng tới trải nghiệm người dùng cao cấp (Premium UI/UX) với Dark Mode mặc định và hiệu ứng Glassmorphism.

## 🛠 Lựa chọn Công nghệ (Tech Stack)

| Lớp | Công nghệ áp dụng | Vai trò |
| :--- | :--- | :--- |
| **Lõi Ứng dụng** | React 18, TypeScript, Vite | Xây dựng khung ứng dụng tốc độ cao, type-safe |
| **Định tuyến** | React Router v6 | Quản lý việc chuyển trang, bảo vệ các trang kín (Protected Routes) |
| **Giao tiếp API** | Axios | Gửi HTTP requests tới API Gateway |
| **Quản lý Trạng thái** | Zustand | Quản lý Global State (vd: danh sách các Trạm quan trắc) nhẹ nhàng, dễ bảo trì |
| **Bảo mật & Xác thực** | Keycloak (`react-oidc-context`) | Quản lý luồng đăng nhập OIDC (Authorization Code Flow), tự động nhúng Token |
| **Trực quan hoá (UI)** | Leaflet (Bản đồ), Lucide-React (Icons), Vanilla CSS | Tích hợp bản đồ GPS, các icon hệ thống và xây dựng giao diện tùy chỉnh 100% |

---

## 🏗 Kiến trúc Thư mục (Directory Structure)

```text
frontend/
├── src/
│   ├── components/
│   │   ├── auth/          # Chứa logic phân quyền (ProtectedRoute) bọc ngoài các trang
│   │   └── layouts/       # Chứa DashboardLayout (Sidebar, Header, Main Content)
│   ├── pages/             # Chứa các màn hình hiển thị: Dashboard, Stations, Alerts, Reports...
│   ├── services/          # Chứa các hàm giao tiếp API (vd: api.ts gắn Token, stations.ts lấy dữ liệu Trạm)
│   ├── store/             # Chứa trạng thái hệ thống bằng Zustand (vd: useStationStore)
│   ├── index.css          # Nơi định nghĩa toàn bộ Design System (Biến màu, Dark mode, Class dùng chung)
│   ├── App.tsx            # Trái tim của ứng dụng: Khai báo các đường dẫn (Routes)
│   └── main.tsx           # Điểm neo: Khởi tạo React và bọc Provider của Keycloak
└── package.json           # File quản lý thư viện và phiên bản
```

---

## 🔄 Luồng tương tác với Backend (FE - BE Integration)

1. **Xác thực (Auth):** Khi người dùng mở trang web, `ProtectedRoute` kiểm tra trạng thái. Nếu chưa có phiên làm việc, người dùng bị đẩy sang màn hình Keycloak (`localhost:8082`). Đăng nhập xong, người dùng mang theo Token quay lại trang chủ.
2. **Quản lý Giao tiếp (Axios):** File `api.ts` đã cấu hình một *Interceptor*. Mọi request gửi lên Gateway (`localhost:8080/api/v1`) đều tự động được đính kèm chuỗi `Authorization: Bearer <Token>`. Nếu không có Token, Backend Gateway sẽ chặn đứng.
3. **Gọi API thực tế:** Khi mở trang Quản lý Trạm, `useStationStore` (Zustand) kích hoạt hàm `getStations()`. Request xuyên qua Gateway, chạm tới `device-service` và lấy dữ liệu MySQL về trả lên Bảng (Table) cho người dùng xem.

---

## 🚀 Hướng dẫn Cài đặt và Chạy cục bộ (Local Development)

### Yêu cầu tiên quyết
- Cần có Node.js cài đặt sẵn trên máy.
- Hệ thống Backend Docker (bao gồm Keycloak, API Gateway) phải đang chạy nền.

### Các bước khởi chạy
1. Di chuyển vào thư mục frontend:
   ```bash
   cd /home/pxtien/Documents/QuanTrac/frontend
   ```
2. Cài đặt các thư viện phụ thuộc:
   ```bash
   npm install
   ```
3. Chạy server phát triển (Dev Server):
   ```bash
   npm run dev
   ```
4. Truy cập giao diện:
   Mở trình duyệt và truy cập vào **[http://localhost:5174](http://localhost:5174)**.
